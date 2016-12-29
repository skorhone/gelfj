package org.graylog2.message;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class GelfMessageBuilderConfiguration {
	private String originHost;
	private boolean extractStacktrace;
	private Map<String, String> additionalFields;
	
	public GelfMessageBuilderConfiguration() {
		this.additionalFields = new HashMap<String, String>();
		this.extractStacktrace = true;
	}

	public void setFacility(String facility) {
		addAdditionalField("facility", facility);
	}
	
	public void setExtractStacktrace(boolean extractStacktrace) {
		this.extractStacktrace = extractStacktrace;
	}

	public String getOriginHost() {
		if (originHost == null) {
			originHost = getLocalHostName();
		}
		return originHost;
	}

	public void setOriginHost(String originHost) {
		this.originHost = originHost;
	}

	public boolean isExtractStacktrace() {
		return extractStacktrace;
	}

	public Map<String, String> getAdditionalFields() {
		return additionalFields;
	}

	public void addAdditionalField(String key, String value) {
		this.additionalFields.put(key, value);
	}

	public void addAdditionalFields(Map<String, String> additionalFields) {
		this.additionalFields.putAll(additionalFields);
	}

	private String getLocalHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException uhe) {
			throw new IllegalStateException(
					"Origin host could not be resolved automatically. Please set originHost property", uhe);
		}
	}
}
