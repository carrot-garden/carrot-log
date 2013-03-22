/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.helpers.LogLog;

import com.carrotgarden.log4j.aws.sns.Signature.Mask;

public class Util {

	protected final static Charset UTF_8 = Charset.forName("UTF-8");

	/** bytes */
	public static final int MESSAGE_LIMIT = 64 * 1024;

	/** bytes */
	public static final int SUBJECT_LIMIT = 100;

	/** truncate if too long */
	public static String forceByteLimit(final String text, final int size) {

		final byte[] array = text.getBytes(UTF_8);

		if (array.length > size) {
			return new String(array, 0, size, UTF_8);
		} else {
			return text;
		}

	}

	/**
	 * http://aws.amazon.com/sns/faqs/#10
	 * 
	 * "arn:aws:sns:us-east-1:1234567890123456:mytopic" -> "mytopic"
	 * */
	public static String topicNameFromARN(final String topicARN) {

		final int index = topicARN.lastIndexOf(":") + 1;

		return topicARN.substring(index);

	}

	public static int getIntProperty(final String propName,
			final int propDefault) {

		final String propValue = System.getProperty(propName);

		try {
			return Integer.parseInt(propValue);
		} catch (final Exception e) {
			return propDefault;
		}

	}

	public static int getIntValue(final String propValue, final int propDefault) {

		try {
			return Integer.parseInt(propValue);
		} catch (final Exception e) {
			LogLog.error("wrong value", e);
			return propDefault;
		}

	}

	public static Properties propsFrom(final String text) {

		final Properties props = new Properties();

		try {

			final InputStream input = new ByteArrayInputStream(
					text.getBytes(UTF_8));

			props.load(input);

		} catch (final Exception e) {
			LogLog.error("invalid properties : " + text, e);
		}

		return props;

	}

	public static TimeUnit unitFrom(final Properties props, final String key,
			final TimeUnit defVal) {

		final String value = props.getProperty(key);

		try {
			return TimeUnit.valueOf(value.trim());
		} catch (final Exception e) {
			LogLog.error("invalid entry : " + key + "=" + value, e);
			return defVal;
		}

	}

	public static long longFrom(final Properties props, final String key,
			final long defVal) {

		final String value = props.getProperty(key);

		try {
			return Long.parseLong(value.trim());
		} catch (final Exception e) {
			LogLog.error("invalid entry : " + key + "=" + value, e);
			return defVal;
		}

	}

	public static Set<Mask> maskFrom(final Properties props, final String key,
			final Set<Mask> defVal) {

		final String value = props.getProperty(key);

		final EnumSet<Mask> mask = EnumSet.noneOf(Mask.class);

		try {

			final String[] nameArray = value.split(",");

			for (final String name : nameArray) {

				try {
					mask.add(Mask.valueOf(name.trim()));
				} catch (final Exception e) {
					LogLog.error("invalid mask name : " + name, e);
				}

			}

			return mask;

		} catch (final Exception e) {

			LogLog.error("invalid entry : " + key + "/" + value, e);

			return defVal;
		}

	}

}
