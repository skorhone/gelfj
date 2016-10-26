package org.graylog2.sender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.graylog2.message.GelfMessage;

public class GelfHTTPSender implements GelfSender {
	private final URL url;
	private boolean shutdown;
	private int timeout;

	public GelfHTTPSender(GelfSenderConfiguration configuration) throws MalformedURLException {
		url = new URL(configuration.getTargetURI());
		timeout = configuration.getSendTimeout();
	}

	public void sendMessage(GelfMessage message) throws GelfSenderException {
		if (shutdown) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_SHUTTING_DOWN);
		}
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Encoding", "gzip");
			connection.connect();
			DeflaterOutputStream outputStream = new GZIPOutputStream(connection.getOutputStream());
			try {
				outputStream.write(message.toJson().getBytes("utf-8"));
			} finally {
				outputStream.close();
			}
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_ACCEPTED) {
				throw new IOException(
						"Invalid response code: " + responseCode + " expected " + HttpURLConnection.HTTP_ACCEPTED);
			}
		} catch (Exception exception) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, exception);
		}
	}

	public void close() {
		shutdown = true;
	}
}
