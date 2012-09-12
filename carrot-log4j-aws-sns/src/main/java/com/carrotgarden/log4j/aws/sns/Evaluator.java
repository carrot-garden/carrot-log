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

/** accept configuration properties */
public interface Evaluator extends TriggeringEventEvaluator {

	@Override
	boolean isTriggeringEvent(LoggingEvent event);

	/** evaluator configuration via key=value properties text */
	void setProperties(String propsText);

}
