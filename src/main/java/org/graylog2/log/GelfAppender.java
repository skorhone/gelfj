package org.graylog2.log;

import java.util.logging.ErrorManager;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
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
public class GelfAppender extends AppenderSkeleton {
	private GelfSenderConfiguration senderConfiguration;
	private GelfSender gelfSender;

	public GelfAppender() {
		this.senderConfiguration = new GelfSenderConfiguration();
		this.layout = new GelfLayout();
	}

	@Override
	public void setLayout(Layout layout) {
		if (!(layout instanceof GelfLayout)) {
			throw new IllegalArgumentException("Only GelfLayout is supported!");
		}
		this.layout = layout;
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

	public int getReenableTimeout() {
		return senderConfiguration.getReenableTimeout();
	}

	public void setReenableTimeout(int reenableTimeout) {
		senderConfiguration.setReenableTimeout(reenableTimeout);
	}

	public int getErrorCountThreshold() {
		return senderConfiguration.getErrorCountThreshold();
	}

	public void setErrorCountThreshold(int errorCountThreshold) {
		senderConfiguration.setErrorCountThreshold(errorCountThreshold);
	}

	public int getMaxRetries() {
		return senderConfiguration.getMaxRetries();
	}

	public void setMaxRetries(int maxRetries) {
		senderConfiguration.setMaxRetries(maxRetries);
	}

	public GelfLayout getLayout() {
		return (GelfLayout) super.getLayout();
	}

	public void setFieldExtractor(String type) {
		getLayout().setFieldExtractor(type);
	}

	public void setExtractStacktrace(boolean extractStacktrace) {
		getLayout().setExtractStacktrace(extractStacktrace);
	}

	public void setOriginHost(String originHost) {
		getLayout().setOriginHost(originHost);
	}

	public void setFacility(String facility) {
		getLayout().setFacility(facility);
	}

	public void setAddExtendedInformation(boolean addExtendedInformation) {
		getLayout().setAddExtendedInformation(addExtendedInformation);
	}

	public void setIncludeLocation(boolean includeLocation) {
		getLayout().setIncludeLocation(includeLocation);
	}

	public void setFields(String encodedFields) {
		getLayout().setFields(encodedFields);
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
				sender.sendMessage(getLayout().format(event));
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
}
