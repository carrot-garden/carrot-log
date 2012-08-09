/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.log4j.aws.sns;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * original idea from
 * 
 * https://github.com/apetresc/amazon-sns-log4j-appender
 * 
 * https://github.com/insula/log4j-sns
 * 
 */
public class TestAppender {

	private static final Logger log = Logger.getLogger(TestAppender.class);

	private static final String LOGGER = "tester";
	private static final String SUBJECT = "subject";
	private static final String MESSAGE = "logging message";
	private static final String EXCEPTION = "abracadabra";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAppendLoggingEvent() {

		final String topicArn = "arn:topic:test";

		final AmazonSNSAsync amazonClient = mock(AmazonSNSAsync.class);

		when(amazonClient.createTopic(any(CreateTopicRequest.class)))
				.thenReturn(new CreateTopicResult().withTopicArn(topicArn));

		final ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor
				.forClass(PublishRequest.class);

		when(amazonClient.publishAsync(requestCaptor.capture())).thenReturn(
				null);

		final Appender appender = new Appender();

		appender.topicName = "test";
		appender.topicARN = topicArn;
		appender.amazonClient = amazonClient;
		appender.isActivated = true;
		appender.topicSubject = SUBJECT;

		appender.ensureLayout();
		appender.ensureEvaluator();
		appender.ensureTopicName();

		final Logger logger = Logger.getLogger(LOGGER);

		final LoggingEvent event = new LoggingEvent("", logger, Level.WARN,
				MESSAGE, new Exception(EXCEPTION));

		appender.append(event);
		appender.append(event);
		appender.append(event);

		final PublishRequest publishRequest = requestCaptor.getValue();

		log.debug("subject=" + publishRequest.getSubject());
		log.debug("message=" + publishRequest.getMessage());

		assertEquals(SUBJECT, publishRequest.getSubject());
		assertTrue(publishRequest.getMessage().contains(MESSAGE));
		assertTrue(publishRequest.getMessage().contains("java.lang.Exception"));
		assertTrue(publishRequest.getMessage().contains(EXCEPTION));

		verify(amazonClient, times(1)).publishAsync(any(PublishRequest.class));

	}

}
