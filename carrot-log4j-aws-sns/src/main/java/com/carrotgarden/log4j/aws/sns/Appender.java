/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import com.amazonaws.auth.*;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Topic;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.Loader;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.*;

/**
 * AWS SNS appender
 * 
 * original idea from
 * 
 * https://github.com/apetresc/amazon-sns-log4j-appender
 * 
 * https://github.com/insula/log4j-sns
 * 
 */
public class Appender extends AppenderSkeleton {

	public static final int DEFAULT_POOL_MIN = 0;
	public static final int DEFAULT_POOL_MAX = 10;
    public static final String DEFAULT_AWS_REGION_NAME = Regions.US_EAST_1.getName();

	//

	/** log4j config option; amazon credentials URL or file; must exist */
	@JsonProperty
	protected String credentials;

	/** log4j config option; SNS topic name; must exist */
	@JsonProperty
	protected String topicName;

	/**
	 * log4j config option; SNS topic subject; use for instance identity;
	 * optional
	 */
	@JsonProperty
	protected String topicSubject;

    /**
     * log4j config option; AWS region for SNS topic; optional
     * optional
     */
    @JsonProperty
    protected String regionName = DEFAULT_AWS_REGION_NAME;

    /** log4j config option; minimum thread pool size; optional */
	@JsonProperty
	protected int poolMin = DEFAULT_POOL_MIN;

	/** log4j config option; maximum thread pool size; optional */
	@JsonProperty
	protected int poolMax = DEFAULT_POOL_MAX;

	/** log4j config option; layout class name; optional */
	@JsonProperty
	public Layout getLayout() {
		return super.getLayout();
	}

	/** render for {@link #toString()} */
	@JsonProperty
	public String getLayoutClassName() {
		final Layout layout = getLayout();
		return layout == null ? null : layout.getClass().getName();
	}

	/** log4j config option; layout class name; optional */
	public void setLayoutClassName(final String layoutClassName) {

		final Layout defaultLayout = new PatternLayout();

		layout = (Layout) OptionConverter.instantiateByClassName( //
				layoutClassName, //
				Layout.class, //
				defaultLayout //
				);

	}

	/** render for {@link #toString()} */
	@JsonProperty
	public String getEvaluatorClassName() {
		final Evaluator evaluator = getEvaluator();
		return evaluator == null ? null : evaluator.getClass().getName();
	}

	/** log4j config option; evaluator class name; optional */
	public void setEvaluatorClassName(final String evaluatorClassName) {

		final Evaluator defaultEvaluator = new EvaluatorThrottler();

		evaluator = (Evaluator) OptionConverter.instantiateByClassName( //
				evaluatorClassName, //
				Evaluator.class, //
				defaultEvaluator //
				);

	}

	/** evaluator configured for this appender */
	@JsonProperty
	protected String evaluatorProperties;

	//

	/** evaluator configured for this appender */
	@JsonProperty
	protected Evaluator evaluator;

	/**
	 * topic ARN resolved from existing amazon topic name
	 * 
	 * http://aws.amazon.com/sns/faqs/#10
	 */
	@JsonProperty
	protected String topicARN;

	/** AWS SNS client thread pool */
	protected ExecutorService service;

	/** AWS SNS client dedicated to the appender */
	protected AmazonSNSAsync amazonClient;

    protected AWSCredentialsProvider amazonCredentials;

    /** AWS SNS region */
    protected Region awsSnsRegion;

    /** appender activation status */
	@JsonProperty
	protected volatile boolean isActive;

	//

	public boolean isActive() {
		return isActive;
	}

	public boolean isTriggering(final LoggingEvent event) {
		return isActive && evaluator.isTriggeringEvent(event);
	}

	public boolean hasAwsCredentialsOverride() {
		return "true".equalsIgnoreCase(System.getProperty("aws_sns_credentials_override"));
	}

	public String getEnvAwsRegion() {
		return System.getProperty("aws_sns_region");
	}

	public boolean hasCredentials() {
		return credentials != null;
	}

	public boolean hasTopicName() {
		return topicName != null;
	}

	public boolean hasTopicSubject() {
		return topicSubject != null;
	}

	public boolean hasEvaluator() {
		return evaluator != null;
	}

