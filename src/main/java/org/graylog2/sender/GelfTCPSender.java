package org.graylog2.sender;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.graylog2.message.GelfMessage;

public class GelfTCPSender implements GelfSender {
	private boolean shutdown;
	private String host;
	private int port;
	private int sendBufferSize;
	private int sendTimeout;
	private boolean keepalive;
	private TCPBufferManager bufferManager;
	private Selector selector;
	private SocketChannel channel;

	public GelfTCPSender(GelfSenderConfiguration configuration) {
		this.host = configuration.getGraylogHost();
		this.port = configuration.getGraylogPort();
		this.sendBufferSize = configuration.getSocketSendBufferSize();
		this.sendTimeout = configuration.getSendTimeout();
		this.keepalive = configuration.isTcpKeepalive();
		this.bufferManager = new TCPBufferManager();
	}

	public void sendMessage(GelfMessage message) throws GelfSenderException {
		try {
			if (!isConnected()) {
				connect();
			}
			send(bufferManager.toTCPBuffer(message.toJson()));
		} catch (Exception exception) {
			closeConnection();
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, exception);
		}
	}

	private void send(ByteBuffer buffer) throws IOException, InterruptedException {
		while (buffer.hasRemaining() && channel.write(buffer) == 0) {
			if (selector.select(sendTimeout) == 0) {
				throw new IOException("Send operation timed out");
			}
		}
	}

	private synchronized void connect() throws IOException, GelfSenderException {
		if (shutdown) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_SHUTTING_DOWN);
		}
		selector = Selector.open();
		channel = SocketChannel.open();
		channel.configureBlocking(false);
		if (sendBufferSize > 0) {
			channel.setOption(StandardSocketOptions.SO_SNDBUF, sendBufferSize);
		}
		channel.setOption(StandardSocketOptions.SO_KEEPALIVE, keepalive);
		channel.connect(new InetSocketAddress(host, port));
		channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);

		while (!channel.finishConnect()) {
			if (selector.select(sendTimeout) == 0) {
				throw new IOException("Connect operation timed out");
			}
		}
	}

	private boolean isConnected() {
		return channel != null;
	}

	public synchronized void close() {
		if (!shutdown) {
			shutdown = true;
			closeConnection();
		}
	}

	private void closeConnection() {
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignoredException) {
			}
		}
		if (selector != null) {
			try {
				selector.close();
			} catch (Exception ignoredException) {
			}
		}
		channel = null;
		selector = null;
	}

	public static class TCPBufferManager extends AbstractBufferManager {
		public ByteBuffer toTCPBuffer(String message) {
			byte[] messageBytes;
			try {
				// Do not use GZIP, as the headers will contain \0 bytes
				// graylog2-server uses \0 as a delimiter for TCP frames
				// see: https://github.com/Graylog2/graylog2-server/issues/127
				message += '\0';
				messageBytes = message.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("No UTF-8 support available.", e);
			}
			return ByteBuffer.wrap(messageBytes);
		}
	}
}
