package ch.cern.cms.load.taps.flashlist;

import java.util.HashMap;

import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.taps.EventsTap;

public abstract class AbstractFlashlistEventsTap implements EventsTap {

	public static final String PKEY_FIELD_TYPE = "PROPERTY_KEY_FIELD_NAME_TO_TYPE_MAP";
	protected static FieldTypeResolver resolver;

	@Override
	public void defineProperties(ExpertController controller) {
		/** Multiple taps might exists simultaneously **/
		synchronized (AbstractFlashlistEventsTap.class) {
			if (!controller.getSettings().containsKey(PKEY_FIELD_TYPE)) {
				resolver = new FieldTypeResolver();
				controller.getSettings().put(PKEY_FIELD_TYPE, resolver);
			}
		}
	}

	public static final class FieldTypeResolver extends
			HashMap<String, Class<?>> {
		private FieldTypeResolver() {
		}

		private static final long serialVersionUID = 1L;

		public Class<?> getFieldType(String fieldName) {
			return get(fieldName);
		}

		public Class<?> getFieldType(String fieldName, String listName) {
			Class<?> result = containsKey(listName + "." + fieldName) ? get(listName
					+ "." + fieldName)
					: null;
			result = result != null ? result : get(fieldName);
			return result != null ? result : String.class;
		}

		public void setFieldType(String fieldName, Class<?> type) {
			put(fieldName, type);
		}

		public void setFieldType(String fieldName, String listName,
				Class<?> type) {
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
}
