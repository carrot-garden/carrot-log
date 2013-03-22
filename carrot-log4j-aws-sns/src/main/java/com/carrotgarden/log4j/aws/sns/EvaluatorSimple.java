/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import org.apache.log4j.spi.LoggingEvent;

/**
 * TODO
 */
public class EvaluatorSimple implements Evaluator {

	@Override
	public boolean isTriggeringEvent(final LoggingEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setProperties(final String props) {
		// TODO Auto-generated method stub

	}

}
