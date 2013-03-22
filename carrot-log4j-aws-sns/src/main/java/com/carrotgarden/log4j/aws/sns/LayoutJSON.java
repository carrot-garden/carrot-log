/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
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
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;

/**
 * render log4j event in json format
 * 
 * original idea from
 * 
 * https://github.com/Aconex/json-log4j-layout
 * 
 * TODO auto size limit
 */
public class LayoutJSON extends Layout {

	public static final String IGNORE = "ignore";

	public static final int DEFAULT_STACK_DEPTH = 3;

	//

	/** stack depth reporting limit */
	protected int stackDepth = DEFAULT_STACK_DEPTH;

	/** list of MDC keys which should be reported */
	protected String[] mdcKeys = new String[0];

	protected final JsonFactory jsonFactory = new JsonFactory();

	protected boolean shouldInclude(final String fieldName) {
		if (fieldName == null || fieldName.length() == 0) {
			return false;
		}
		if (IGNORE.equalsIgnoreCase(fieldName)) {
			return false;
		}
		return true;
	}

	//

	@JsonProperty
	protected String usePrettyPrinter = "false";

	//

	@JsonProperty
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

			LogLog.error("sns: layout error", e);

			String errorMessage = e.toString();

			errorMessage = Util.forceByteLimit(errorMessage,
					Util.MESSAGE_LIMIT / 2);

			return "{ \"" + fieldError + " : \"" + errorMessage + "\" }";

		}
	}

	protected JsonGenerator createJsonGen(final StringWriter writer)
			throws Exception {

		final JsonGenerator jsonGen = jsonFactory.createJsonGenerator(writer);

		if ("true".equalsIgnoreCase(getUsePrettyPrinter())) {
			jsonGen.useDefaultPrettyPrinter();
		}

		return jsonGen;

	}

	@JsonProperty
	protected String fieldLogger = "logger";
	@JsonProperty
	protected String fieldLevel = "level";
	@JsonProperty
	protected String fieldTime = "time";
	@JsonProperty
	protected String fieldThread = "thread";
	@JsonProperty
	protected String fieldMessage = "message";
	@JsonProperty
	protected String fieldFile = "file";
	@JsonProperty
	protected String fieldClass = "class";
	@JsonProperty
	protected String fieldMethod = "method";
	@JsonProperty
	protected String fieldLine = "line";

	protected void write(final LoggingEvent event, final JsonGenerator jsonGen)
			throws Exception {

		if (shouldInclude(fieldLogger)) {
			jsonGen.writeStringField(fieldLogger, event.getLoggerName());
		}

		if (shouldInclude(fieldLevel)) {
			jsonGen.writeStringField(fieldLevel, event.getLevel().toString());
		}

		if (shouldInclude(fieldTime)) {
			jsonGen.writeStringField(fieldTime,
					new DateTime(event.timeStamp).toString());
		}

		if (shouldInclude(fieldThread)) {
			jsonGen.writeStringField(fieldThread, event.getThreadName());
		}

		if (shouldInclude(fieldMessage)) {
			jsonGen.writeStringField(fieldMessage, event.getMessage()
					.toString());
		}

		if (shouldInclude(fieldFile)) {
			final LocationInfo location = event.getLocationInformation();
			jsonGen.writeStringField(fieldFile, location.getFileName());
		}

		if (shouldInclude(fieldClass)) {
			final LocationInfo location = event.getLocationInformation();
			jsonGen.writeStringField(fieldClass, location.getClassName());
		}

		if (shouldInclude(fieldMethod)) {
			final LocationInfo location = event.getLocationInformation();
			jsonGen.writeStringField(fieldMethod, location.getMethodName());
		}

		if (shouldInclude(fieldLine)) {
			final LocationInfo location = event.getLocationInformation();
			jsonGen.writeStringField(fieldLine, location.getLineNumber());
		}

	}

	@JsonProperty
	protected String fieldNDC = "ndc";

	protected void writeNDC(final LoggingEvent event,
			final JsonGenerator jsonGen) throws Exception {

		if (!shouldInclude(fieldNDC)) {
			return;
		}

		final String ndcText = event.getNDC();

		if (ndcText == null) {
			return;
		}

		jsonGen.writeStringField(fieldNDC, ndcText);

	}

	@JsonProperty
	protected String fieldStack = "stack";

	protected void writeStack(final LoggingEvent event,
			final JsonGenerator jsonGen) throws Exception {

		if (!shouldInclude(fieldStack)) {
			return;
		}

		final String[] stackArray = event.getThrowableStrRep();

		if (stackArray == null) {
			return;
		}

		jsonGen.writeFieldName(fieldStack);

		jsonGen.writeStartArray();

		int index = 0;

		for (final String entry : stackArray) {

			if (index > stackDepth) {
				break;
			}

			jsonGen.writeStartObject();
			jsonGen.writeStringField(Integer.toString(index), entry);
			jsonGen.writeEndObject();

			index++;

		}

		jsonGen.writeEndArray();

	}

	@JsonProperty
	protected String fieldMDC = "mdc";

	protected void writeMDC(final LoggingEvent event,
			final JsonGenerator jsonGen) throws IOException {

		if (!shouldInclude(fieldMDC)) {
			return;
		}

		if (mdcKeys.length == 0) {
			return;
		}

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

	public String[] getMdcKeys() {
		return mdcKeys.clone();
	}

	public void setMdcKeys(final String[] mdcKeysArray) {
		if (mdcKeysArray == null) {
			mdcKeys = new String[0];
		} else {
			mdcKeys = mdcKeysArray;
		}
	}

	public void setMdcKeys(final String mdcKeysText) {
		if (mdcKeysText == null || mdcKeysText.length() == 0) {
			mdcKeys = new String[0];
		} else {
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

	public String getFieldLogger() {
		return fieldLogger;
	}

	public void setFieldLogger(final String fieldLog) {
		this.fieldLogger = fieldLog;
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

	public String getFieldLine() {
		return fieldLine;
	}

	public void setFieldLine(final String fieldLine) {
		this.fieldLine = fieldLine;
	}

	public String getFieldMethod() {
		return fieldMethod;
	}

	public void setFieldMethod(final String fieldMethod) {
		this.fieldMethod = fieldMethod;
	}

	public String getFieldFile() {
		return fieldFile;
	}

	public void setFieldFile(final String fieldFile) {
		this.fieldFile = fieldFile;
	}

	public String getFieldClass() {
		return fieldClass;
	}

	public void setFieldClass(final String fieldClass) {
		this.fieldClass = fieldClass;
	}

	public String getUsePrettyPrinter() {
		return usePrettyPrinter;
	}

	public void setUsePrettyPrinter(final String usePrettyPrinter) {
		this.usePrettyPrinter = usePrettyPrinter;
	}

}
