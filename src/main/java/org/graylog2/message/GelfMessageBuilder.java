package org.graylog2.message;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class GelfMessageBuilder {
	public static final String LOGGER_NAME_FIELD = "loggerName";
	public static final String THREAD_NAME_FIELD = "threadName";
	public static final String NATIVE_LEVEL_FIELD = "nativeLevel";
	public static final String CLASS_NAME_FIELD = "className";
	public static final String METHOD_NAME_FIELD = "methodName";
	private long timestamp;
	private String message;
	private Throwable throwable;
	private GelfMessageBuilderConfiguration configuration;
	private String level;
	private Map<String, Object> additionalFields;

	public GelfMessageBuilder(GelfMessageBuilderConfiguration configuration) {
		this.configuration = configuration;
	}

	public GelfMessageBuilder setMessage(String message) {
		this.message = message;
		return this;
	}

	public GelfMessageBuilder setThrowable(Throwable throwable) {
		this.throwable = throwable;
		return this;
	}

	public GelfMessageBuilder setLevel(String level) {
		this.level = level;
		return this;
	}

	public GelfMessageBuilder setTimestamp(long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public GelfMessageBuilder addField(String key, Object value) {
		if (additionalFields == null) {
			additionalFields = new HashMap<String, Object>();
		}
		additionalFields.put(key, value);
		return this;
	}

	public GelfMessageBuilder addFields(Map<String, ? extends Object> fields) {
		if (fields != null) {
			if (additionalFields == null) {
				additionalFields = new HashMap<String, Object>();
			}
			additionalFields.putAll(fields);
		}
		return this;
	}

	public GelfMessage build() throws GelfMessageBuilderException {
		GelfMessage gelfMessage = new GelfMessage();
		gelfMessage.setJavaTimestamp(timestamp);
		gelfMessage.setHost(configuration.getOriginHost());
		gelfMessage.setShortMessage(message);
		if (throwable != null || message.length() > 1000) {
			gelfMessage.setFullMessage(format(message, throwable, configuration.isExtractStacktrace()));
		}
		gelfMessage.setLevel(level);
		if (additionalFields != null) {
			for (Entry<String, Object> fieldEntry : additionalFields.entrySet()) {
				gelfMessage.addField(fieldEntry.getKey(), fieldEntry.getValue());
			}
		}
		for (Entry<String, String> fieldEntry : configuration.getAdditionalFields().entrySet()) {
			gelfMessage.addField(fieldEntry.getKey(), fieldEntry.getValue());
		}
		return gelfMessage;
	}

	private String format(String message, Throwable throwable, boolean extractStacktrace) {
		StringBuilder sb = new StringBuilder(message);
		if (throwable != null) {
			sb.append("\r\n");
			StringWriter writer = new StringWriter();
			if (extractStacktrace) {
				throwable.printStackTrace(new PrintWriter(writer));
			} else {
				writer.append(throwable.toString());
			}
			sb.append(writer.toString());
		}
		return sb.toString();
	}
}
