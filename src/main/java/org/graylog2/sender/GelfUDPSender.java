package org.graylog2.sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;

import org.graylog2.message.GelfMessage;

public class GelfUDPSender implements GelfSender {
	private String destinationHost;
	private int destinationPort;
	private int sendBufferSize;
	private int maxRetries;
	private UDPBufferBuilder bufferBuilder;
	private SocketAddress destinationAddress;
	private DatagramSocket socket;

	public GelfUDPSender(GelfSenderConfiguration configuration, boolean enableRetry) {
		this.destinationHost = configuration.getGraylogHost();
		this.destinationPort = configuration.getGraylogPort();
		this.sendBufferSize = configuration.getSocketSendBufferSize();
		this.maxRetries = enableRetry ? configuration.getMaxRetries() : 0;
		this.bufferBuilder = new UDPBufferBuilder();
	}

	private void initializeSocket() throws IOException {
		socket = new DatagramSocket();
		socket.setBroadcast(false);
		if (sendBufferSize > 0) {
			socket.setSendBufferSize(sendBufferSize);
		}
		destinationAddress = new InetSocketAddress(this.destinationHost, this.destinationPort);
		socket.connect(destinationAddress);
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
				if (socket == null) {
					initializeSocket();
				}
				for (ByteBuffer buffer : bytesList) {
					socket.send(new DatagramPacket(buffer.array(), buffer.limit(), destinationAddress));
				}
				return;
			} catch (Exception exception) {
				tries++;
				lastException = exception;
				close();
			}
		} while (tries <= maxRetries);

		throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, lastException);
	}

	public void close() {
		if (socket != null) {
			socket.close();
			socket = null;
		}
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	public void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	public static class UDPBufferBuilder extends BufferBuilder {
		private static final byte[] GELF_CHUNKED_ID = new byte[] { 0x1e, 0x0f };
		private static final int MAXIMUM_CHUNK_SIZE = 1420;

		private Random random;
		private byte[] hostId;

		public UDPBufferBuilder() {
			this.random = new Random();
			this.hostId = createHostId();
		}

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
			byte[] messageId = createMessageId();

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

		private byte[] createMessageId() {
			byte[] messageId = new byte[8];
			byte[] randomBytes = createHostId();
			random.nextBytes(randomBytes);
			System.arraycopy(hostId, 0, messageId, 0, 4);
			System.arraycopy(randomBytes, 0, messageId, 4, 4);
			return messageId;
		}

		private byte[] createHostId() {
			byte[] hostId = new byte[4];
			byte[] address = getAddress();
			hostId[0] = (byte) new SecureRandom().nextInt();
			hostId[1] = address[0];
			hostId[2] = address[1];
			hostId[3] = address[2];
			return hostId;
		}

		private byte[] getAddress() {
			byte[] address = null;
			try {
				InetAddress inetAddress = InetAddress.getLocalHost();
				if (inetAddress != null && !inetAddress.isLoopbackAddress()) {
					address = inetAddress.getAddress();
				}
			} catch (Exception exception) {
			}
			if (address == null) {
				address = new byte[4];
				new SecureRandom().nextBytes(address);
			}
			return address;
		}

		private byte[] concatByteArray(byte[] first, byte[] second) {
			byte[] result = new byte[first.length + second.length];
			System.arraycopy(first, 0, result, 0, first.length);
			System.arraycopy(second, 0, result, first.length, second.length);
			return result;
		}
	}
}
