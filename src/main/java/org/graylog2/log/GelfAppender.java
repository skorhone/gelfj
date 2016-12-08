package org.graylog2.log;

import java.lang.reflect.Method;
import java.util.logging.ErrorManager;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.message.GelfMessage;
import org.graylog2.message.GelfMessageBuilder;
import org.graylog2.message.GelfMessageBuilderConfiguration;
import org.graylog2.message.GelfMessageBuilderException;
import org.graylog2.sender.GelfSender;
import org.graylog2.sender.GelfSenderConfiguration;
import org.graylog2.sender.GelfSenderConfigurationException;
import org.graylog2.sender.GelfSenderException;
import org.graylog2.sender.GelfSenderFactory;
import org.graylog2.util.Fields;

/**
 *
 * @author Anton Yakimov
 * @author Jochen Schalanda
 */
public class GelfAppender extends AppenderSkeleton {
	private static final Method methodGetTimeStamp;
	private static final String LOGGER_NDC = "loggerNdc";
	private GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration;
	private GelfSenderConfiguration senderConfiguration;
	private GelfSender gelfSender;
	private boolean extractStacktrace;
	private boolean addExtendedInformation;
	private boolean includeLocation;

	static {
		Method method = null;
		try {
			method = LoggingEvent.class.getDeclaredMethod("getTimeStamp");
		} catch (Exception ignoredException) {
		}
		methodGetTimeStamp = method;
	}

	public GelfAppender() {
		this.gelfMessageBuilderConfiguration = new GelfMessageBuilderConfiguration();
		this.senderConfiguration = new GelfSenderConfiguration();
		this.includeLocation = true;
	}

	public String getTargetURI() {
		return senderConfiguration.getTargetURI();
	}

	public void setTargetURI(String graylogHost) {
		senderConfiguration.setTargetURI(graylogHost);
	}

	public boolean isThreaded() {
		return senderConfiguration.isThreaded();
	}

	public void setThreaded(boolean threaded) {
		senderConfiguration.setThreaded(threaded);
	}

	public int getThreadedQueueMaxDepth() {
		return senderConfiguration.getThreadedQueueMaxDepth();
	}

	public void setThreadedQueueMaxDepth(int threadedQueueMaxDepth) {
		senderConfiguration.setThreadedQueueMaxDepth(threadedQueueMaxDepth);
	}

	public int getThreadedQueueTimeout() {
		return senderConfiguration.getThreadedQueueTimeout();
	}

	public void setThreadedQueueTimeout(int threadedQueueTimeout) {
		senderConfiguration.setThreadedQueueTimeout(threadedQueueTimeout);
	}

	public int getSendTimeout() {
		return senderConfiguration.getSendTimeout();
	}

	public int getMaxRetries() {
		return senderConfiguration.getMaxRetries();
	}

	public void setMaxRetries(int maxRetries) {
		senderConfiguration.setMaxRetries(maxRetries);
	}

	public boolean isExtractStacktrace() {
		return extractStacktrace;
	}

	public void setExtractStacktrace(boolean extractStacktrace) {
		this.extractStacktrace = extractStacktrace;
	}

	public String getOriginHost() {
		return gelfMessageBuilderConfiguration.getOriginHost();
	}

	public void setOriginHost(String originHost) {
		gelfMessageBuilderConfiguration.setOriginHost(originHost);
	}

	public GelfMessageBuilderConfiguration getGelfMessageBuilderConfiguration() {
		return gelfMessageBuilderConfiguration;
	}

	public void setFacility(String facility) {
		gelfMessageBuilderConfiguration.setFacility(facility);
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

	@Override
	public void activateOptions() {
		try {
			if (gelfSender != null) {
				gelfSender.close();
			}
			gelfSender = GelfSenderFactory.getInstance().createSender(senderConfiguration);
		} catch (GelfSenderConfigurationException exception) {
			errorHandler.error(exception.getMessage(), exception.getCauseException(), ErrorManager.WRITE_FAILURE);
		} catch (Exception exception) {
			errorHandler.error("Could not activate GELF appender", exception, ErrorManager.WRITE_FAILURE);
		}
	}

	@Override
	protected void append(LoggingEvent event) {
		GelfSender sender = getGelfSender();
		if (sender == null) {
			errorHandler.error("Could not send GELF message. Gelf Sender is not initialised and equals null");
		} else {
			try {
				GelfMessage gelfMessage = createMessage(event);
				sender.sendMessage(gelfMessage);
			} catch (GelfMessageBuilderException exception) {
				errorHandler.error("Error building GELF message", exception, ErrorCode.WRITE_FAILURE);
			} catch (GelfSenderException exception) {
				errorHandler.error("Error during sending GELF message. Error code: " + exception.getErrorCode() + ".",
						exception.getCause(), ErrorCode.WRITE_FAILURE);
			}
		}
	}

	public GelfSender getGelfSender() {
		return gelfSender;
	}

	public void close() {
		if (gelfSender != null) {
			gelfSender.close();
		}
	}

	public boolean requiresLayout() {
		return true;
	}

	@SuppressWarnings("unchecked")
	private GelfMessage createMessage(LoggingEvent event) throws GelfMessageBuilderException {
		long timeStamp = getTimeStamp(event);
		Level level = event.getLevel();

		GelfMessageBuilder builder = new GelfMessageBuilder(getGelfMessageBuilderConfiguration());

		if (isIncludeLocation()) {
			LocationInfo locationInformation = event.getLocationInformation();
			builder.addField(GelfMessageBuilder.CLASS_NAME_FIELD, locationInformation.getClassName());
			builder.addField(GelfMessageBuilder.METHOD_NAME_FIELD, locationInformation.getMethodName());
		}

		builder.setLevel(String.valueOf(level.getSyslogEquivalent()));
		builder.setTimestamp(timeStamp);
		builder.setMessage(formatMessage(event));
		if (event.getThrowableInformation() != null) {
			builder.setThrowable(event.getThrowableInformation().getThrowable());
		}

		if (isAddExtendedInformation()) {
			builder.addField(GelfMessageBuilder.THREAD_NAME_FIELD, event.getThreadName());
			builder.addField(GelfMessageBuilder.NATIVE_LEVEL_FIELD, level.toString());
			builder.addField(GelfMessageBuilder.LOGGER_NAME_FIELD, event.getLoggerName());
			builder.addFields(MDC.getContext());
			String ndc = event.getNDC();
			if (ndc != null) {
				builder.addField(LOGGER_NDC, event.getNDC());
			}
		}
		builder.addFields(Fields.getFields(event.getMessage()));

		return builder.build();
	}

	private String formatMessage(LoggingEvent event) {
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
