package org.graylog2.logging;

import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.graylog2.message.GelfMessageBuilderConfiguration;
import org.graylog2.message.GelfMessageBuilderException;
import org.graylog2.sender.GelfSender;
import org.graylog2.sender.GelfSenderConfiguration;
import org.graylog2.sender.GelfSenderConfigurationException;
import org.graylog2.sender.GelfSenderException;
import org.graylog2.sender.GelfSenderFactory;

public class GelfHandler extends Handler {
	private GelfSenderConfiguration senderConfiguration;
	private GelfSender gelfSender;
	private boolean closed;

	public GelfHandler() {
		configure(new JULProperties(LogManager.getLogManager(), System.getProperties(), getClass().getName()));
	}

	public GelfHandler(JULProperties properties) {
		configure(properties);
	}

	private void configure(JULProperties properties) {
		GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration = JULConfigurationManager
				.getGelfMessageBuilderConfiguration(properties);
		this.senderConfiguration = JULConfigurationManager.getGelfSenderConfiguration(properties);

		final String level = properties.getProperty("level");
		if (null != level) {
			setLevel(Level.parse(level.trim()));
		} else {
			setLevel(Level.INFO);
		}

		setFormatter(new GelfFormatter(JULConfigurationManager.getGelfFormatterConfiguration(properties), gelfMessageBuilderConfiguration));

		final String filter = properties.getProperty("filter");
		try {
			if (null != filter) {
				setFilter((Filter) getClass().getClassLoader().loadClass(filter).newInstance());
			}
		} catch (Exception ignoredException) {
		}
	}

	@Override
	public synchronized void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		if (isLoggable(record)) {
			send(record);
		}
	}

	private void send(LogRecord record) {
		try {
			GelfSender gelfSender = getGelfSender();
			if (gelfSender != null) {
				gelfSender.sendMessage(getFormatter().format(record));
			}
		} catch (GelfSenderException exception) {
			reportError("Error during sending GELF message. Error code: " + exception.getErrorCode() + ".",
					exception.getCause(), ErrorManager.WRITE_FAILURE);
		} catch (GelfSenderConfigurationException exception) {
			reportError(exception.getMessage(), exception.getCauseException(), ErrorManager.WRITE_FAILURE);
		} catch (GelfMessageBuilderException exception) {
			reportError("Could not create GELF message", exception, ErrorManager.WRITE_FAILURE);
		} catch (Exception exception) {
			reportError("Could not send GELF message", exception, ErrorManager.WRITE_FAILURE);
		}
	}

	public synchronized GelfSender getGelfSender() {
		if (null == gelfSender && !closed) {
			gelfSender = GelfSenderFactory.getInstance().createSender(senderConfiguration);
		}
		return gelfSender;
	}

	@Override
	public synchronized void close() {
		if (gelfSender != null) {
			gelfSender.close();
			gelfSender = null;
		}
		closed = true;
	}

	public synchronized void setGelfSender(GelfSender gelfSender) {
		this.gelfSender = gelfSender;
	}
}
