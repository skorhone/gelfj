package org.graylog2.sender;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import org.graylog2.message.GelfMessage;

public class GelfAMQPSender implements GelfSender {
	private volatile boolean shutdown = false;

	private final ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	private AMQPBufferBuilder bufferBuilder;
	private final String exchangeName;
	private final String routingKey;
	private final int maxRetries;
	private final String channelMutex = "channelMutex";

	public GelfAMQPSender(GelfSenderConfiguration configuration, boolean enableRetry)
			throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
		factory = new ConnectionFactory();
		factory.setUri(configuration.getAmqpURI());

		this.bufferBuilder = new AMQPBufferBuilder();
		this.exchangeName = configuration.getAmqpExchangeName();
		this.routingKey = configuration.getAmqpRoutingKey();
		this.maxRetries = enableRetry ? configuration.getMaxRetries() : 0;
	}

	public void sendMessage(GelfMessage message) throws GelfSenderException {
		if (shutdown) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_SHUTTING_DOWN);
		}
		if (!message.isValid()) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_MESSAGE_NOT_VALID);
		}
		// set unique id to identify duplicates after connection failure
		String uuid = UUID.randomUUID().toString();
		String messageid = "gelf" + message.getHost() + message.getFacility() + message.getTimestamp() + uuid;

		int tries = 0;
		Exception lastException = null;
		do {
			try {
				// establish the connection the first time
				if (channel == null) {
					synchronized (channelMutex) {
						if (channel == null) {
							connection = factory.newConnection();
							channel = connection.createChannel();
							channel.confirmSelect();
						}
					}
				}

				BasicProperties.Builder propertiesBuilder = new BasicProperties.Builder();
				propertiesBuilder.contentType("application/json; charset=utf-8");
				propertiesBuilder.contentEncoding("gzip");
				propertiesBuilder.messageId(messageid);
				propertiesBuilder.timestamp(new Date(message.getJavaTimestamp()));
				BasicProperties properties = propertiesBuilder.build();

				channel.basicPublish(exchangeName, routingKey, properties,
						bufferBuilder.toAMQPBuffer(message.toJson()).array());
				channel.waitForConfirms();
				return;
			} catch (Exception exception) {
				channel = null;
				tries++;
				lastException = exception;
			}
		} while (tries <= maxRetries || maxRetries < 0);

		throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, lastException);
	}

	public void close() {
		shutdown = true;
		try {
			channel.close();
		} catch (Exception e) {
		}
		try {
			connection.close();
		} catch (Exception e) {
		}
	}

	public static class AMQPBufferBuilder extends BufferBuilder {
		public ByteBuffer toAMQPBuffer(String message) {
			byte[] messageBytes = gzipMessage(message);
			ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length);
			buffer.put(messageBytes);
			buffer.flip();
			return buffer;
		}
	}
}
