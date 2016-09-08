package org.graylog2.sender;

import org.graylog2.message.GelfMessage;

public interface GelfSender {
	public void sendMessage(GelfMessage message) throws GelfSenderException;

	public void close();
}
