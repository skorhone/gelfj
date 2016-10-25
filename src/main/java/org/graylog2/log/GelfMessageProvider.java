package org.graylog2.log;

import org.graylog2.message.GelfMessageBuilderConfiguration;

public interface GelfMessageProvider {
	public GelfMessageBuilderConfiguration getGelfMessageBuilderConfiguration();

	public boolean isExtractStacktrace();

	public boolean isAddExtendedInformation();

	public boolean isIncludeLocation();
}