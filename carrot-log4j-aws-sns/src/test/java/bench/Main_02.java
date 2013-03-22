/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package bench;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Main_02 {

	static Logger log = Logger.getLogger(Main_02.class);

	public static void main(final String[] args) throws Exception {

		final Properties props = new Properties();

		props.load(Main_02.class
				.getResourceAsStream("/log4j-test-02.properties"));

		PropertyConfigurator.configure(props);

		log.info("init");

		log.fatal("amazon tester");

		log.info("done");

		/** let AWS client finish event publish */
		Thread.sleep(1 * 1000);

	}

}
