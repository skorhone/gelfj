package org.graylog2.log;

import java.util.logging.ErrorManager;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.message.GelfMessage;
import org.graylog2.message.GelfMessageBuilderConfiguration;
import org.graylog2.message.GelfMessageBuilderException;
import org.graylog2.sender.GelfSender;
import org.graylog2.sender.GelfSenderConfiguration;
import org.graylog2.sender.GelfSenderConfigurationException;
import org.graylog2.sender.GelfSenderException;
import org.graylog2.sender.GelfSenderFactory;

/**
 *
 * @author Anton Yakimov
 * @author Jochen Schalanda
 */
public class GelfAppender extends AppenderSkeleton implements GelfMessageProvider {
	private GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration;
	private GelfSenderConfiguration senderConfiguration;
	private GelfSender gelfSender;
	private GelfMessageFactory messageFactory;
	private boolean extractStacktrace;
	private boolean addExtendedInformation;
	private boolean includeLocation;

	public GelfAppender() {
		this.gelfMessageBuilderConfiguration = new GelfMessageBuilderConfiguration();
		this.senderConfiguration = new GelfSenderConfiguration();
		this.messageFactory = new GelfMessageFactory();
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
				GelfMessage gelfMessage = messageFactory.makeMessage(layout, event, this);
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

	public GelfMessageFactory getMessageFactory() {
		return messageFactory;
	}

	public void close() {
		GelfSender x = this.getGelfSender();
		if (x != null) {
			x.close();
		}
	}

	public boolean requiresLayout() {
		return true;
	}
}
