package ch.cern.cms.esper;

import com.espertech.esper.epl.agg.aggregator.AggregationMethod;

public class CustomConcatFunction implements AggregationMethod {

	private char DELIMITER = ' ';
	private String delimiter = "";
	private StringBuilder builder = new StringBuilder();
	private int enterCount = 0;

	public CustomConcatFunction() {
		builder = new StringBuilder();
		delimiter = "";
	}

	public Class getValueType() {
		return String.class;
	}

	@Override
	public void enter(Object value) {
		builder.append(builder.length() == 0 ? "" : ", ").append(value.toString());
	}

	@Override
	public void leave(Object value) {
		if (value != null) {
			int index = builder.indexOf(value.toString());
			builder.delete(index, Math.min(index + value.toString().length() + 2, builder.length()));
		}
	}

	@Override
	public Object getValue() {
		return builder.toString();
	}

	@Override
	public void clear() {
		builder = new StringBuilder();
	}

}
