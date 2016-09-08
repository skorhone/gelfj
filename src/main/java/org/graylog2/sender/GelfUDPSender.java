package org.graylog2.sender;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Random;

import org.graylog2.message.GelfMessage;

public class GelfUDPSender implements GelfSender {
	private static final byte[] GELF_CHUNKED_ID = new byte[] { 0x1e, 0x0f };
	private static final int MAXIMUM_CHUNK_SIZE = 1420;

	private String host;
	private int port;
	private int sendBufferSize;
	private int maxRetries;
	private UDPBufferBuilder bufferBuilder;
	private DatagramChannel channel;

	public GelfUDPSender(GelfSenderConfiguration configuration, boolean enableRetry) {
		this.host = configuration.getGraylogHost();
		this.port = configuration.getGraylogPort();
		this.sendBufferSize = configuration.getSocketSendBufferSize();
		this.maxRetries = enableRetry ? configuration.getMaxRetries() : 0;
		this.bufferBuilder = new UDPBufferBuilder();
	}

	private DatagramChannel initiateChannel() throws IOException {
		DatagramChannel resultingChannel = DatagramChannel.open();
		DatagramSocket socket = resultingChannel.socket();
		socket.bind(new InetSocketAddress(0));
		if (sendBufferSize > 0) {
			socket.setSendBufferSize(sendBufferSize);
		}
		socket.connect(new InetSocketAddress(this.host, this.port));
		return resultingChannel;
	}

	public void sendMessage(GelfMessage message) throws GelfSenderException {
		if (!message.isValid()) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_MESSAGE_NOT_VALID);
		}
		sendDatagrams(bufferBuilder.toUDPBuffers(message.toJson()));
	}

	private void sendDatagrams(ByteBuffer[] bytesList) throws GelfSenderException {
		int tries = 0;
		Exception lastException = null;
		do {
			try {
				if (getChannel() == null || !getChannel().isOpen()) {
					setChannel(initiateChannel());
				}
				for (ByteBuffer buffer : bytesList) {
					getChannel().write(buffer);
				}
				return;
			} catch (Exception exception) {
				tries++;
				lastException = exception;
			}
		} while (tries <= maxRetries);

		throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, lastException);
	}

	public void close() {
		try {
			getChannel().close();
		} catch (IOException ignoredException) {
		}
	}

	public DatagramChannel getChannel() {
		return channel;
	}

	public void setChannel(DatagramChannel channel) {
		this.channel = channel;
	}

	public static class UDPBufferBuilder extends BufferBuilder {
		public ByteBuffer[] toUDPBuffers(String message) {
			byte[] messageBytes = gzipMessage(message);
			// calculate the length of the datagrams array
			int diagrams_length = messageBytes.length / MAXIMUM_CHUNK_SIZE;
			// In case of a remainder, due to the integer division, add a extra
			// datagram
			if (messageBytes.length % MAXIMUM_CHUNK_SIZE != 0) {
				diagrams_length++;
			}
			ByteBuffer[] datagrams = new ByteBuffer[diagrams_length];
			if (messageBytes.length > MAXIMUM_CHUNK_SIZE) {
				sliceDatagrams(messageBytes, datagrams);
			} else {
				datagrams[0] = ByteBuffer.allocate(messageBytes.length);
				datagrams[0].put(messageBytes);
				datagrams[0].flip();
			}
			return datagrams;
		}

		private void sliceDatagrams(byte[] messageBytes, ByteBuffer[] datagrams) {
			int messageLength = messageBytes.length;
			byte[] messageId = new byte[8];
			new Random().nextBytes(messageId);

			// Reuse length of datagrams array since this is supposed to be the
			// correct number of datagrams
			int num = datagrams.length;
			for (int idx = 0; idx < num; idx++) {
				byte[] header = concatByteArray(GELF_CHUNKED_ID,
						concatByteArray(messageId, new byte[] { (byte) idx, (byte) num }));
				int from = idx * MAXIMUM_CHUNK_SIZE;
				int to = from + MAXIMUM_CHUNK_SIZE;
				if (to >= messageLength) {
					to = messageLength;
				}

				byte[] range = new byte[to - from];
				System.arraycopy(messageBytes, from, range, 0, range.length);

				byte[] datagram = concatByteArray(header, range);
				datagrams[idx] = ByteBuffer.allocate(datagram.length);
				datagrams[idx].put(datagram);
				datagrams[idx].flip();
			}
		}

		private byte[] concatByteArray(byte[] first, byte[] second) {
			byte[] result = new byte[first.length + second.length];
			System.arraycopy(first, 0, result, 0, first.length);
			System.arraycopy(second, 0, result, first.length, second.length);
			return result;
		}
	}
}
