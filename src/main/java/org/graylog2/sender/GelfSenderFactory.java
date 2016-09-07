package org.graylog2.sender;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class GelfSenderFactory {
	private GelfSenderFactory() {
	}

	public static GelfSenderFactory getInstance() {
		return new GelfSenderFactory();
	}

	public GelfSender createSender(GelfSenderConfiguration configuration) {
		GelfSender gelfSender = null;
		if (configuration.getGraylogHost() == null && configuration.getAmqpURI() == null) {
			throw new GelfSenderConfigurationException("Graylog2 hostname and amqp uri are empty!");
		}
		if (configuration.getGraylogHost() != null && configuration.getAmqpURI() != null) {
			throw new GelfSenderConfigurationException("Graylog2 hostname and amqp uri are both informed!");
		}
		try {
			if ("tcp".equalsIgnoreCase(configuration.getProtocol())) {
				gelfSender = new GelfTCPSender(configuration);
			} else if ("udp".equalsIgnoreCase(configuration.getProtocol())) {
				gelfSender = new GelfUDPSender(configuration);
			} else if ("amqp".equalsIgnoreCase(configuration.getProtocol())) {
				gelfSender = new GelfAMQPSender(configuration);
			} else {
				throw new GelfSenderConfigurationException("Unsupported protocol: " + configuration.getProtocol());
			}
			if (configuration.isThreaded()) {
				gelfSender = new GelfThreadedSender(gelfSender, configuration.getThreadedQueueTimeout(),
						configuration.getThreadedQueueMaxDepth());
			}
			return gelfSender;
		} catch (UnknownHostException e) {
			throw new GelfSenderConfigurationException("Unknown Graylog2 hostname:" + configuration.getGraylogHost(),
					e);
		} catch (SocketException e) {
			throw new GelfSenderConfigurationException("Socket exception", e);
		} catch (IOException e) {
			throw new GelfSenderConfigurationException("IO exception", e);
		} catch (URISyntaxException e) {
			throw new GelfSenderConfigurationException("AMQP uri exception", e);
		} catch (NoSuchAlgorithmException e) {
			throw new GelfSenderConfigurationException("AMQP algorithm exception", e);
		} catch (KeyManagementException e) {
			throw new GelfSenderConfigurationException("AMQP key exception", e);
		} catch (Exception e) {
			throw new GelfSenderConfigurationException("Unknown exception while configuring GelfSender", e);
		}
	}
}
