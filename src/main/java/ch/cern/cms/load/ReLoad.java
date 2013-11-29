package ch.cern.cms.load;

import com.espertech.esper.epl.generated.EsperEPL2GrammarParser.newAssign_return;
import com.espertech.esper.metrics.codahale_metrics.metrics.core.Histogram;
import com.espertech.esper.metrics.codahale_metrics.metrics.core.MetricsRegistry;

public class ReLoad {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MetricsRegistry mr = new MetricsRegistry();
		Histogram h = mr.newHistogram(ReLoad.class, "test");
		System.out.println(h.toString());
		
	}

}
