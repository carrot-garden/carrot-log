/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.annotate.JsonProperty;

import com.carrotgarden.log4j.aws.sns.Signature.Mask;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * log4j event evaluator that throttles similar events via time based cache
 * eviction
 * 
 * original idea from
 * 
 * https://github.com/insula/log4j-sns
 */
public class EvaluatorThrottler implements Evaluator {

	public static final String KEY_PERIOD = "period";
	public static final String KEY_UNIT = "unit";
	public static final String KEY_MASK = "mask";

	public static final int DEFAULT_PERIOD = 10;
	public static final TimeUnit DEFAULT_UNIT = TimeUnit.MINUTES;
	public static final Signature DEFAULT_SIGNATURE = Signature.DEFAULT;

	/** ( event-signature, is-event-present ) */
	private final Cache<String, Boolean> eventCache;

	@JsonProperty
	protected final long period;
	@JsonProperty
	protected final TimeUnit timeUnit;
	@JsonProperty
	protected Signature signature;

	public EvaluatorThrottler(final Properties props) {

		this( //
				Util.longFrom(props, KEY_PERIOD, DEFAULT_PERIOD), //
				Util.unitFrom(props, KEY_UNIT, DEFAULT_UNIT), //
				new Signature(Util.maskFrom(props, KEY_MASK, Mask.DEFAULT()))
		//
		);

	}

	public EvaluatorThrottler(final String params) {

		this(Util.propsFrom(params));

	}

	public EvaluatorThrottler(final long period, final TimeUnit timeUnit,
			final Signature signature) {

		this.period = period;
		this.timeUnit = timeUnit;
		this.signature = signature;

		this.eventCache = CacheBuilder.newBuilder()
				.expireAfterWrite(period, timeUnit).build();

	}

	public EvaluatorThrottler() {
		this(DEFAULT_PERIOD, DEFAULT_UNIT, Signature.DEFAULT);
	}

	/**  */
	@Override
	public boolean isTriggeringEvent(final LoggingEvent event) {

		final String key = getEventSignature(event);

		final boolean isNew = (eventCache.getIfPresent(key) == null);

		// LogLog.error("isNew=" + isNew);

		if (isNew) {

			eventCache.put(key, true);

			return true;

		} else {

			return false;

		}

	}

	/** event signature based on event set mask */
	@Override
	public String getEventSignature(final LoggingEvent event) {

		final StringBuilder text = new StringBuilder(128);

		final Set<Mask> mask = signature.mask;

		if (mask.contains(Mask.LEVEL)) {
			text.append(event.getLevel().toString());
			text.append("/");
		}

		if (mask.contains(Mask.LOGGER_NAME)) {
			text.append(event.getLoggerName());
			text.append("/");
		}

		final LocationInfo location = event.getLocationInformation();

		if (mask.contains(Mask.CLASS_NAME)) {
			text.append(location.getClassName());
			text.append("/");
		}

		if (mask.contains(Mask.METHOD_NAME)) {
			text.append(location.getMethodName());
			text.append("/");
		}

		if (mask.contains(Mask.LINE_NUMBER)) {
			text.append(location.getLineNumber());
			text.append("/");
		}

		if (mask.contains(Mask.THREAD_NAME)) {
			text.append(event.getThreadName());
			text.append("/");
		}

		if (mask.contains(Mask.STACK_TRACE)) {
			final String[] stackArray = event.getThrowableStrRep();
			if (stackArray != null) {
				for (final String entry : stackArray) {
					text.append("/");
					text.append(entry);
				}
			}
		}

		return text.toString();

	}

}
