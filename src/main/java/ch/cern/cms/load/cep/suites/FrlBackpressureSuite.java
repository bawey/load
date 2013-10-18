package ch.cern.cms.load.cep.suites;

import java.util.HashMap;
import java.util.Map;

import com.espertech.esper.client.EPStatement;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.eventData.FrlControllerLink;

public class FrlBackpressureSuite extends EplSuite {

	public static final String FRL_BACKPRESSURE = "FRL_BACKPRESSURE";

	public static final String KONTEXT = "kontext";
	public static final String LINK = "link";
	public static final String SLOT = "slot";
	public static final String BP_FRACTION = "bpFraction";
	public static final String TIMESTAMP = "timestamp";

	public static final String[] BACKPRESSURE_COLUMNS = new String[] { KONTEXT, LINK, SLOT, BP_FRACTION, TIMESTAMP };

	static {
		types.put(KONTEXT, String.class);
		types.put(LINK, Integer.class);
		types.put(SLOT, Integer.class);
		types.put(BP_FRACTION, Double.class);
		types.put(TIMESTAMP, String.class);
	}

	public FrlBackpressureSuite(EventProcessor ep) {
		super(ep);
	}

	public void run() {

		epl(createWindow(FRL_BACKPRESSURE, BACKPRESSURE_COLUMNS, RetentionPolicy.unique(KONTEXT, SLOT, LINK)));
		epl("on pattern[every a={0}]", new Object[] {frlcontrollerLink});

		/** create BackpressureFilter window **/
		ep.createEPL("create window BackpressureFilter.std:unique(kontext, slotNumber, linkNumber) as (bpFraction double, kontext String, slotNumber Integer, linkNumber Integer, timestamp String)");
		/** connect BackpressureFilter with data sources **/
		ep.createEPL("on pattern[every a=frlcontrollerLink(fifoAlmostFullCnt>0)->b=frlcontrollerLink(fifoAlmostFullCnt>0,context=a.context,slotNumber=a.slotNumber,sessionid=a.sessionid,clockCount>a.clockCount)]"
				+ " insert into BackpressureFilter select (b.fifoAlmostFullCnt-a.fifoAlmostFullCnt)/(b.clockCount-a.clockCount) as bpFraction, b.slotNumber as slotNumber, b.linkNumber as linkNumber, b.timestamp as timestamp, b.context as kontext");

		/** create Backpressure window to hold most recent values for triplets **/
		ep.createEPL("create window Backpressure.std:unique(kontext, linkNumber, slotNumber) as select * from BackpressureFilter");
		/** create stream for BackpressureAlarms **/
		createConclusionStream(ep, "BackpressureAlarm", "(title String) copyfrom BackpressureFilter");

		/**
		 * split the BackpressureFilter and insert into BackpressureAlarms and/or Backpressure when necessary
		 **/
		ep.createEPL("on BackpressureFilter(bpFraction>0) as bf "
				+ "insert into BackpressureAlarm select 'frl backpressure' as title, bf.kontext as kontext, bf.linkNumber as linkNumber, bf.slotNumber as slotNumber where (select count(*) as cnt from Backpressure where kontext=bf.kontext and linkNumber=bf.linkNumber and slotNumber=bf.slotNumber)=0"
				+ " insert into Backpressure select bf.*" + " output all");
		/** cancel the associated alarm when bpFraction goes back down for a triplet **/
		ep.createEPL("on BackpressureFilter(bpFraction=0) as bf delete from Backpressure b where bf.linkNumber = b.linkNumber and bf.slotNumber = b.slotNumber and bf.kontext=b.kontext");
		ep.createEPL("on BackpressureFilter(bpFraction=0) as bf delete from Conclusions c where "
				+ "c.title.toString()='frl backpressure' and c.kontext.toString()=bf.kontext.toString() and c.slotNumber.toString()=bf.slotNumber.toString() and c.linkNumber.toString()=bf.linkNumber.toString()");

		ep.registerStatement(
				"select b.bpFraction as value, 'BP '||b.kontext||'L'||b.linkNumber.toString()||'S'||b.slotNumber.toString() as label from pattern[every b=Backpressure]",
				watchUpdater);
	}
}
