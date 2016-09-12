package org.graylog2.json;

public class JSON {
	public static String encodeQuoted(String string) {
		if (string == null) {
			return "null";
		}
		if (string.length() == 0) {
			return "\"\"";
		}
		StringBuilder sb = new StringBuilder(string.length() + 4);

		sb.append('"');
		for (int i = 0; i < string.length(); i += 1) {
			char c = string.charAt(i);
			if (c < ' ') {
				switch (c) {
				case '\b':
					sb.append("\\b");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\r':
					sb.append("\\r");
					break;
				default:
					sb.append(String.format("\\u%04X", c));
					break;
				}
			} else if (c == '"' || c == '/' || c == '\\') {
				sb.append('\\');
				sb.append(c);
			} else {
				sb.append(c);
			}
		}
		sb.append('"');
		return sb.toString();
	}
}
