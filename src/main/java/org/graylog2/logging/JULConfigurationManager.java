package org.graylog2.logging;

import org.graylog2.field.FieldExtractor;
import org.graylog2.field.FieldExtractors;
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
		configuration.setExtractStacktrace("true".equalsIgnoreCase(properties.getProperty("extractStacktrace")));

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
			configuration.setThreadedQueueMaxDepth(Integer.parseInt(queueMaxDepth));
		}
		if (queueTimeout != null) {
			configuration.setThreadedQueueTimeout(Integer.parseInt(queueTimeout));
		}
		if (maxRetries != null) {
			configuration.setMaxRetries(Integer.parseInt(maxRetries));
		}
		return configuration;
	}

	public static GelfFormatterConfiguration getGelfFormatterConfiguration(JULProperties properties) {
		GelfFormatterConfiguration gelfFormatterConfiguration = new GelfFormatterConfiguration();

		String fieldExtractorType = properties.getProperty("fieldExtractor");
		FieldExtractor fieldExtractor = fieldExtractorType != null ? FieldExtractors.getInstance(fieldExtractorType)
				: FieldExtractors.getDefaultInstance();

		gelfFormatterConfiguration
				.setIncludeLocation(!"false".equalsIgnoreCase(properties.getProperty("includeLocation")));
		gelfFormatterConfiguration.setFieldExtractor(fieldExtractor);

		return gelfFormatterConfiguration;
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