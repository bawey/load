package ch.cern.cms.load;

import java.util.HashMap;

public final class FieldTypeResolver extends HashMap<String, Class<?>> {
	protected FieldTypeResolver() {
	}

	private static final long serialVersionUID = 1L;

	public Class<?> getFieldType(String fieldName) {
		return get(fieldName);
	}

	public Class<?> getFieldType(String fieldName, String listName) {
		Class<?> result = containsKey(listName + "." + fieldName) ? get(listName + "." + fieldName) : null;
		result = result != null ? result : get(fieldName);
		return result != null ? result : String.class;
	}

	public void setFieldType(String fieldName, Class<?> type) {
		put(fieldName, type);
	}

	public void setFieldType(String fieldName, String listName, Class<?> type) {
		put(listName + "." + fieldName, type);
	}

	public Object convert(String rawValue, String fieldName, String listName) {
		Class<?> type = this.getFieldType(fieldName, listName);
		if (type != null) {
			if (type.equals(Integer.class)) {
				return Integer.parseInt(rawValue);
			} else if (type.equals(Long.class)) {
				return Long.parseLong(rawValue);
			} else if (type.equals(Double.class)) {
				return Double.parseDouble(rawValue);
			}
		}
		return rawValue;
	}
}
