/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;

/**
 * original idea from
 * 
 * https://github.com/Aconex/json-log4j-layout
 * 
 * TODO auto size limit
 * 
 */
public class LayoutJson extends Layout {

	public static final int DEFAULT_STACK_DEPTH = 3;

	protected int stackDepth;

	protected String[] mdcKeys = new String[0];

	protected final JsonFactory jsonFactory = new JsonFactory();

	protected String fieldError = "error";

	@Override
	public String format(final LoggingEvent event) {
		try {

			final StringWriter writer = new StringWriter();

			final JsonGenerator jsonGen = createJsonGen(writer);

			jsonGen.writeStartObject();

			write(event, jsonGen);

			writeStack(event, jsonGen);

			writeMDC(event, jsonGen);

			writeNDC(event, jsonGen);

			jsonGen.writeEndObject();

			jsonGen.close();

			return writer.toString();

		} catch (final Exception e) {

			LogLog.error("layout error", e);

			String errorMessage = e.toString();

			errorMessage = Util.forceByteLimit(errorMessage,
					Util.MESSAGE_LIMIT / 2);

			return "{ \"" + fieldError + " : \"" + errorMessage + "\" }";

		}
	}

	protected JsonGenerator createJsonGen(final StringWriter writer)
			throws Exception {

		return jsonFactory.createJsonGenerator(writer);

	}

	protected String fieldLog = "log";
	protected String fieldLevel = "level";
	protected String fieldTime = "time";
	protected String fieldThread = "thread";
	protected String fieldMessage = "message";

	protected void write(final LoggingEvent event, final JsonGenerator jsonGen)
			throws Exception {

		jsonGen.writeStringField(fieldLog, event.getLoggerName());

		jsonGen.writeStringField(fieldLevel, event.getLevel().toString());

		jsonGen.writeStringField(fieldTime,
				new DateTime(event.timeStamp).toString());

		jsonGen.writeStringField(fieldThread, event.getThreadName());

		jsonGen.writeStringField(fieldMessage, event.getMessage().toString());

	}

	protected String fieldNDC = "ndc";

	protected void writeNDC(final LoggingEvent event,
			final JsonGenerator jsonGen) throws Exception {

		if (event.getNDC() != null) {

			jsonGen.writeStringField(fieldNDC, event.getNDC());

		}

	}

	protected String fieldStack = "stack";

	protected void writeStack(final LoggingEvent event,
			final JsonGenerator jsonGen) throws Exception {

		final String[] stackArray = event.getThrowableStrRep();

		if (stackArray == null) {
			return;
		}

		jsonGen.writeFieldName(fieldStack);

		jsonGen.writeStartArray();

		int index = 0;

		for (final String entry : stackArray) {

			jsonGen.writeStartObject();
			jsonGen.writeStringField(Integer.toString(index), entry);
			jsonGen.writeEndObject();

			index++;

			if (index > 3) {
				break;
			}

		}

		jsonGen.writeEndArray();

	}

	protected String fieldMDC = "mdc";

	protected void writeMDC(final LoggingEvent event,
			final JsonGenerator jsonGen) throws IOException {

		if (mdcKeys.length > 0) {

			event.getMDCCopy();

			jsonGen.writeObjectFieldStart(fieldMDC);

			for (final String key : mdcKeys) {

				final Object mdc = event.getMDC(key);

				if (mdc != null) {
					jsonGen.writeStringField(key, mdc.toString());
				}

			}

			jsonGen.writeEndObject();

		}

	}

	public String[] getMdcKeys() {
		return mdcKeys.clone();
	}

	public void setMdcKeys(final String[] mdcKeysArray) {
		if (mdcKeysArray != null) {
			mdcKeys = mdcKeysArray;
		}
	}

	public void setMdcKeys(final String mdcKeysText) {
		if (mdcKeysText != null && mdcKeysText.contains(",")) {
			mdcKeys = mdcKeysText.split(",");
		}
	}

	@Override
	public boolean ignoresThrowable() {
		return false;
	}

	@Override
	public void activateOptions() {
	}

	public int getStackDepth() {
		return stackDepth;
	}

	public void setStackDepth(final int stackDepth) {
		this.stackDepth = stackDepth;
	}

	public void setStackDepth(final String stackDepthText) {
		stackDepth = Util.getIntValue(stackDepthText, DEFAULT_STACK_DEPTH);
	}

	public String getFieldLog() {
		return fieldLog;
	}

	public void setFieldLog(final String fieldLog) {
		this.fieldLog = fieldLog;
	}

	public String getFieldLevel() {
		return fieldLevel;
	}

	public void setFieldLevel(final String fieldLevel) {
		this.fieldLevel = fieldLevel;
	}

	public String getFieldTime() {
		return fieldTime;
	}

	public void setFieldTime(final String fieldTime) {
		this.fieldTime = fieldTime;
	}

	public String getFieldThread() {
		return fieldThread;
	}

	public void setFieldThread(final String fieldThread) {
		this.fieldThread = fieldThread;
	}

	public String getFieldMessage() {
		return fieldMessage;
	}

	public void setFieldMessage(final String fieldMessage) {
		this.fieldMessage = fieldMessage;
	}

	public String getFieldNDC() {
		return fieldNDC;
	}

	public void setFieldNDC(final String fieldNDC) {
		this.fieldNDC = fieldNDC;
	}

	public String getFieldStack() {
		return fieldStack;
	}

	public void setFieldStack(final String fieldStack) {
		this.fieldStack = fieldStack;
	}

	public String getFieldMDC() {
		return fieldMDC;
	}

	public void setFieldMDC(final String fieldMDC) {
		this.fieldMDC = fieldMDC;
	}

	public String getFieldError() {
		return fieldError;
	}

	public void setFieldError(final String fieldError) {
		this.fieldError = fieldError;
	}

}
