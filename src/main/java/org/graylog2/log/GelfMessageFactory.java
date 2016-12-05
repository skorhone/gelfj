package org.graylog2.log;

import java.lang.reflect.Method;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.message.GelfMessage;
import org.graylog2.message.GelfMessageBuilder;
import org.graylog2.message.GelfMessageBuilderException;
import org.graylog2.util.Fields;

public class GelfMessageFactory {
	private static Method methodGetTimeStamp = null;
	private static final String LOGGER_NDC = "loggerNdc";

	static {
		Method[] declaredMethods = LoggingEvent.class.getDeclaredMethods();
		for (Method m : declaredMethods) {
			if (m.getName().equals("getTimeStamp")) {
				methodGetTimeStamp = m;
				break;
			}
		}
	}

	public GelfMessage makeMessage(Layout layout, LoggingEvent event, GelfMessageProvider provider)
			throws GelfMessageBuilderException {
		long timeStamp = getTimeStamp(event);
		Level level = event.getLevel();

		GelfMessageBuilder builder = new GelfMessageBuilder(provider.getGelfMessageBuilderConfiguration());

		if (provider.isIncludeLocation()) {
			LocationInfo locationInformation = event.getLocationInformation();
			builder.addField(GelfMessageBuilder.CLASS_NAME_FIELD, locationInformation.getClassName());
			builder.addField(GelfMessageBuilder.METHOD_NAME_FIELD, locationInformation.getMethodName());
		}

		builder.setLevel(String.valueOf(level.getSyslogEquivalent()));
		builder.setTimestamp(timeStamp);
		builder.setMessage(formatMessage(layout, event));
		if (event.getThrowableInformation() != null) {
			builder.setThrowable(event.getThrowableInformation().getThrowable());
		}
		
		if (provider.isAddExtendedInformation()) {
			builder.addField(GelfMessageBuilder.THREAD_NAME_FIELD, event.getThreadName());
			builder.addField(GelfMessageBuilder.NATIVE_LEVEL_FIELD, level.toString());
			builder.addField(GelfMessageBuilder.LOGGER_NAME_FIELD, event.getLoggerName());
			String ndc = event.getNDC();
			if (ndc != null) {
				builder.addField(LOGGER_NDC, event.getNDC());
			}
		}
		builder.addFields(Fields.getFields(event.getMessage()));
		
		return builder.build();
	}

	private String formatMessage(Layout layout, LoggingEvent event) {
		String renderedMessage = layout != null ? layout.format(event) : event.getRenderedMessage();
		if (renderedMessage == null) {
			renderedMessage = "";
		}
		return renderedMessage;
	}

	private long getTimeStamp(LoggingEvent event) {
		long timeStamp = 0;
		if (methodGetTimeStamp != null) {
			try {
				timeStamp = (Long) methodGetTimeStamp.invoke(event);
			} catch (Exception ignoredException) {
			}
		}
		return timeStamp == 0 ? System.currentTimeMillis() : timeStamp;
	}
}