	public boolean hasLayout() {
		return layout != null;
	}

	public boolean hasTopicARN() {
		return topicARN != null;
	}

	public boolean hasAmazonClient() {
		return amazonClient != null;
	}

	/** provide amazon login credentials from file */
	protected boolean ensureCredentials() {
		if (hasAwsCredentialsOverride()) {
			amazonCredentials = new AWSCredentialsProviderChain(new InstanceProfileCredentialsProvider(),
																new EnvironmentVariableCredentialsProvider(),
																new SystemPropertiesCredentialsProvider());
		} else if (hasCredentials()) {
            // Try to get URL and open stream
            // Any exception, try as file
            URL url = null;
            try {
                url = new URL(getCredentials());
            } catch (MalformedURLException murle) {
                url = Loader.getResource(getCredentials());
            }

            if (url != null) {
                try {
                    amazonCredentials = new StaticCredentialsProvider(new PropertiesCredentials(url.openStream()));
                    return true;
                } catch (Exception e) {
                    // fail out
                }
            } else {
                try {
                    amazonCredentials = new StaticCredentialsProvider(new PropertiesCredentials(new File(getCredentials())));
                    return true;
                } catch (Exception e2) {
                    // fail out
                }
            }
		}

		LogLog.error("sns: invalid option", new IllegalArgumentException(
				getCredentials() + " for Credentials"));

		return false;

	}

	/** amazon topic name is required option */
	protected boolean ensureTopicName() {

		if (hasTopicName()) {

			return true;

		} else {

			LogLog.error("sns: ivalid option", new IllegalArgumentException(
					"TopicName"));

			return false;

		}

	}

    /** instantiate amazon client */
	protected boolean ensureAmazonClient() {

		try {

			amazonClient = new AmazonSNSAsyncClient(amazonCredentials, service);

			return true;

		} catch (final Exception e) {

			LogLog.error("sns: amazon client init failure", e);

			return false;

		}

	}

    /** instantiate amazon region */
    protected boolean ensureAmazonRegion() {
		String envRegionName = getEnvAwsRegion();
		if (envRegionName != null && !envRegionName.isEmpty()) {
			return this.ensureAmazonRegion(envRegionName);
		} else if (this.regionName != null && !this.regionName.isEmpty()) {
			return this.ensureAmazonRegion(this.regionName);
		}
		return false;
    }

	protected boolean ensureAmazonRegion(String regionName) {
		try {
			awsSnsRegion = RegionUtils.getRegion(regionName);

			amazonClient.setRegion(awsSnsRegion);

			return true;

		} catch (final Exception e) {

			LogLog.error("sns: amazon region init failure", e);

			return false;

		}
	}

    /** resolve topic ARN from topic name */
	protected boolean ensureTopicARN() {

		try {

			final ListTopicsResult result = amazonClient.listTopics();

			final List<Topic> topicList = result.getTopics();

			for (final Topic entry : topicList) {

				final String arn = entry.getTopicArn();
				final String name = Util.topicNameFromARN(arn);

				if (getTopicName().equals(name)) {
					topicARN = arn;
					return true;
				}

			}

			LogLog.error("sns: unknown topic name",
					new IllegalArgumentException(getTopicName()));

			return false;

		} catch (final Exception e) {

			LogLog.error("sns: amazon topic lookup failure", e);

			return false;

		}

	}

	/** provide default throttling evaluator */
	protected boolean ensureEvaluator() {

		try {

			if (!hasEvaluator()) {
				setEvaluator(new EvaluatorThrottler());
			}

			getEvaluator().setProperties(getEvaluatorProperties());

			return true;

		} catch (final Exception e) {

			LogLog.error("sns: evaluator init falure", e);

			return false;

		}

	}

	/** provide default JSON event layout renderer */
	protected boolean ensureLayout() {

		try {

			if (!hasLayout()) {
				setLayout(new LayoutJSON());
			}

			return true;

		} catch (final Exception e) {

			LogLog.error("sns: layout init failure", e);

			return false;

		}

	}

