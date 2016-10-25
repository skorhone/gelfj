package org.graylog2.message;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class GelfMessageBuilderConfiguration {
	private String facility;
	private String originHost;
	private Map<String, String> additionalFields;

	public String getFacility() {
		return facility;
	}

	public void setFacility(String facility) {
		this.facility = facility;
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

	public Map<String, String> getAdditionalFields() {
		if (additionalFields == null) {
			additionalFields = new HashMap<String, String>();
		}
		return additionalFields;
	}

	public void setAdditionalFields(Map<String, String> additionalFields) {
		this.additionalFields = additionalFields;
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
