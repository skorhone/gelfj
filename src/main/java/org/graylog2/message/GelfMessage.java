package org.graylog2.message;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.graylog2.json.JSON;

public class GelfMessage {
	private static final String ID_NAME = "id";
	private static final String GELF_VERSION = "1.1";
	private static final BigDecimal TIME_DIVISOR = new BigDecimal(1000);
	private String version;
	private String host;
	private String shortMessage;
	private String fullMessage;
	private long javaTimestamp;
	private String level;
	private String line;
	private String file;
	private Map<String, Object> additionalFields;

	public GelfMessage() {
		this.version = GELF_VERSION;
	}

	public String toJson() {
		Map<String, Object> message = new LinkedHashMap<String, Object>(100);
		message.put("version", getVersion());
		message.put("host", getHost());
		message.put("short_message", getShortMessage());
		message.put("full_message", getFullMessage());
		message.put("timestamp", getTimestamp());
		if (getFile() != null) {
			message.put("file", getFile());
		}
		if (getLine() != null) {
			message.put("line", getLine());
		}
		message.put("level", getLevel());
		for (Entry<String, Object> entry : getAdditionalFields().entrySet()) {
			message.put("_" + entry.getKey(), entry.getValue());
		}
		return JSON.encode(message, new StringBuilder()).toString();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getShortMessage() {
		return shortMessage;
	}

	public void setShortMessage(String shortMessage) {
		this.shortMessage = shortMessage;
	}

	public String getFullMessage() {
		return fullMessage;
	}

	public void setFullMessage(String fullMessage) {
		this.fullMessage = fullMessage;
	}

	public String getTimestamp() {
		return new BigDecimal(javaTimestamp).divide(TIME_DIVISOR).toPlainString();
	}

	public Long getJavaTimestamp() {
		return javaTimestamp;
	}

	public void setJavaTimestamp(long javaTimestamp) {
		this.javaTimestamp = javaTimestamp;
	}

	public long getLevel() {
		long level;
		try {
			level = Long.parseLong(this.level);
		} catch (NumberFormatException e) {
			// fallback to info
			level = 6L;
		}
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Long getLine() {
		if (this.line == null) {
			return null;
		}
		try {
			return Long.parseLong(this.line);
		} catch (NumberFormatException exception) {
			return -1L;
		}
	}

	public void setLine(String line) {
		this.line = line;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public void addField(String key, Object value) {
		if (key.equals(ID_NAME)) {
			throw new IllegalArgumentException("Field name is not allowed!");
		}
		if (additionalFields == null) {
			additionalFields = new HashMap<String, Object>();
		}
		additionalFields.put(key, value);
	}

	public Map<String, Object> getAdditionalFields() {
		if (additionalFields == null) {
			return Collections.emptyMap();
		}
		return additionalFields;
	}
}
