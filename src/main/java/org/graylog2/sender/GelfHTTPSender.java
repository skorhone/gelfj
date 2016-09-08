package org.graylog2.sender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.DeflaterOutputStream;

import org.graylog2.message.GelfMessage;

public class GelfHTTPSender implements GelfSender {
	private final URL url;
	private boolean shutdown;

	public GelfHTTPSender(GelfSenderConfiguration configuration) throws MalformedURLException {
		url = new URL(configuration.getGraylogURI());
	}

	public GelfSenderResult sendMessage(GelfMessage message) {
		if (message.isValid()) {
			return GelfSenderResult.MESSAGE_NOT_VALID;
		}
		if (shutdown) {
			return GelfSenderResult.MESSAGE_NOT_VALID_OR_SHUTTING_DOWN;
		}
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(false);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Encoding", "deflate");
			connection.connect();
			DeflaterOutputStream outputStream = new DeflaterOutputStream(connection.getOutputStream());
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
		} catch (MalformedURLException exception) {
			return new GelfSenderResult(GelfSenderResult.ERROR_CODE, exception);
		} catch (IOException exception) {
			return new GelfSenderResult(GelfSenderResult.ERROR_CODE, exception);
		}
		return GelfSenderResult.OK;
	}

	public void close() {
		shutdown = true;
	}
}
