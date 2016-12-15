package org.graylog2.logging;

import java.io.IOException;

import org.graylog2.sender.GelfSender;

/**
 * @author lkmikkel
 */
public class MockGelfHandlerSender implements GelfSender {
	private String lastMessage;

	public MockGelfHandlerSender() throws IOException {
	}

	@Override
	public void sendMessage(String message) {
		lastMessage = message;
	}

	@Override
	public void close() {
	}

	public String getLastMessage() {
		return lastMessage;
	}
}
