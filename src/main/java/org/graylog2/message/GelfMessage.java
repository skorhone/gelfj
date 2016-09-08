package org.graylog2.message;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.graylog2.json.JSON;

public class GelfMessage {
	private static final String ID_NAME = "id";
	private static final String GELF_VERSION = "1.1";
	private static final BigDecimal TIME_DIVISOR = new BigDecimal(1000);

	private String version = GELF_VERSION;
	private String host;
	private String shortMessage;
	private String fullMessage;
	private long javaTimestamp;
	private String level;
	private String facility = "gelf-java";
	private String line;
	private String file;
	private Map<String, Object> additonalFields = new HashMap<String, Object>();

	public GelfMessage() {
	}

	public GelfMessage(String shortMessage, String fullMessage, long timestamp, String level) {
		this(shortMessage, fullMessage, timestamp, level, null, null);
	}

	public GelfMessage(String shortMessage, String fullMessage, long timestamp, String level, String line,
			String file) {
		this.shortMessage = shortMessage != null ? shortMessage : "null";
		this.fullMessage = fullMessage;
		this.javaTimestamp = timestamp;
		this.level = level;
		this.line = line;
		this.file = file;
	}

	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"version\": ").append(JSON.encodeQuoted(getVersion())).append(", ");
		sb.append("\"host\": ").append(JSON.encodeQuoted(getHost())).append(", ");
		sb.append("\"short_message\": ").append(JSON.encodeQuoted(getShortMessage())).append(", ");
		sb.append("\"full_message\": ").append(JSON.encodeQuoted(getFullMessage())).append(", ");
		sb.append("\"timestamp\": ").append(getTimestamp()).append(", ");
		sb.append("\"facility\": ").append(JSON.encodeQuoted(getFacility())).append(", ");
		if (getFile() != null) {
			sb.append("\"file\": ").append(JSON.encodeQuoted(getFile())).append(", ");
		}
		Long line = getLine();
		if (line != null) {
			sb.append("\"line\": ").append(line).append(", ");
		}
		sb.append("\"level\": ").append(getLevel());

		for (Map.Entry<String, Object> additionalField : additonalFields.entrySet()) {
			if (!ID_NAME.equals(additionalField.getKey())) {
				String key = JSON.encodeQuoted("_" + additionalField.getKey());
				Object objectValue = additionalField.getValue();
				String value;
				if (objectValue != null) {
					if (objectValue instanceof Number) {
						value = objectValue.toString();
					} else if (objectValue instanceof Boolean) {
						value = objectValue.toString();
					} else {
						value = JSON.encodeQuoted(additionalField.getValue().toString());
					}
				} else {
					value = "null";
				}
				sb.append(", ").append(key).append(": ").append(value);
			}
		}
		sb.append("}");
		return sb.toString();
	}

	public int getCurrentMillis() {
		return (int) System.currentTimeMillis();
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

	public String getFacility() {
		return facility;
	}

	public void setFacility(String facility) {
		this.facility = facility;
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

	public GelfMessage addField(String key, String value) {
		getAdditonalFields().put(key, value);
		return this;
	}

	public GelfMessage addField(String key, Object value) {
		getAdditonalFields().put(key, value);
		return this;
	}

	public Map<String, Object> getAdditonalFields() {
		return additonalFields;
	}

	public void setAdditonalFields(Map<String, Object> additonalFields) {
		this.additonalFields = new HashMap<String, Object>(additonalFields);
	}

	public boolean isValid() {
		return isShortOrFullMessagesExists() && !isEmpty(version) && !isEmpty(host) && !isEmpty(facility);
	}

	private boolean isShortOrFullMessagesExists() {
		return shortMessage != null || fullMessage != null;
	}

	public boolean isEmpty(String str) {
		return str == null || "".equals(str.trim());
	}
}
