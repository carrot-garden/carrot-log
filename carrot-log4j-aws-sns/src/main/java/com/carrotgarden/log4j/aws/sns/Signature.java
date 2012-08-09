/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import static com.carrotgarden.log4j.aws.sns.Signature.Mask.*;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * event signature mask maker
 */
public class Signature {

	/**
	 * include in event signature the following parts
	 */
	public static enum Mask {

		/** {@link LoggingEvent#getLevel()} */
		LEVEL, //

		/** {@link LoggingEvent#getLoggerName()} */
		LOGGER_NAME, //

		/** {@link LoggingEvent#getThreadName()} */
		THREAD_NAME, //

		/** {@link LocationInfo#getFileName()} */
		FILE_NAME, //

		/** {@link LocationInfo#getClassName()} */
		CLASS_NAME, //

		/** {@link LocationInfo#getMethodName()} */
		METHOD_NAME, //

		/** {@link LocationInfo#getLineNumber()} */
		LINE_NUMBER, //

		/** {@link LoggingEvent#getThrowableStrRep()} */
		STACK_TRACE, //

		;

		public static Set<Mask> DEFAULT = Collections.unmodifiableSet(//
				EnumSet.of(LOGGER_NAME, LINE_NUMBER));

	}

	public static final Signature DEFAULT = new Signature(Mask.DEFAULT);

	@JsonProperty
	public final Set<Mask> mask;

	public Signature(final Set<Mask> mask) {
		this.mask = mask;
	}

	/**
	 * make event signature based on event set mask
	 */
	public String make(final LoggingEvent event) {

		final StringBuilder text = new StringBuilder(128);

		if (mask.contains(LEVEL)) {
			text.append(event.getLevel().toString());
			text.append("/");
		}

		if (mask.contains(LOGGER_NAME)) {
			text.append(event.getLoggerName());
			text.append("/");
		}

		if (mask.contains(THREAD_NAME)) {
			text.append(event.getThreadName());
			text.append("/");
		}

		if (mask.contains(FILE_NAME)) {
			final LocationInfo location = event.getLocationInformation();
			text.append(location.getFileName());
			text.append("/");
		}

		if (mask.contains(CLASS_NAME)) {
			final LocationInfo location = event.getLocationInformation();
			text.append(location.getClassName());
			text.append("/");
		}

		if (mask.contains(METHOD_NAME)) {
			final LocationInfo location = event.getLocationInformation();
			text.append(location.getMethodName());
			text.append("/");
		}

		if (mask.contains(LINE_NUMBER)) {
			final LocationInfo location = event.getLocationInformation();
			text.append(location.getLineNumber());
			text.append("/");
		}

		if (mask.contains(STACK_TRACE)) {
			final String[] stackArray = event.getThrowableStrRep();
			if (stackArray != null) {
				for (final String entry : stackArray) {
					text.append("/");
					text.append(entry);
				}
			}
		}

		final String value = text.toString();

		// LogLog.error("value=" + value);

		return value;

	}

}