	/** provide AWS SNS thread pool */
	protected boolean ensureService() {

		try {

			service = new ThreadPoolExecutor(//
					poolMin, //
					poolMax, //
					60L, //
					TimeUnit.SECONDS, //
					new SynchronousQueue<Runnable>(), //
					new ThreadFactoryAWS() //
			);

			return true;

		} catch (final Exception e) {

			LogLog.warn("sns: failed to init service; using default", e);

			service = Executors.newCachedThreadPool();

			return true;

		}

	}

	@Override
	public synchronized void activateOptions() {

		isActive = ensureLayout() //
				&& ensureEvaluator() //
				&& ensureService() //
				&& ensureCredentials() //
				&& ensureAmazonClient() //
                && ensureAmazonRegion() //
				&& ensureTopicName() //
				&& ensureTopicARN() //
		;

		LogLog.warn("sns: appender activate : " + getClass().getName() + "\n"
				+ this);

		if (!isActive()) {
			LogLog.error("sns: appender is disabled due to invalid configration  : "
					+ getClass().getName());
		}

	}

	/**  */
	@Override
	public synchronized void close() {

		isActive = false;

		LogLog.warn("sns: appender deactivate : " + getClass().getName());

		if (hasAmazonClient()) {

			service.shutdown();
			service = null;

			amazonClient.shutdown();
			amazonClient = null;

		}

	}

	/** will used json layout by default */
	@Override
	public boolean requiresLayout() {
		return true;
	}

	@Override
	public void append(final LoggingEvent event) {

		// LogLog.warn("event=" + event.getMessage());

		if (!isTriggering(event)) {
			return;
		}

		// LogLog.warn("event=" + event.getLoggerName());

		String message;

		if (hasLayout()) {
			message = getLayout().format(event);
		} else {
			message = event.getRenderedMessage();
		}

		message = Util.forceByteLimit(message, Util.MESSAGE_LIMIT);

		String subject;

		if (hasTopicSubject()) {
			subject = getTopicSubject();
			subject = Util.forceByteLimit(subject, Util.SUBJECT_LIMIT);
		} else {
			subject = null;
		}

		publish(message, subject);

	}

	protected void publish(final String message, final String subject) {
		try {

			final PublishRequest request = new PublishRequest(//
					topicARN, message, subject);

			amazonClient.publishAsync(request);

		} catch (final Exception e) {

			LogLog.error("sns: publish failure", e);

		}
	}

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getCredentials() {
		return credentials;
	}

	public void setCredentials(final String credentials) {
		this.credentials = credentials;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(final String topicName) {
		this.topicName = topicName;
	}

	public String getTopicSubject() {
		return topicSubject;
	}

	public void setTopicSubject(final String topicSubject) {
		this.topicSubject = topicSubject;
	}

	public Evaluator getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(final Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	public int getPoolMin() {
		return poolMin;
	}

	public void setPoolMin(final int poolMin) {
		this.poolMin = poolMin;
	}

	public void setPoolMin(final String poolMinText) {
		this.poolMin = Util.getIntValue(poolMinText, DEFAULT_POOL_MIN);
	}

	public int getPoolMax() {
		return poolMax;
	}

	public void setPoolMax(final int poolMax) {
		this.poolMax = poolMax;
	}

	public void setPoolMax(final String poolMaxText) {
		this.poolMax = Util.getIntValue(poolMaxText, DEFAULT_POOL_MAX);
	}

	public String getEvaluatorProperties() {
		return evaluatorProperties;
	}

	public void setEvaluatorProperties(final String evaluatorProperties) {
		this.evaluatorProperties = evaluatorProperties;
	}

	/** render as JSON; use only @JsonProperty annotated fields */
	@Override
	public String toString() {

		try {

			final ObjectMapper mapper = new ObjectMapper();

			mapper.configure(Feature.INDENT_OUTPUT, true);

			mapper.configure(Feature.USE_ANNOTATIONS, true);

			mapper.configure(Feature.AUTO_DETECT_FIELDS, false);
			mapper.configure(Feature.AUTO_DETECT_GETTERS, false);
			mapper.configure(Feature.AUTO_DETECT_IS_GETTERS, false);

			mapper.configure(Feature.FAIL_ON_EMPTY_BEANS, false);

			return mapper.writeValueAsString(this);

		} catch (final Exception e) {

			LogLog.error("sns: ", e);

			return "{}";

		}

	}

}
