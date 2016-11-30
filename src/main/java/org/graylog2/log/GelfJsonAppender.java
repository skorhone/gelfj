package org.graylog2.log;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.message.GelfMessage;
import org.graylog2.message.GelfMessageBuilderException;
import org.graylog2.sender.GelfSender;
import org.graylog2.sender.GelfSenderException;
import org.json.simple.JSONValue;

/**
 * A GelfAppender which will parse the given JSON message into additional fields
 * in GELF
 *
 * @author Anton Yakimov
 * @author Jochen Schalanda
 * @author the-james-burton
 */
public class GelfJsonAppender extends GelfAppender {
	@Override
	protected void append(final LoggingEvent event) {
		GelfSender sender = getGelfSender();
		if (sender == null) {
			errorHandler.error("Could not send GELF message. Gelf Sender is not initialised and equals null");
		} else {
			try {
				GelfMessage gelfMessage = getMessageFactory().makeMessage(layout, event, this);
				@SuppressWarnings("unchecked")
				Map<String, String> fields = (Map<String, String>) JSONValue.parse(event.getMessage().toString());
				if (fields != null) {
					for (Entry<String,String> entry : fields.entrySet()) {
						if ("message".equals(entry.getKey())) {
							gelfMessage.setShortMessage(entry.getValue());
						} else {
							gelfMessage.addField(entry.getKey(), entry.getValue());
						}
					}
				}
				sender.sendMessage(gelfMessage);
			} catch (GelfMessageBuilderException exception) {
				errorHandler.error("Error building GELF message", exception, ErrorCode.WRITE_FAILURE);
			} catch (GelfSenderException exception) {
				errorHandler.error("Error during sending GELF message. Error code: " + exception.getErrorCode() + ".",
						exception.getCause(), ErrorCode.WRITE_FAILURE);
			}
		}
	}

}
