package org.graylog2.message;

public class TestGelfMessage extends GelfMessage {
	public TestGelfMessage() {
		setHost("localhost");
		setShortMessage("short");
		setFullMessage("full");
	}
}
