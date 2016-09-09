package org.graylog2.sender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

public abstract class AbstractBufferManager {
	protected byte[] gzipMessage(String message) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			Writer writer = new OutputStreamWriter(new GZIPOutputStream(bos), "utf-8");
			writer.write(message);
			writer.close();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
