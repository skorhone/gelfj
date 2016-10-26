package org.graylog2.logging;

import org.graylog2.message.GelfMessageBuilderConfiguration;
import org.graylog2.sender.GelfSenderConfiguration;

public class JULConfigurationManager {
	private JULConfigurationManager() {
	}

	public static GelfMessageBuilderConfiguration getGelfMessageBuilderConfiguration(JULProperties properties) {
		String originHost = properties.getProperty("originHost");
		String facility = properties.getProperty("facility");

		GelfMessageBuilderConfiguration configuration = new GelfMessageBuilderConfiguration();
		if (originHost != null) {
			configuration.setOriginHost(originHost);
		}
		configuration.setFacility(facility);
		configuration.addAdditionalFields(properties.getProperties("additionalField"));

		return configuration;
	}

	public static GelfSenderConfiguration getGelfSenderConfiguration(JULProperties properties) {
		String maxRetries = properties.getProperty("maxRetries");
		if (maxRetries == null) {
			maxRetries = properties.getProperty("amqpMaxRetries");
		}
		String queueMaxDepth = properties.getProperty("threadedQueueMaxDepth");
		String queueTimeout = properties.getProperty("threadedQueueTimeout");

		GelfSenderConfiguration configuration = new GelfSenderConfiguration();

		configuration.setTargetURI(getURI(properties));
		configuration.setThreaded("true".equalsIgnoreCase(properties.getProperty("threaded")));
		if (queueMaxDepth != null) {
			configuration.setThreadedQueueMaxDepth(Integer.valueOf(queueMaxDepth));
		}
		if (queueTimeout != null) {
			configuration.setThreadedQueueTimeout(Integer.valueOf(queueTimeout));
		}
		if (maxRetries != null) {
			configuration.setMaxRetries(Integer.valueOf(maxRetries));
		}
		return configuration;
	}

	private static String getURI(JULProperties properties) {
		String uri = properties.getProperty("targetURI");
		if (uri == null) {
			// All this is for backwards compatibility :-(
			String host = properties.getProperty("graylogHost");
			String amqpURI = properties.getProperty("amqpURI");
			if (host != null) {
				if (host.indexOf(':') == -1) {
					host = GelfSenderConfiguration.DEFAULT_PROTOCOL + "://" + host;
				}
				String port = properties.getProperty("graylogPort");
				if (port != null) {
					uri = host + ":" + port;
				}
			} else if (amqpURI != null) {
				String exchange = properties.getProperty("amqpExchangeName");
				String routingKey = properties.getProperty("amqpRoutingKey");
				uri = amqpURI + (amqpURI.indexOf('?') == -1 ? '?' : '&') + "exchange=" + exchange + "&routingKey="
						+ routingKey;
			}
		}
		return uri;
	}
}