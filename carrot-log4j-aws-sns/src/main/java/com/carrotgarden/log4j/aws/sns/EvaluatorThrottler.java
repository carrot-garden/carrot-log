/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.annotate.JsonProperty;

import com.carrotgarden.log4j.aws.sns.Signature.Mask;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * log4j event evaluator that throttles similar events via after-write time
 * based cache eviction
 * 
 * original idea from
 * 
 * https://github.com/insula/log4j-sns
 */
public class EvaluatorThrottler implements Evaluator {

	public static final String PROP_PERIOD = "period";
	public static final String PROP_UNIT = "unit";
	public static final String PROP_MASK = "mask";

	public static final int DEFAULT_PERIOD = 10;
	public static final TimeUnit DEFAULT_UNIT = TimeUnit.MINUTES;
	public static final Signature DEFAULT_SIGNATURE = Signature.DEFAULT;

	/** cache after-write retention period value */
	@JsonProperty
	protected long period = DEFAULT_PERIOD;

	/** cache after-write retention period unit */
	@JsonProperty
	protected TimeUnit timeUnit = DEFAULT_UNIT;

	/** cache event signature */
	@JsonProperty
	protected Signature signature = DEFAULT_SIGNATURE;

	/** event cache map : [ event-signature, is-event-present ] */
	private Cache<String, Boolean> eventCache;

	protected void ensureCache() {

		eventCache = CacheBuilder.newBuilder()
				.expireAfterWrite(period, timeUnit).build();

	}

	public EvaluatorThrottler() {
		ensureCache();
	}

	@Override
	public void setProperties(final String propsText) {

		if (propsText == null) {
			return;
		}

		final Properties props = Util.propsFrom(propsText);

		period = Util.longFrom(props, PROP_PERIOD, DEFAULT_PERIOD);

		timeUnit = Util.unitFrom(props, PROP_UNIT, DEFAULT_UNIT);

		signature = new Signature(Util.maskFrom(props, PROP_MASK, Mask.DEFAULT));

		ensureCache();

	}

	/**  */
	@Override
	public boolean isTriggeringEvent(final LoggingEvent event) {

		final String key = signature.make(event);

		final boolean isNew = (eventCache.getIfPresent(key) == null);

		// LogLog.error("isNew=" + isNew);

		if (isNew) {

			eventCache.put(key, true);

			return true;

		} else {

			return false;

		}

	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(final long period) {
		this.period = period;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(final TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public Signature getSignature() {
		return signature;
	}

	public void setSignature(final Signature signature) {
		this.signature = signature;
	}

}
