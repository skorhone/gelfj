package org.graylog2.sender;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.graylog2.message.GelfMessage;

public class GelfTCPSender implements GelfSender {
	private boolean shutdown = false;
	private String host;
	private int port;
	private int sendBufferSize;
	private boolean keepalive;
	private TCPBufferManager bufferManager;
	private Socket socket;
	private OutputStream os;

	public GelfTCPSender(GelfSenderConfiguration configuration) {
		this.host = configuration.getGraylogHost();
		this.port = configuration.getGraylogPort();
		this.sendBufferSize = configuration.getSocketSendBufferSize();
		this.keepalive = configuration.isTcpKeepalive();
		this.bufferManager = new TCPBufferManager();
	}

	public void sendMessage(GelfMessage message) throws GelfSenderException {
		try {
			if (!isConnected()) {
				connect();
			}
			os.write(bufferManager.toTCPBuffer(message.toJson()).array());
		} catch (Exception exception) {
			closeConnection();
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, exception);
		}
	}

	private synchronized void connect() throws IOException, GelfSenderException {
		if (shutdown) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_SHUTTING_DOWN);
		}
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

	public synchronized void close() {
		if (!shutdown) {
			shutdown = true;
			closeConnection();
		}
	}

	private void closeConnection() {
		if (os != null) {
			try {
				os.close();
			} catch (Exception ignoredException) {
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception ignoredException) {
			}
		}
		os = null;
		socket = null;
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
