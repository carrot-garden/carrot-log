/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package bench;

import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Main_03 {

	static Logger log = Logger.getLogger(Main_03.class);

	static void test() {

		log.info("amazon tester 1 ");
		log.warn("amazon tester 2");
		log.error("amazon tester 3");

	}

	public static void main(final String[] args) throws Exception {

		final Properties props = new Properties();

		props.load(Main_03.class
				.getResourceAsStream("/log4j-test-03.properties"));

		PropertyConfigurator.configure(props);

		final Logger logger = Logger.getRootLogger();
		final Appender appender = logger.getAppender("SNS");

		/** only first invocation is published */
		test();
		test();
		test();
		test();
		test();
		test();

		// log.info("appender \n" + appender);

		/** let AWS client finish event publish */
		Thread.sleep(1 * 1000);

		appender.close();

		log.debug("test");

	}

}
