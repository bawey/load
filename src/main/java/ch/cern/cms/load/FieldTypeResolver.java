package ch.cern.cms.load;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.espertech.esper.epl.generated.EsperEPL2GrammarParser.firstAggregation_return;

import fieldTypes.AbstractListOfData;
import fieldTypes.ListOfDoubles;
import fieldTypes.ListOfStrings;

public final class FieldTypeResolver extends HashMap<String, Class<?>> {
	// 2013-09-26T08:16:01.987334Z

	private Load load;
	public final static SimpleDateFormat dateFormat = new SimpleDateFormat(Load.getInstance().getSettings().getProperty("dateFormat"));

	private int dateTrimLength;

	protected FieldTypeResolver() {
		this.load = Load.getInstance();
		dateTrimLength = load.getSettings().getProperty("dateFormat").replace("'", "").length();
		crudeSetup();
		System.out.println("producing resolver!");
	}

	private static final long serialVersionUID = 1L;

	public Class<?> getFieldType(String fieldName) {
		return get(fieldName);
	}

	public Class<?> getFieldType(String fieldName, String listName) {
		Class<?> result = this.containsKey(listName + "." + fieldName) ? this.get(listName + "." + fieldName) : null;
		result = result != null ? result : this.get(fieldName);
		return result != null ? result : String.class;
	}

	public void setFieldType(String fieldName, Class<?> type) {
		put(fieldName, type);
	}

	public void setFieldType(String fieldName, String listName, Class<?> type) {
		put(listName + "." + fieldName, type);
	}

	public Object convert(String input, String field, String list) {
		return convert(input, field, list, null);
	}

	public Object convert(String rawValue, String fieldName, String listName, Class<?> typeOverride) {
		Class<?> type = typeOverride == null ? this.getFieldType(fieldName, listName) : typeOverride;
		if (type != null) {
			try {
				if (type.equals(Integer.class)) {
					return Integer.parseInt(rawValue);
				} else if (type.equals(Long.class)) {
					return Long.parseLong(rawValue);
				} else if (type.equals(Double.class)) {
					return Double.parseDouble(rawValue);
				} else if (type.equals(Date.class)) {
					try {
						return dateFormat.parse(rawValue.substring(0, dateTrimLength));
					} catch (ParseException e) {
						throw new RuntimeException(e);
					}
				} else if (type.equals(ListOfStrings.class)) {
					return new ListOfStrings(rawValue);
				} else if (type.equals(ListOfDoubles.class)) {
					return new ListOfDoubles(rawValue);
				} else if (type.isArray()) {
					String[] tokens = AbstractListOfData.tokenizeFlashlistInput(rawValue);
					if (type.getComponentType().equals(String.class)) {
						return tokens;
					} else {
						Object result = Array.newInstance(type.getComponentType(), tokens.length);
						for (int i = 0; i < tokens.length; ++i) {
							Array.set(result, i, convert(tokens[i], null, null, type.getComponentType()));
						}
						return result;
					}
				} else if (Collection.class.isAssignableFrom(type)) {
					rawValue = rawValue.substring(rawValue.indexOf('[') + 1, rawValue.lastIndexOf(']'));
					String[] array = rawValue.split(",");
					List<Object> list = new ArrayList<Object>(array.length);
					for (String s : array) {
						list.add(s);
					}
					return list;
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed convert " + listName + "[" + fieldName + "] to " + type.getSimpleName() + " from " + rawValue, e);
			}
		}
		return rawValue;
	}

	// TODO: placeholder really. so far
	public boolean isRolled(String propertyName, String eventName) {
		return false;
		// return propertyName.equals("streamNames") || propertyName.equals("ratePerStream");
	}

	private void crudeSetup() {
		setFieldType("deltaT", Double.class);
		setFieldType("deltaN", Double.class);
		setFieldType("fifoAlmostFullCnt", Long.class);
		setFieldType("fractionBusy", Double.class);
		setFieldType("fractionWarning", Double.class);
		setFieldType("clockCount", Double.class);
		setFieldType("linkNumber", Integer.class);
		setFieldType("slotNumber", Integer.class);
		setFieldType("geoslot", Integer.class);
		setFieldType("io", Integer.class);
		setFieldType("epMacroStateInt", List.class);
		setFieldType("epMicroStateInt", List.class);
		setFieldType("nbProcessed", Long.class);
		setFieldType("bxNumber", Long.class);
		setFieldType("triggerNumber", Long.class);
		setFieldType("FEDSourceId", Long.class);
		setFieldType("timestamp", Date.class);
		setFieldType("lastEVMtimestamp", Date.class);
		setFieldType("streamNames", String[].class);
		setFieldType("ratePerStream", Double[].class);

		setFieldType("myrinetLastResyncEvt", Long.class);
		setFieldType("myrinetResync", Long.class);
		setFieldType("cpuUsage", Double.class);
		setFieldType("timeTag", Long.class);
		setFieldType("integralTimeBusy", Long.class);
		setFieldType("integralTimeError", Long.class);
		setFieldType("integralTimeOOS", Long.class);
		setFieldType("integralTimeWarning", Long.class);
		setFieldType("integralTimeReady", Long.class);
		
		/**
		 * outputFractionReadyA as  fracReady,
           x.outputFractionBusyA as  fracBusy,
           x.outputFractionWarningA as  fracWarning,
           x.outputFractionOOSA as  fracOOS,
           x.outputFractionErrorA
		 */
		setFieldType("outputFractionReadyA", Double.class);
		setFieldType("outputFractionBusyA", Double.class);
		setFieldType("outputFractionWarningA", Double.class);
		setFieldType("outputFractionOOSA", Double.class);
		setFieldType("outputFractionErrorA", Double.class);
		
		setFieldType("outputFractionReadyB", Double.class);
		setFieldType("outputFractionBusyB", Double.class);
		setFieldType("outputFractionWarningB", Double.class);
		setFieldType("outputFractionOOSB", Double.class);
		setFieldType("outputFractionErrorB", Double.class);
		
	}
}
