/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

/** provide signature and subject */
public interface Evaluator extends TriggeringEventEvaluator {

	@Override
	boolean isTriggeringEvent(LoggingEvent event);

	/** unique event key for event cache */
	String makeEventSignature(LoggingEvent event);

	void apply(String props);

}