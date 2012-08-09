/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.Topic;

/**
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

	//

	/** log4j option; amazon credentials file; must exist */
	@JsonProperty
	protected String credentials;

	/** log4j option; SNS topic name; must exist */
	@JsonProperty
	protected String topicName;

	/** log4j option; SNS topic subject; use for instance identity; optional */
	@JsonProperty
	protected String topicSubject;

	/** log4j option; evaluator class name; optional */
	@JsonProperty
	protected String evaluatorClassName;

	/** log4j option; evaluator properties text; optional */
	@JsonProperty
	protected String evaluatorProperties;

	/** log4j option; layout class name; optional */
	@JsonProperty
	protected String layoutClassName;

	/** log4j option; minimum thread pool size; optional */
	@JsonProperty
	protected int poolMin = DEFAULT_POOL_MIN;

	/** log4j option; maximum thread pool size; optional */
	@JsonProperty
	protected int poolMax = DEFAULT_POOL_MAX;

	//

	/**
	 * topic ARN resolved from existng topic name
	 * 
	 * http://aws.amazon.com/sns/faqs/#10
	 */
	@JsonProperty
	protected String topicARN;

	/** AWS SNS client thread pool */
	protected ExecutorService service;

	/** AWS SNS client dedicated to the appender */
	protected AmazonSNSAsync amazonClient;

	/** evaluator configured for appender */
	@JsonProperty
	protected Evaluator evaluator;

	/** appender initialization status */
	@JsonProperty
	protected boolean isValid;

	//

	protected boolean isValid() {
		return isValid;
	}

	protected boolean isTriggering(final LoggingEvent event) {
		return isValid && evaluator.isTriggeringEvent(event);
	}

	protected boolean hasCredentials() {
		return credentials != null;
	}

	protected boolean hasTopicName() {
		return topicName != null;
	}

	protected boolean hasSubject() {
		return topicSubject != null;
	}

	protected boolean hasLayout() {
		return layout != null;
	}

	protected boolean hasTopicARN() {
		return topicARN != null;
	}

	protected boolean hasEvaluatorProperties() {
		return evaluatorProperties != null;
	}

	protected boolean hasAmazonClient() {
		return amazonClient != null;
	}

	protected boolean ensureClientProps() {
		if (hasCredentials()) {
			final File file = new File(getCredentials());
			if (file.exists() && file.isFile() && file.canRead()) {
				return true;
			}
		}
		LogLog.error("ivalid option", new IllegalArgumentException(
				"clientProps"));
		return false;
	}

	protected boolean ensureTopicName() {
		if (hasTopicName()) {
			return true;
		} else {
			LogLog.error("ivalid option", new IllegalArgumentException(
					"topicName"));
			return false;
		}
	}

	protected boolean ensureClient() {

		try {

			final File file = new File(getCredentials());

			final AWSCredentials creds = new PropertiesCredentials(file);

			amazonClient = new AmazonSNSAsyncClient(creds, service);

			return true;

		} catch (final Exception e) {

			LogLog.error("client init failure", e);

			return false;

		}

	}

	protected boolean ensureTopic() {

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

			LogLog.error("unknown topic name", new IllegalArgumentException(
					getTopicName()));

			return false;

		} catch (final Exception e) {

			LogLog.error("topic lookup failure", e);

			return false;

		}

	}

	protected boolean ensureEvaluator() {

		try {

			final Evaluator defaultEvaluator;
			if (hasEvaluatorProperties()) {
				defaultEvaluator = new EvaluatorThrottler(
						getEvaluatorProperties());
			} else {
				defaultEvaluator = new EvaluatorThrottler();
			}

			evaluator = (Evaluator) OptionConverter.instantiateByClassName( //
					getEvaluatorClassName(), //
					Evaluator.class, //
					defaultEvaluator //
					);

			return true;

		} catch (final Exception e) {

			LogLog.error("evaluator init falure", e);

			return false;

		}

	}

	protected boolean ensureLayout() {

		try {

			if (!hasLayout()) {
				setLayout(new LayoutJson());
			}

			return true;

		} catch (final Exception e) {

			LogLog.error("layout init failure", e);

			return false;

		}

	}

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

			LogLog.error("failed to init service", e);

			service = Executors.newCachedThreadPool();

			return true;

		}

	}

	@Override
	public void activateOptions() {

		isValid = true //
				&& ensureService() //
				&& ensureLayout() //
				&& ensureEvaluator() //
				&& ensureClientProps() //
				&& ensureTopicName() //
				&& ensureClient() //
				&& ensureTopic() //
		;

		if (!isValid) {
			LogLog.error("appender is disabled due to invalid configration  : "
					+ getClass().getName());
		}

	}

	@Override
	public synchronized void close() {

		if (hasAmazonClient()) {

			service.shutdown();

			if (future != null) {
				try {
					future.get(3, TimeUnit.SECONDS);
				} catch (final Exception e) {
					LogLog.warn("some events might be lost on close", e);
				}
			}

			amazonClient.shutdown();

		}

	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	public void append(final LoggingEvent event) {

		if (!isTriggering(event)) {
			return;
		}

		// LogLog.error("event=" + event.getLoggerName());

		String message;

		if (hasLayout()) {
			message = getLayout().format(event);
		} else {
			message = event.getRenderedMessage();
		}

		message = Util.forceByteLimit(message, Util.MESSAGE_LIMIT);

		String subject;

		if (hasSubject()) {
			subject = getTopicSubject();
			subject = Util.forceByteLimit(subject, Util.SUBJECT_LIMIT);
		} else {
			subject = null;
		}

		publish(message, subject);

	}

	protected Future<PublishResult> future;

	protected void publish(final String message, final String subject) {
		try {

			final PublishRequest request = new PublishRequest(//
					topicARN, message, subject);

			future = amazonClient.publishAsync(request);

		} catch (final Exception e) {
			LogLog.error("publish failure", e);
		}
	}

	public String getCredentials() {
		return credentials;
	}

	public void setCredentials(final String clientProps) {
		this.credentials = clientProps;
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

	public void setTopicSubject(final String subject) {
		this.topicSubject = subject;
	}

	public String getEvaluatorClassName() {
		return evaluatorClassName;
	}

	public void setEvaluatorClassName(final String evaluatorClassName) {
		this.evaluatorClassName = evaluatorClassName;
	}

	public String getLayoutClassName() {
		return layoutClassName;
	}

	public void setLayoutClassName(final String layoutClassName) {
		this.layoutClassName = layoutClassName;
	}

	public String getEvaluatorProperties() {
		return evaluatorProperties;
	}

	public void setEvaluatorProperties(final String evaluatorParameters) {
		this.evaluatorProperties = evaluatorParameters;
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

	@Override
	public String toString() {

		final ObjectMapper mapper = new ObjectMapper();

		mapper.configure(Feature.AUTO_DETECT_FIELDS, false);
		mapper.configure(Feature.AUTO_DETECT_GETTERS, false);
		mapper.configure(Feature.AUTO_DETECT_IS_GETTERS, false);
		mapper.configure(Feature.INDENT_OUTPUT, true);

		try {
			return mapper.writeValueAsString(this);
		} catch (final Exception e) {
			LogLog.error("", e);
			return "{}";
		}

	}

}
