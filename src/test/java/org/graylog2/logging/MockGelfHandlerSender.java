package org.graylog2.logging;

import java.io.IOException;

import org.graylog2.message.GelfMessage;
import org.graylog2.sender.GelfSender;

/**
 * @author lkmikkel
 */
public class MockGelfHandlerSender implements GelfSender {
    private GelfMessage lastMessage;

    public MockGelfHandlerSender() throws IOException {
    }

    public void sendMessage(GelfMessage message) {
        lastMessage = message;
    }
    
    public void close() {
    }

    public GelfMessage getLastMessage() {
        return lastMessage;
    }
}
