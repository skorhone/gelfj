package org.graylog2.logging;

import org.graylog2.host.HostConfiguration;
import org.graylog2.sender.GelfSenderConfiguration;

public class JULConfigurationManager {
	private JULConfigurationManager() {
	}

	public static HostConfiguration getHostConfiguration(JULProperties properties) {
		String originHost = properties.getProperty("originHost");
		String facility = properties.getProperty("facility");

		HostConfiguration configuration = new HostConfiguration();
		if (originHost != null) {
			configuration.setOriginHost(originHost);
		}
		configuration.setFacility(facility);
		return configuration;
	}

	public static GelfSenderConfiguration getGelfSenderConfiguration(JULProperties properties) {
		String port = properties.getProperty("graylogPort");
		String sendBufferSize = properties.getProperty("socketSendBufferSize");
		String maxRetries = properties.getProperty("maxRetries");
		if (maxRetries == null) {
			maxRetries = properties.getProperty("amqpMaxRetries");
		}
		String sendTimeout = properties.getProperty("sendTimeout");
		String queueMaxDepth = properties.getProperty("threadedQueueMaxDepth");
		String queueTimeout = properties.getProperty("threadedQueueTimeout");

		GelfSenderConfiguration configuration = new GelfSenderConfiguration();
		configuration.setGraylogURI(properties.getProperty("graylogHost"));
		if (port != null) {
			configuration.setGraylogPort(Integer.parseInt(port));
		}
		configuration.setAmqpURI(properties.getProperty("amqpURI"));
		if (sendBufferSize != null) {
			configuration.setSocketSendBufferSize(Integer.parseInt(sendBufferSize));
		}
		configuration.setTcpKeepalive("true".equalsIgnoreCase(properties.getProperty("tcpKeepalive")));
		configuration.setThreaded("true".equalsIgnoreCase(properties.getProperty("threaded")));
		if (sendTimeout != null) {
			configuration.setSendTimeout(Integer.valueOf(sendTimeout));
		}
		if (queueMaxDepth != null) {
			configuration.setThreadedQueueMaxDepth(Integer.valueOf(queueMaxDepth));
		}
		if (queueTimeout != null) {
			configuration.setThreadedQueueTimeout(Integer.valueOf(queueTimeout));
		}
		configuration.setAmqpExchangeName(properties.getProperty("amqpExchangeName"));
		configuration.setAmqpRoutingKey(properties.getProperty("amqpRoutingKey"));
		if (maxRetries != null) {
			configuration.setMaxRetries(Integer.valueOf(maxRetries));
		}
		return configuration;
	}
}