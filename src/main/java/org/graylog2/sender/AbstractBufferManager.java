package org.graylog2.sender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.DeflaterOutputStream;

public abstract class AbstractBufferManager {
	protected byte[] getMessageAsBytes(String message) {
		try {
			if (message.length() < 1000) {
				return message.getBytes("utf-8");
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Writer writer = new OutputStreamWriter(new DeflaterOutputStream(bos), "utf-8");
			writer.write(message);
			writer.close();
			
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
