/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;

/**
 * original idea from
 * 
 * https://github.com/Aconex/json-log4j-layout
 */
public class TestLayoutJSON {

	private static final Logger log = Logger.getLogger(TestLayoutJSON.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private LayoutJSON jsonLayout;

	@Before
	public void setup() {

		jsonLayout = new LayoutJSON();

		jsonLayout.activateOptions();

	}

	@Test
	public void validateMDCKeys() {

		jsonLayout.setMdcKeys("UserID,RequestID,IPAddress");

		final String[] mdckeys = jsonLayout.getMdcKeys();

		assertEquals(mdckeys.length, 3);

		assertEquals(mdckeys[0], "UserID");
		assertEquals(mdckeys[1], "RequestID");
		assertEquals(mdckeys[2], "IPAddress");

	}

	@Test
	public void emptyMDCStringShouldResultInEmptyArray() {

		jsonLayout.setMdcKeys("");
		String[] mdckeys = jsonLayout.getMdcKeys();
		assertEquals(mdckeys.length, 0);

		jsonLayout.setMdcKeys((String) null);
		mdckeys = jsonLayout.getMdcKeys();
		assertEquals(mdckeys.length, 0);

	}

	@Test
	public void validateBasicLogStructure() {

		final LoggingEvent event = createDefaultLoggingEvent();

		final String logOutput = jsonLayout.format(event);

		validateBasicLogOutput(logOutput, event);

	}

	@Test
	public void validateMDCValueIsLoggedCorrectly() {

		final Map<String, String> mdcMap = createMapAndPopulateMDC();

		final Set<String> mdcKeySet = mdcMap.keySet();

		final LoggingEvent event = createDefaultLoggingEvent();

		jsonLayout.setMdcKeys(Joiner.on(",").join(mdcKeySet));

		final String output = jsonLayout.format(event);

		validateBasicLogOutput(output, event);

		assertEquals(jsonLayout.getMdcKeys().length, mdcKeySet.size());

		assertArrayEquals(jsonLayout.getMdcKeys(), mdcKeySet.toArray());

		validateMDCValues(output);

	}

	@Test
	public void validateNDCValueIsLoggedCorrectly() {

		populateNDC();

		final LoggingEvent event = createDefaultLoggingEvent();
		final String logOutput = jsonLayout.format(event);

		validateBasicLogOutput(logOutput, event);
		assertEquals(NDC.getDepth(), (2));
		validateNDCValues(logOutput);

	}

	@Test
	public void validateExceptionIsLoggedCorrectly() {

		final LoggingEvent event = createDefaultLoggingEventWithException();

		final String output = jsonLayout.format(event);

		validateExceptionInlogOutput(output);

	}

	private void validateBasicLogOutput(final String logOutput,
			final LoggingEvent event) {
		validateLevel(logOutput, event);
		validateLogger(logOutput, event);
		validateThreadName(logOutput, event);
		validateMessage(logOutput, event);
		validateNewLine(logOutput, event);

	}

	private void validateNewLine(final String output, final LoggingEvent event) {

		// assertTrue("every line in a log must end with a new line character",
		// output.endsWith("\n"));

	}

	private void validateLevel(final String logOutput, final LoggingEvent event) {

		if (event.getLevel() != null) {

			final String partialOutput = "\"level\":\""
					+ event.getLevel().toString() + "\"";

			assertTrue(logOutput.contains(partialOutput));

		} else {

			fail("Expected the level value to be set in the logging event");

		}
	}

	private void validateLogger(final String output, final LoggingEvent event) {
		if (event.getLogger() != null) {

			final String partial = "\"" + jsonLayout.fieldLogger + "\":\""
					+ event.getLoggerName() + "\"";

			assertTrue(output.contains(partial));

		} else {

			fail("Expected the logger to be set in the logging event");

		}
	}

	private void validateThreadName(final String output,
			final LoggingEvent event) {

		if (event.getThreadName() != null) {

			final String partial = "\"" + jsonLayout.fieldThread + "\":\""
					+ event.getThreadName() + "\"";

			assertTrue(output.contains(partial));

		} else {

			fail("Expected the threadname to be set in the logging event");

		}
	}

	private void validateMessage(final String logOutput,
			final LoggingEvent event) {
		if (event.getMessage() != null) {
			final String partialOutput = "\"message\":\"" + event.getMessage()
					+ "\"";
			assertTrue(logOutput.contains(partialOutput));
		} else {
			fail("Expected the message to be set in the logging event");
		}
	}

	private void validateMDCValues(final String output) {

		final String partial = "\"" + jsonLayout.fieldMDC + "\":{\"UserId\":\""
				+ "U1" + "\",\"ProjectId\":\"" + "P1" + "\"}";

		assertTrue(output.contains(partial));

	}

	private void validateNDCValues(final String output) {

		final String partial = "\"" + jsonLayout.fieldNDC + "\":\"NDC1 NDC2\"";

		assertTrue(output.contains(partial));

	}

	private void validateExceptionInlogOutput(final String output) {

		final List<String> partial = new ArrayList<String>();

		partial.add("java.lang.IllegalArgumentException: Test Exception in event");

		// partial.add("org.elasticflume.log4j.JSONLayoutTest.createDefaultLoggingEventWithException(JSONLayoutTest.java:");

		// partial.add("at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)");
		// partial.add("at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39");
		// partial.add("at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)");
		// partial.add("at java.lang.reflect.Method.invoke(Method.java:597)");

		for (final String entry : partial) {

			assertTrue(output.contains(entry));

		}

	}

	private LoggingEvent createDefaultLoggingEvent() {
		return new LoggingEvent("", log, Level.INFO, "Hello World", null);
	}

	private LoggingEvent createDefaultLoggingEventWithException() {
		return new LoggingEvent("", log, Level.INFO, "Hello World",
				new IllegalArgumentException("Test Exception in event"));
	}

	private Map<String, String> createMapAndPopulateMDC() {
		final Map<String, String> mdcMap = new LinkedHashMap<String, String>();
		mdcMap.put("UserId", "U1");
		mdcMap.put("ProjectId", "P1");

		for (final Map.Entry<String, String> entry : mdcMap.entrySet()) {
			MDC.put(entry.getKey(), entry.getValue());
		}
		return mdcMap;
	}

	private void populateNDC() {
		NDC.push("NDC1");
		NDC.push("NDC2");
	}

}
