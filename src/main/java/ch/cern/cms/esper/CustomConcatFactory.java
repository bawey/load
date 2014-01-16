package ch.cern.cms.esper;

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationValidationContext;

public class CustomConcatFactory implements AggregationFunctionFactory {

	@Override
	public void setFunctionName(String functionName) {

	}

	@Override
	public void validate(AggregationValidationContext validationContext) {
	}

	@Override
	public AggregationMethod newAggregator() {
		return new CustomConcatFunction();
	}

	@Override
	public Class<?> getValueType() {
		return String.class;
	}

}
