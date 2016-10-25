package org.graylog2.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

public class JULProperties {
	private final LogManager manager;
	private final Properties systemProperties;
	private final String prefix;

	public JULProperties(LogManager manager, Properties systemProperties, String prefix) {
		this.manager = manager;
		this.systemProperties = systemProperties;
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getProperty(String key) {
		String fullKey = prefix + "." + key;
		String value = systemProperties.getProperty(fullKey);
		if (value == null || value.isEmpty()) {
			value = manager.getProperty(fullKey);
		}
		return value;
	}

	public Map<String, String> getProperties(String key) {
		String fullKeyPrefix = prefix + "." + key + ".";

		Map<String, String> properties = new HashMap<String, String>();
		int fieldNumber = 0;
		while (true) {
			final String property = manager.getProperty(fullKeyPrefix + fieldNumber);
			if (null == property) {
				break;
			}
			final int index = property.indexOf('=');
			if (-1 != index) {
				properties.put(property.substring(0, index), property.substring(index + 1));
			}
			fieldNumber++;
		}
		for (String propertyName : systemProperties.stringPropertyNames()) {
			if (propertyName.startsWith(fullKeyPrefix)) {
				properties.put(propertyName.substring(fullKeyPrefix.length()),
						systemProperties.getProperty(propertyName));
			}
		}
		return properties;
	}
}
