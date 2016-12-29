package org.graylog2.log;

import java.lang.reflect.Method;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.field.FieldExtractor;
import org.graylog2.field.FieldExtractors;
import org.graylog2.message.GelfMessageBuilder;
import org.graylog2.message.GelfMessageBuilderConfiguration;

public class GelfLayout extends Layout {
	private static final String LOGGER_NDC = "loggerNdc";
	private static final Method getTimeStamp;
	private GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration;
	private boolean addExtendedInformation;
	private boolean includeLocation;
	private FieldExtractor fieldExtractor;

	static {
		Method method = null;
		try {
			method = LoggingEvent.class.getDeclaredMethod("getTimeStamp");
		} catch (Exception ignoredException) {
		}
		getTimeStamp = method;
	}

	public GelfLayout() {
		this.gelfMessageBuilderConfiguration = new GelfMessageBuilderConfiguration();
		this.includeLocation = true;
		this.fieldExtractor = FieldExtractors.getDefaultInstance();
	}
	
	public void setOriginHost(String originHost) {
		gelfMessageBuilderConfiguration.setOriginHost(originHost);
	}
	
	public void setFacility(String facility) {
		gelfMessageBuilderConfiguration.setFacility(facility);
	}
	
	public void setFieldExtractor(String type) {
		this.fieldExtractor = FieldExtractors.getInstance(type);
	}

	public void setExtractStacktrace(boolean extractStacktrace) {
		gelfMessageBuilderConfiguration.setExtractStacktrace(extractStacktrace);
	}

	public boolean isAddExtendedInformation() {
		return addExtendedInformation;
	}

	public void setAddExtendedInformation(boolean addExtendedInformation) {
		this.addExtendedInformation = addExtendedInformation;
	}

	public boolean isIncludeLocation() {
		return this.includeLocation;
	}

	public void setIncludeLocation(boolean includeLocation) {
		this.includeLocation = includeLocation;
	}

	public void setFields(String encodedFields) {
		for (String encodedField : encodedFields.split(",")) {
			int equals = encodedField.indexOf('=');
			if (equals != -1) {
				String key = encodedField.substring(0, equals).trim();
				String value = encodedField.substring(equals + 1).trim();
				gelfMessageBuilderConfiguration.addAdditionalField(key, value);
			}
		}
	}

	public GelfMessageBuilderConfiguration getGelfMessageBuilderConfiguration() {
		return gelfMessageBuilderConfiguration;
	}

	@Override
	public void activateOptions() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public String format(LoggingEvent event) {
		long timeStamp = getTimeStamp(event);
		Level level = event.getLevel();

		GelfMessageBuilder builder = new GelfMessageBuilder(getGelfMessageBuilderConfiguration());

		if (isIncludeLocation()) {
			LocationInfo locationInformation = event.getLocationInformation();
			if (locationInformation != null) {
				builder.addField(GelfMessageBuilder.CLASS_NAME_FIELD, locationInformation.getClassName());
				builder.addField(GelfMessageBuilder.METHOD_NAME_FIELD, locationInformation.getMethodName());
			}
		}
		builder.setLevel(String.valueOf(level.getSyslogEquivalent()));
		builder.setTimestamp(timeStamp);
		builder.setMessage(formatMessage(event));
		if (event.getThrowableInformation() != null) {
			builder.setThrowable(event.getThrowableInformation().getThrowable());
		}
		builder.addField(GelfMessageBuilder.THREAD_NAME_FIELD, event.getThreadName());
		builder.addField(GelfMessageBuilder.LOGGER_NAME_FIELD, event.getLoggerName());
		builder.addField(GelfMessageBuilder.NATIVE_LEVEL_FIELD, level.toString());
		if (isAddExtendedInformation()) {
			builder.addFields(MDC.getContext());
			String ndc = event.getNDC();
			if (ndc != null) {
				builder.addField(LOGGER_NDC, event.getNDC());
			}
		}
		if (fieldExtractor != null) {
			builder.addFields(fieldExtractor.getFields(event.getMessage()));
		}
		return builder.build().toJson();
	}

	private String formatMessage(LoggingEvent event) {
		String renderedMessage = event.getRenderedMessage();
		if (renderedMessage == null) {
			renderedMessage = "";
		}
		return renderedMessage;
	}

	@Override
	public boolean ignoresThrowable() {
		return false;
	}

	private long getTimeStamp(LoggingEvent event) {
		long timeStamp = 0;
		if (getTimeStamp != null) {
			try {
				timeStamp = (Long) getTimeStamp.invoke(event);
			} catch (Exception ignoredException) {
			}
		}
		return timeStamp == 0 ? System.currentTimeMillis() : timeStamp;
	}
}
