package org.graylog2.sender;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.graylog2.message.GelfMessage;

public class GelfTCPSender implements GelfSender {
	private boolean shutdown = false;
	private String host;
	private int port;
	private int sendBufferSize;
	private boolean keepalive;
	private TCPBufferBuilder bufferBuilder;
	private Socket socket;
	private OutputStream os;

	public GelfTCPSender(GelfSenderConfiguration configuration) {
		this.host = configuration.getGraylogHost();
		this.port = configuration.getGraylogPort();
		this.sendBufferSize = configuration.getSocketSendBufferSize();
		this.keepalive = configuration.isTcpKeepalive();
		this.bufferBuilder = new TCPBufferBuilder();
	}

	public void sendMessage(GelfMessage message) throws GelfSenderException {
		if (shutdown) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_SHUTTING_DOWN);
		}
		if (!message.isValid()) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_MESSAGE_NOT_VALID);
		}
		try {
			if (!isConnected()) {
				connect();
			}
			os.write(bufferBuilder.toTCPBuffer(message.toJson()).array());
		} catch (Exception exception) {
			closeConnection();
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, exception);
		}
	}

	private void connect() throws UnknownHostException, IOException, SocketException {
		socket = new Socket(host, port);
		if (sendBufferSize > 0) {
			socket.setSendBufferSize(sendBufferSize);
		}
		socket.setKeepAlive(keepalive);
		os = socket.getOutputStream();
	}

	private boolean isConnected() {
		return socket != null && os != null;
	}

	private void closeConnection() {
		try {
			os.close();
		} catch (Exception ignoredException) {
		} finally {
			os = null;
		}
		try {
			socket.close();
		} catch (Exception ignoredException) {
		} finally {
			socket = null;
		}
	}

	public void close() {
		shutdown = true;
		closeConnection();
	}

	public static class TCPBufferBuilder extends BufferBuilder {
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
			ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length);
			buffer.put(messageBytes);
			buffer.flip();
			return buffer;
		}
	}
}
