package org.graylog2.json;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class JSON {
	@SuppressWarnings("unchecked")
	public static StringBuilder encode(Object object, StringBuilder sb) {
		if (object == null) {
			return encode(null, sb);
		}
		Class<?> type = object.getClass();
		if (Map.class.isAssignableFrom(type)) {
			return encodeMap((Map<? extends Object, ? extends Object>)object, sb);
		}
		if (type.isArray()) {
			return encodeCollection(Arrays.asList((Object[])object), sb);
		}
		if (Collection.class.isAssignableFrom(type)) {
			return encodeCollection((Collection<? extends Object>)object, sb);
		}
		if (BigDecimal.class.isAssignableFrom(type)) {
			return sb.append(((BigDecimal)object).toPlainString());
		}
		if (Number.class.isAssignableFrom(type)) {
			return sb.append(object.toString());
		}
		return encode(object.toString(), sb);
	}
	
	public static StringBuilder encodeCollection(Collection<? extends Object> collection, StringBuilder sb) {
		sb.append('[');
		boolean first = true;
		for (Object object : collection) {
			if (!first) {
				sb.append(", ");
			}
			encode(object, sb);
			first = false;
		}
		return sb.append(']');
	}
	
	public static StringBuilder encodeMap(Map<? extends Object, ? extends Object> map, StringBuilder sb) {
		sb.append('{');
		boolean first = true;
		for (Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
			if (entry.getValue() != null) {
				if (!first) {
					sb.append(", ");
				}
				encode(entry.getKey().toString(), sb);
				sb.append(": ");
				encode(entry.getValue(), sb);
				first = false;
			}
		}
		return sb.append('}');
	}
	
	public static StringBuilder encode(String string, StringBuilder sb) {
		if (string == null) {
			return sb.append("null");
		}
		if (string.length() == 0) {
			return sb.append("\"\"");
		}

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
					sb.append(String.format("\\u%04X", (int)c));
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
		return sb;
	}
}
