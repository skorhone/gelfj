package org.graylog2.sender;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import org.graylog2.message.GelfMessage;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class GelfAMQPSender implements GelfSender {
	private volatile boolean shutdown = false;

	private final ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	private AMQPBufferManager bufferManager;
	private final String exchangeName;
	private final String routingKey;
	private final int maxRetries;

	public GelfAMQPSender(GelfSenderConfiguration configuration, boolean enableRetry)
			throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
		this.factory = new ConnectionFactory();
		this.factory.setUri(configuration.getAmqpURI());
		this.exchangeName = configuration.getAmqpExchangeName();
		this.routingKey = configuration.getAmqpRoutingKey();
		this.maxRetries = enableRetry ? configuration.getMaxRetries() : 0;
		this.bufferManager = new AMQPBufferManager();
	}

	public void sendMessage(GelfMessage message) throws GelfSenderException {
		String uuid = UUID.randomUUID().toString();
		String messageid = "gelf" + message.getHost() + message.getFacility() + message.getTimestamp() + uuid;

		int tries = 0;
		Exception lastException = null;
		do {
			try {
				if (!isConnected()) {
					connect();
				}
				BasicProperties.Builder propertiesBuilder = new BasicProperties.Builder();
				propertiesBuilder.contentType("application/json; charset=utf-8");
				propertiesBuilder.contentEncoding("gzip");
				propertiesBuilder.messageId(messageid);
				propertiesBuilder.timestamp(new Date(message.getJavaTimestamp()));
				BasicProperties properties = propertiesBuilder.build();
				channel.basicPublish(exchangeName, routingKey, properties,
						bufferManager.toAMQPBuffer(message.toJson()));
				channel.waitForConfirms();
				return;
			} catch (Exception exception) {
				closeConnection();
				tries++;
				lastException = exception;
			}
		} while (tries <= maxRetries);

		throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, lastException);
	}

	private synchronized void connect() throws IOException, GelfSenderException {
		if (shutdown) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_SHUTTING_DOWN);
		}
		connection = factory.newConnection();
		channel = connection.createChannel();
		channel.confirmSelect();
	}

	private boolean isConnected() {
		return channel != null;
	}

	public synchronized void close() {
		if (!shutdown) {
			shutdown = true;
			closeConnection();
		}
	}

	private void closeConnection() {
		try {
			channel.close();
		} catch (Exception e) {
		}
		try {
			connection.close();
		} catch (Exception e) {
		}
		channel = null;
		connection = null;
	}

	public static class AMQPBufferManager extends AbstractBufferManager {
		public byte[] toAMQPBuffer(String message) {
			return gzipMessage(message);
		}
	}
}
