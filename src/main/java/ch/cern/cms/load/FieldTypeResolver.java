package ch.cern.cms.load;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public final class FieldTypeResolver extends HashMap<String, Class<?>> {
	// 2013-09-26T08:16:01.987334Z
	public final static SimpleDateFormat dateFormat = new SimpleDateFormat(Load.getInstance().getSettings().getProperty("dateFormat"));

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
			} else if (Collection.class.isAssignableFrom(type)) {
				rawValue = rawValue.substring(rawValue.indexOf('[') + 1, rawValue.lastIndexOf(']'));
				String[] array = rawValue.split(",");
				List<Object> list = new ArrayList<Object>(array.length);
				for (String s : array) {
					list.add(s);
				}
				return list;
			} else if (type.equals(Date.class)) {
				try {
					return dateFormat.parse(rawValue);
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return rawValue;
	}
}
