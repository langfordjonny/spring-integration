/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.jms.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 *
 */
public class GlobalChannelInterceptorTests {

	@SuppressWarnings("rawtypes")
	@Test
	@Ignore
	public void testJmsChannel(){
		ActiveMqTestUtils.prepare();
		ApplicationContext context = new ClassPathXmlApplicationContext("GlobalChannelInterceptorTests-context.xml", 
																		 GlobalChannelInterceptorTests.class);
		AbstractMessageChannel jmsChannel = context.getBean("jmsChannel", AbstractMessageChannel.class);
		Object interceptors = TestUtils.getPropertyValue((TestUtils.getPropertyValue(jmsChannel, "interceptors")), "interceptors");
		assertNotNull(interceptors);
		assertTrue(interceptors instanceof List);
		assertEquals(1, ((List)interceptors).size());
		assertTrue(((List)interceptors).get(0) instanceof SampleInterceptor);
	}
	
	public static class SampleInterceptor implements ChannelInterceptor{
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			return message;
		}

		public void postSend(Message<?> message, MessageChannel channel,
				boolean sent) {
		}

		public boolean preReceive(MessageChannel channel) {
			return true;
		}

		public Message<?> postReceive(Message<?> message, MessageChannel channel) {
			return message;
		}
		
	}
}
