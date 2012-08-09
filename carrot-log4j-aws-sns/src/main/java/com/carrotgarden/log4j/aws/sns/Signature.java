/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;

/** event signature mask */
public class Signature {

	public static enum Mask {

		LEVEL, //

		LOGGER_NAME, //

		CLASS_NAME, //

		THREAD_NAME, //

		LINE_NUMBER, //

		METHOD_NAME, //

		STACK_TRACE, //

		;

		public static EnumSet<Mask> DEFAULT() {
			return EnumSet.of(LOGGER_NAME, LINE_NUMBER).clone();
		}

	}

	public static final Signature DEFAULT = new Signature(Mask.DEFAULT());

	@JsonProperty
	public final Set<Mask> mask;

	public Signature(final Set<Mask> mask) {
		this.mask = Collections.unmodifiableSet(mask);
	}

}
