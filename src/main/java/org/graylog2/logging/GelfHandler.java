package org.graylog2.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.graylog2.message.GelfMessage;
import org.graylog2.message.GelfMessageBuilder;
import org.graylog2.message.GelfMessageBuilderConfiguration;
import org.graylog2.message.GelfMessageBuilderException;
import org.graylog2.sender.GelfSender;
import org.graylog2.sender.GelfSenderConfiguration;
import org.graylog2.sender.GelfSenderConfigurationException;
import org.graylog2.sender.GelfSenderException;
import org.graylog2.sender.GelfSenderFactory;

public class GelfHandler extends Handler {
	private GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration;
	private GelfSenderConfiguration senderConfiguration;
	private Map<String, String> fields;
	private GelfSender gelfSender;
	private boolean closed;

	public GelfHandler() {
		configure(new JULProperties(LogManager.getLogManager(), System.getProperties(), getClass().getName()));
	}

	public GelfHandler(JULProperties properties) {
		configure(properties);
	}

	private void configure(JULProperties properties) {
		this.gelfMessageBuilderConfiguration = JULConfigurationManager.getGelfMessageBuilderConfiguration(properties);
		this.senderConfiguration = JULConfigurationManager.getGelfSenderConfiguration(properties);

		int fieldNumber = 0;
		fields = new HashMap<String, String>();
		while (true) {
			final String property = properties.getProperty("additionalField." + fieldNumber);
			if (null == property) {
				break;
			}
			final int index = property.indexOf('=');
			if (-1 != index) {
				fields.put(property.substring(0, index), property.substring(index + 1));
			}
			fieldNumber++;
		}

		final String level = properties.getProperty("level");
		if (null != level) {
			setLevel(Level.parse(level.trim()));
		} else {
			setLevel(Level.INFO);
		}

		boolean extractStacktrace = "true".equalsIgnoreCase(properties.getProperty("extractStacktrace"));
		setFormatter(new SimpleFormatter(extractStacktrace));

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

	private synchronized void send(LogRecord record) {
		if (!closed) {
			try {
				if (null == gelfSender) {
					gelfSender = GelfSenderFactory.getInstance().createSender(senderConfiguration);
				}
				gelfSender.sendMessage(makeMessage(record));
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
	}

	@Override
	public synchronized void close() {
		if (gelfSender != null) {
			gelfSender.close();
			gelfSender = null;
		}
		closed = true;
	}

	private GelfMessage makeMessage(LogRecord record) throws GelfMessageBuilderException {
		String message = getFormatter().format(record);

		GelfMessageBuilder builder = new GelfMessageBuilder(gelfMessageBuilderConfiguration);

		builder.setFullMessage(message);
		builder.setLevel(String.valueOf(levelToSyslogLevel(record.getLevel())));
		builder.addField(GelfMessageBuilder.THREAD_NAME_FIELD, Thread.currentThread().getName());
		builder.addField(GelfMessageBuilder.LOGGER_LEVEL_FIELD, record.getLevel());
		builder.addField(GelfMessageBuilder.LOGGER_NAME_FIELD, record.getLoggerName());
		builder.addField(GelfMessageBuilder.SOURCE_CLASS_FIELD, record.getSourceClassName());
		builder.addField(GelfMessageBuilder.SOURCE_METHOD_FIELD, record.getSourceMethodName());

		if (record instanceof GelfLogRecord) {
			GelfLogRecord gelfLogRecord = (GelfLogRecord) record;
			builder.addFields(gelfLogRecord.getFields());
		}
		// builder.addFields(fields);

		return builder.build();
	}

	private int levelToSyslogLevel(Level level) {
		final int syslogLevel;
		if (level.intValue() == Level.SEVERE.intValue()) {
			syslogLevel = 3;
		} else if (level.intValue() == Level.WARNING.intValue()) {
			syslogLevel = 4;
		} else if (level.intValue() == Level.INFO.intValue()) {
			syslogLevel = 6;
		} else {
			syslogLevel = 7;
		}
		return syslogLevel;
	}

	public synchronized void setGelfSender(GelfSender gelfSender) {
		this.gelfSender = gelfSender;
	}

	public void setAdditionalField(String entry) {
		if (entry == null)
			return;
		final int index = entry.indexOf('=');
		if (-1 != index) {
			String key = entry.substring(0, index);
			String val = entry.substring(index + 1);
			if (key.equals(""))
				return;
			fields.put(key, val);
		}
	}

	public Map<String, String> getFields() {
		return fields;
	}
}
