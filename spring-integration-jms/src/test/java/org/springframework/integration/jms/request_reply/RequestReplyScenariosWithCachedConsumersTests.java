/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms.request_reply;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class RequestReplyScenariosWithCachedConsumersTests extends ActiveMQMultiContextTests {

	private final SimpleMessageConverter converter = new SimpleMessageConverter();

	@Rule
	public LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

	@Test(expected = MessageTimeoutException.class)
	public void messageCorrelationBasedOnRequestMessageIdOptimized() throws Exception {
		ActiveMqTestUtils.prepare();

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("producer-cached-consumers.xml",
				this
				.getClass());
		try {
			RequestReplyExchanger gateway = context
					.getBean("standardMessageIdCopyingConsumerWithOptimization", RequestReplyExchanger.class);
			CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);
			final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);


			final Destination requestDestination = context.getBean("siOutQueueOptimizedA", Destination.class);
			final Destination replyDestination = context.getBean("siInQueueOptimizedA", Destination.class);
			new Thread(() -> {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				jmsTemplate.send(replyDestination, (MessageCreator) session -> {
					TextMessage message = session.createTextMessage();
					message.setText("bar");
					message.setJMSCorrelationID(requestMessage.getJMSMessageID());
					return message;
				});
			}).start();
			gateway.exchange(new GenericMessage<String>("foo"));
		}
		finally {
			context.close();
		}

	}

	@Test
	public void messageCorrelationBasedOnRequestMessageIdNonOptimized() throws Exception {
		ActiveMqTestUtils.prepare();

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("producer-cached-consumers.xml",
				this
				.getClass());
		try {
			RequestReplyExchanger gateway = context
					.getBean("standardMessageIdCopyingConsumerWithoutOptimization", RequestReplyExchanger.class);
			CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);
			final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);


			final Destination requestDestination = context.getBean("siOutQueueNonOptimizedB", Destination.class);
			final Destination replyDestination = context.getBean("siInQueueNonOptimizedB", Destination.class);
			new Thread(() -> {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				jmsTemplate.send(replyDestination, (MessageCreator) session -> {
					TextMessage message = session.createTextMessage();
					message.setText("bar");
					message.setJMSCorrelationID(requestMessage.getJMSMessageID());
					return message;
				});
			}).start();
			org.springframework.messaging.Message<?> siReplyMessage = gateway
					.exchange(new GenericMessage<String>("foo"));
			assertThat(siReplyMessage.getPayload()).isEqualTo("bar");
		}
		finally {
			context.close();
		}
	}

	@Test
	public void messageCorrelationBasedOnRequestCorrelationIdOptimized() throws Exception {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("producer-cached-consumers.xml",
				this
				.getClass());
		try {
			RequestReplyExchanger gateway = context
					.getBean("correlationPropagatingConsumerWithOptimization", RequestReplyExchanger.class);
			CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);

			final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);

			final Destination requestDestination = context.getBean("siOutQueueOptimizedC", Destination.class);
			final Destination replyDestination = context.getBean("siInQueueOptimizedC", Destination.class);
			new Thread(() -> {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				jmsTemplate.send(replyDestination, (MessageCreator) session -> {
					TextMessage message = session.createTextMessage();
					message.setText("bar");
					message.setJMSCorrelationID(requestMessage.getJMSCorrelationID());
					return message;
				});
			}).start();
			org.springframework.messaging.Message<?> siReplyMessage = gateway
					.exchange(new GenericMessage<String>("foo"));
			assertThat(siReplyMessage.getPayload()).isEqualTo("bar");
		}
		finally {
			context.close();
		}
	}

	@Test(expected = MessageTimeoutException.class)
	public void messageCorrelationBasedOnRequestCorrelationIdNonOptimized() throws Exception {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("producer-cached-consumers.xml",
				this
				.getClass());
		try {
			RequestReplyExchanger gateway = context
					.getBean("correlationPropagatingConsumerWithoutOptimization", RequestReplyExchanger.class);
			CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);

			final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);

			final Destination requestDestination = context.getBean("siOutQueueNonOptimizedD", Destination.class);
			final Destination replyDestination = context.getBean("siInQueueNonOptimizedD", Destination.class);
			new Thread(() -> {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				jmsTemplate.send(replyDestination, (MessageCreator) session -> {
					TextMessage message = session.createTextMessage();
					message.setText("bar");
					message.setJMSCorrelationID(requestMessage.getJMSCorrelationID());
					return message;
				});
			}).start();
			gateway.exchange(new GenericMessage<String>("foo"));
		}
		finally {
			context.close();
		}
	}

	@Test
	public void messageCorrelationBasedOnRequestCorrelationIdTimedOutFirstReplyOptimized() throws Exception {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("producer-cached-consumers.xml", this.getClass());
		try {
			RequestReplyExchanger gateway =
					context.getBean("correlationPropagatingConsumerWithOptimizationDelayFirstReply",
							RequestReplyExchanger.class);
			final ConnectionFactory connectionFactory = context
					.getBean("jmsConnectionFactory", ConnectionFactory.class);

			final Destination requestDestination = context.getBean("siOutQueueE", Destination.class);
			final Destination replyDestination = context.getBean("siInQueueE", Destination.class);

			for (int i = 0; i < 3; i++) {
				try {
					gateway.exchange(gateway.exchange(new GenericMessage<String>("foo")));
				}
				catch (Exception e) { /*ignore*/ }

			}

			final CountDownLatch latch = new CountDownLatch(1);
			new Thread(() -> {
				DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
				dmlc.setConnectionFactory(connectionFactory);
				dmlc.setDestination(requestDestination);
				dmlc.setMessageListener((SessionAwareMessageListener<Message>) (message, session) -> {
					String requestPayload = (String) extractPayload(message);
					try {
						TextMessage replyMessage = session.createTextMessage();
						replyMessage.setText(requestPayload);
						replyMessage.setJMSCorrelationID(message.getJMSCorrelationID());
						MessageProducer producer = session.createProducer(replyDestination);
						producer.send(replyMessage);
					}
					catch (Exception e) {
						// ignore. the test will fail
					}
				});
				dmlc.afterPropertiesSet();
				dmlc.start();
				latch.countDown();
			}).start();

			latch.await();


			TestUtils.getPropertyValue(context.getBean("fastGateway"), "handler", JmsOutboundGateway.class)
					.setReceiveTimeout(10000);
			Thread.sleep(1000);
			assertThat(gateway.exchange(new GenericMessage<>("bar")).getPayload()).isEqualTo("bar");
		}
		finally {
			context.close();
		}
	}

	private Object extractPayload(Message jmsMessage) throws JMSException {
		return this.converter.fromMessage(jmsMessage);
	}

}
