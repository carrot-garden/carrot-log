/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
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

		log.fatal("amazon tester");
		log.fatal("amazon tester");
		log.fatal("amazon tester");

	}

	public static void main(final String[] args) throws Exception {

		final Properties props = new Properties();

		props.load(Main_03.class
				.getResourceAsStream("/log4j-test-03.properties"));

		PropertyConfigurator.configure(props);

		final Logger logger = Logger.getRootLogger();
		final Appender appender = logger.getAppender("SNS");

		log.info("appender \n" + appender);

		log.debug("init");

		/** only first invocation is published */
		test();
		test();
		test();
		test();
		test();
		test();

		log.debug("done");

		/** let AWS client finish event publish */
		Thread.sleep(1 * 1000);

	}

}
