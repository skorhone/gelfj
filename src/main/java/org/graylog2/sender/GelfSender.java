package org.graylog2.sender;

public interface GelfSender {
	public void sendMessage(String message) throws GelfSenderException;

	public void close();
}
