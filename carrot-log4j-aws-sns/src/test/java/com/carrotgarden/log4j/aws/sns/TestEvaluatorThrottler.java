/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import static org.junit.Assert.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestEvaluatorThrottler {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private static final String MESSAGE = "logging message";

	@Test
	public void testCacheEvition1() throws Exception {

		final Evaluator evaluator = new EvaluatorThrottler();
		evaluator
				.apply(" period=400 \n unit=MILLISECONDS \n mask=LOGGER_NAME,LINE_NUMBER");

		final Logger logger = Logger.getLogger(getClass());

		final LoggingEvent event = new LoggingEvent("", logger, Level.INFO,
				MESSAGE, new Exception());

		assertTrue(evaluator.isTriggeringEvent(event));
		assertFalse(evaluator.isTriggeringEvent(event));
		assertFalse(evaluator.isTriggeringEvent(event));

		Thread.sleep(800);

		assertTrue(evaluator.isTriggeringEvent(event));
		assertFalse(evaluator.isTriggeringEvent(event));
		assertFalse(evaluator.isTriggeringEvent(event));

	}

	@Test
	public void testCacheEvition2() throws Exception {

		final Evaluator evaluator = new EvaluatorThrottler();
		evaluator
				.apply(" period=400 \n unit=MILLISECONDS \n mask=LOGGER_NAME,LINE_NUMBER");

		final Logger logger = Logger.getLogger(getClass());

		final LoggingEvent event = new LoggingEvent("", logger, Level.INFO,
				MESSAGE, null);

		assertTrue(evaluator.isTriggeringEvent(event));
		assertFalse(evaluator.isTriggeringEvent(event));
		assertFalse(evaluator.isTriggeringEvent(event));

		Thread.sleep(800);

		assertTrue(evaluator.isTriggeringEvent(event));
		assertFalse(evaluator.isTriggeringEvent(event));
		assertFalse(evaluator.isTriggeringEvent(event));

	}
}
