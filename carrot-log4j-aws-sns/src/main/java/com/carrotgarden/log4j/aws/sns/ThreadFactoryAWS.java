/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * use daemon threads for quick pool shutdown
 * 
 * note: events can be lost during JVM shutdown
 */
public class ThreadFactoryAWS implements ThreadFactory {

	private final AtomicLong counter = new AtomicLong(0);

	@Override
	public Thread newThread(final Runnable runnable) {

		final Thread thread = new Thread(runnable);

		thread.setName("# amazon-sns " + counter.getAndIncrement());

		thread.setDaemon(true);

		return thread;

	}

}
