package ch.cern.cms.load.taps.flashlist;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.SwingTest;
import ch.cern.cms.load.hwdb.CmsHw;
import ch.cern.cms.load.hwdb.HwInfo;
import ch.cern.cms.load.taps.AbstractEventsTap;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.event.map.MapEventBean;

public class AdHocTapTest extends SwingTest {

	ExpertController ec;
	EventProcessor ep;

	/**
	 * SETUP FOR DEADTIME AND DB AND BACKPRESSURE - FULL
	 */
	private double pace = 1;
	private long advance = 700000;
	HwInfo hwInfo = HwInfo.getInstance();

	/**
	 * SETUP FOR OUTPERFORMERS
	 */
	// private double pace = 3;
	// private long advance = 700000; // 700000R
	// HwInfo hwInfo = null;// HwInfo.getInstance();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() {
		super.setUp();
		try {
			ec = ExpertController.getInstance();
			ec.getResolver().setFieldType("deltaT", Double.class);
			ec.getResolver().setFieldType("deltaN", Double.class);
			ec.getResolver().setFieldType("fifoAlmostFullCnt", Long.class);
			ec.getResolver().setFieldType("fractionBusy", Double.class);
			ec.getResolver().setFieldType("fractionWarning", Double.class);
			ec.getResolver().setFieldType("clockCount", Double.class);
			ec.getResolver().setFieldType("linkNumber", Integer.class);
			ec.getResolver().setFieldType("slotNumber", Integer.class);
			ec.getResolver().setFieldType("geoslot", Integer.class);
			ec.getResolver().setFieldType("io", Integer.class);
			ec.getResolver().setFieldType("epMacroStateInt", List.class);
			ec.getResolver().setFieldType("nbProcessed", Long.class);
			ec.getResolver().setFieldType("bxNumber", Long.class);
			ec.getResolver().setFieldType("triggerNumber", Long.class);
			ec.getResolver().setFieldType("FEDSourceId", Integer.class);
			ep = ec.getEventProcessor();
			ep.getConfiguration().addImport(HwInfo.class.getName());

			AbstractEventsTap.registerKnownOfflineTaps();

			AbstractEventsTap.setOfflineTapsPace(pace);
			AbstractEventsTap.setOfflineTapsPosition(advance);

			createConclusionStreams(ep);
			rateJumps(ep);
			performers(ep);
			bx(ep);
			if (hwInfo != null) {
				backpressure(ep);
				deadtime(ep);
				deadVsPressure(ep);
				registerSillyDebugs(ep);
				fedIds(ep);
			}
			// scriptTest(ep);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fedIds(EventProcessor ep) {
		// detect any pair of frlcontrollerLink events with the same FEDSourceId and differing (link, slot, context) triplet
		ep.epl("select 'duplicate SourceFedId' as problem, a.context, a.linkNumber, a.FEDSourceId, a.slotNumber, b.context, b.slotNumber, b.linkNumber from "
				+ "pattern[every a=frlcontrollerLink -> "
				+ "b=frlcontrollerLink(FEDSourceId=a.FEDSourceId and (linkNumber!=a.linkNumber or context!=a.context or slotNumber!=a.slotNumber))]",
				errorLogger);
		// make sure that given triplet resolves to the same FEDSrcId
		ep.epl("create window ConfirmedFeds.win:keepall() as select FEDSourceId as fedId from frlcontrollerLink");
		ep.epl("create window DeniedFeds.win:keepall() as select * from ConfirmedFeds");
		ep.epl("on frlcontrollerLink as a insert into ConfirmedFeds select a.FEDSourceId as fedId where a.FEDSourceId not in (select fedId from ConfirmedFeds) "
				+ "and a.FEDSourceId = HwInfo.getInstance().getFedId(a.context, a.slotNumber, a.linkNumber, CmsHw.FRL)");

		ep.epl("select count(*) as confirmedFeds from ConfirmedFeds", watchUpdater);

	}

	private void scriptTest(EventProcessor ep) {

		StringBuilder script2 = new StringBuilder();
		script2.append("create expression print(n) [");
		script2.append("importClass(java.lang.System);");
		script2.append("System.out.println(123);");
		script2.append("0.12");
		script2.append("]");

		ep.epl(script2);
		ep.createEPL("select print(bxNumber) as reMagic from frlcontrollerLink", watchUpdater);

		StringBuilder script = new StringBuilder();
		script.append("create expression double getNumber(n) [");
		script.append("importClass(java.lang.System);");
		script.append("System.out.println(n);");
		script.append("0.12");
		script.append("]");

		ep.epl(script.toString());
		ep.createEPL("select getNumber(bxNumber) as magic from frlcontrollerLink", watchUpdater);

		StringBuilder fib = new StringBuilder();
		fib.append("create expression double js:fib(num) [ ");
		fib.append("importClass(java.lang.System);");
		fib.append("System.out.println(\"calling JS fib(\"+num+\") method\");");
		fib.append("fib(num);");
		fib.append("System.out.println(\"defining fib method\");");
		fib.append("function fib(n){");
		fib.append("System.out.println(\"running fib(\"+n+\")\");");
		fib.append("	if(n<=1){ return n;}");
		fib.append("	else {return (fib(n-1)+fib(n-2));}");
		fib.append("}");
		fib.append("System.out.println(\"defined fib method\");");
		fib.append("]");
		ep.epl(fib);
		ep.createEPL("select fib(1) as fibBx from frlcontrollerLink", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println("FIBONUMBER: " + newEvents[0].getUnderlying().toString());
			}
		});

	}

	private void registerSillyDebugs(EventProcessor ep) {
		ep.epl("on pattern[every bf=BackpressureFilter] select count(*) as value, 'BP 4 current 3plet' as label from Backpressure where kontext=bf.kontext and linkNumber=bf.linkNumber and slotNumber=bf.slotNumber",
				watchUpdater);
		ep.epl("select count(*) as value, 'BP3lets' as label from Backpressure", watchUpdater);
		ep.epl("select HWCFG_KEY as value, 'HWCFG_KEY' as label from levelZeroFM_static", watchUpdater);
		ep.epl("select GLOBAL_CONF_KEY as value, 'GLOBAL_CONF_KEY' as label from levelZeroFM_static", watchUpdater);

		ep.epl("create variable String FED_INFO_STR = ''");
		ep.epl("on levelZeroFM_static(FED_INFO_MAP!=FED_INFO_STR) as s set FED_INFO_STR=s.FED_INFO_MAP");
		ep.epl("select FED_INFO_STR as value, 'FED INFO MAP' as label from EVM as evm", watchUpdater);
		ep.epl("select count(*) as conclusions from Conclusions", watchUpdater);
		ep.epl("select count(*) as bpFeds from BpFeds", watchUpdater);
		// ep.registerStatement("select rstream count(*) as ppFedsRemoved from BpFeds",
		// watchUpdater);

		ep.epl("select irstream * from BpFeds", new UpdateListener() {

			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				if (newEvents != null) {
					console("BpFeds", "added");
					for (EventBean bean : newEvents) {
						console("BpFeds", bean.getUnderlying().toString());
					}
				}
				if (oldEvents != null) {
					console("BpFeds", "removed");
					for (EventBean bean : oldEvents) {
						console("BpFeds", bean.getUnderlying().toString());
					}
				}
			}
		});
	}

	private void createConclusionStream(EventProcessor ep, String name, String body) {
		ep.epl("create schema " + name + " " + body);
		// forward conclusions to the general conclusion stream
		ep.epl("insert into ConclusionsStream select * from " + name);

	}

	private void createConclusionStreams(EventProcessor ep) {
		ep.epl("create variant schema ConclusionsStream as *");

		ep.epl("create window Conclusions.win:keepall() as select * from ConclusionsStream");
		ep.epl("insert into Conclusions select * from ConclusionsStream");

		ep.epl("select c.* from pattern[every c=Conclusions]", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				AdHocTapTest.this.raiseAlarm(newEvents[0].getUnderlying().toString());
			}
		});

		ep.epl("select rstream * from Conclusions", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				for (EventBean bean : newEvents) {
					AdHocTapTest.this.cancelAlarm(bean.getUnderlying().toString());
				}
			}
		});

		ep.epl("select * from Conclusions", new UpdateListener() {

			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				console("allConclusions", newEvents[0].getUnderlying().toString());
			}
		});
	}

	/**
	 * deadtime - detect when fractionBusy/fractionWarning is non-zero
	 */

	public void backpressure(EventProcessor ep) {
		/** create BackpressureFilter window **/
		ep.epl("create window BackpressureFilter.std:unique(kontext, slotNumber, linkNumber) as (bpFraction double, kontext String, slotNumber Integer, linkNumber Integer, timestamp String)");
		/** connect BackpressureFilter with data sources **/
		ep.epl("on pattern[every a=frlcontrollerLink(fifoAlmostFullCnt>0)->b=frlcontrollerLink(fifoAlmostFullCnt>0,context=a.context,slotNumber=a.slotNumber,sessionid=a.sessionid,clockCount>a.clockCount)]"
				+ " insert into BackpressureFilter select (b.fifoAlmostFullCnt-a.fifoAlmostFullCnt)/(b.clockCount-a.clockCount) as bpFraction, b.slotNumber as slotNumber, b.linkNumber as linkNumber, b.timestamp as timestamp, b.context as kontext");

		/** create Backpressure window to hold most recent values for triplets **/
		ep.epl("create window Backpressure.std:unique(kontext, linkNumber, slotNumber) as select * from BackpressureFilter");
		/** create stream for BackpressureAlarms **/
		createConclusionStream(ep, "BackpressureAlarm", "(title String) copyfrom BackpressureFilter");

		/**
		 * split the BackpressureFilter and insert into BackpressureAlarms and/or Backpressure when necessary
		 **/
		ep.epl("on BackpressureFilter(bpFraction>0) as bf "
				+ "insert into BackpressureAlarm select 'frl backpressure' as title, bf.kontext as kontext, bf.linkNumber as linkNumber, bf.slotNumber as slotNumber where (select count(*) as cnt from Backpressure where kontext=bf.kontext and linkNumber=bf.linkNumber and slotNumber=bf.slotNumber)=0"
				+ " insert into Backpressure select bf.*" + " output all");
		/**
		 * cancel the associated alarm when bpFraction goes back down for a triplet
		 **/
		ep.epl("on BackpressureFilter(bpFraction=0) as bf delete from Backpressure b where bf.linkNumber = b.linkNumber and bf.slotNumber = b.slotNumber and bf.kontext=b.kontext");
		ep.epl("on BackpressureFilter(bpFraction=0) as bf delete from Conclusions c where "
				+ "c.title.toString()='frl backpressure' and c.kontext.toString()=bf.kontext.toString() and c.slotNumber.toString()=bf.slotNumber.toString() and c.linkNumber.toString()=bf.linkNumber.toString()");

		ep.epl("select b.bpFraction as value, 'BP '||b.kontext||'L'||b.linkNumber.toString()||'S'||b.slotNumber.toString() as label from pattern[every b=Backpressure]",
				watchUpdater);

	}

	public void deadtime(EventProcessor ep) {
		ep.epl("select 'frBsy: '||context||'G'||geoslot.toString()||'IO'||io.toString() as label, timestamp, fractionBusy as value from FMMInput where fractionBusy > 0",
				watchUpdater);
		ep.epl("select 'frWrn: '||context||'G'||geoslot.toString()||'IO'||io.toString() as label, timestamp, fractionWarning as value from FMMInput where fractionWarning > 0",
				watchUpdater);

		/** want to copy datatypes, hence the stream will be created from window **/
		ep.epl("create window DeadtimeTmp.win:keepall() as select context as kontext, geoslot, io from FMMInput");
		createConclusionStream(ep, "DeadtimeAlarm", "(title String) copyfrom DeadtimeTmp ");

		ep.epl("on FMMInput(fractionWarning>0 or fractionBusy>0) as fmi insert into DeadtimeAlarm select "
				+ "'fmm deadtime' as title, fmi.context as kontext, fmi.geoslot as geoslot, fmi.io as io where "
				+ "(select count(*) from DeadtimeAlarm.win:keepall() where kontext=fmi.context and geoslot=fmi.geoslot and io = fmi.io)=0");

		ep.epl("on FMMInput(fractionWarning=0, fractionBusy=0) as fmi delete from Conclusions as c where"
				+ " c.title.toString()='fmm deadtime' and c.kontext.toString() = fmi.context and c.io.toString() = fmi.io.toString() and c.geoslot.toString() = fmi.geoslot.toString()");

	}

	/** KeepItSimple, for now **/
	public void deadVsPressure(final EventProcessor ep) {

		ep.epl("create window BpFeds.win:keepall() as (id Integer)");
		ep.epl("create window DtFeds.win:keepall() as (id Integer, kontext String, geoslot Integer, io Integer)");

		HwInfo.esperCheck();

		/** naively hold bp-guilty feds **/
		ep.epl("on pattern[every c=BackpressureAlarm(title='frl backpressure')] insert into BpFeds(id) select HwInfo.getInstance().getFedId(c.kontext, c.slotNumber, c.linkNumber, CmsHw.FRL ) where "
				+ "HwInfo.getInstance().getFedId(c.kontext, c.slotNumber ,c.linkNumber ,CmsHw.FRL) is not null");
		ep.epl("on BackpressureFilter(bpFraction=0) as bf delete from BpFeds as bfeds where bfeds.id = HwInfo.getInstance().getFedId(bf.kontext, bf.slotNumber, bf.linkNumber, CmsHw.FRL )",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						console("BpFeds", "sth deleted!");
					}
				});

		/**
		 * seems really hard to retrieve the set of Ids and use them to instantiate events, so we'll go around
		 **/
		ep.epl("select c.* from pattern[every c=Conclusions(title='deadtime')] ", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				Map<?, ?> attrs = (Map<?, ?>) newEvents[0].getUnderlying();
				Collection<Integer> fedIds = HwInfo.getInstance().getDeadtimeRelevantFedIds(attrs.get("kontext"), attrs.get("geoslot"), attrs.get("io"));
				console("e(x)", fedIds.toString());
				for (Integer id : fedIds) {
					Map<Object, Object> map = new HashMap<Object, Object>();
					map.put("id", id);
					map.putAll(attrs);
					ep.getRuntime().sendEvent(map, "DtFeds");
				}
			}
		});
		ep.epl("on FMMInput(fractionWarning=0, fractionBusy=0) as fmi delete from DtFeds as dtf where"
				+ " kontext=fmi.context and dtf.geoslot=fmi.geoslot and dtf.io=fmi.io");

		ep.epl("select count(*) as dtFeds from DtFeds", watchUpdater);

		ep.epl("select id as dtFedId, kontext, geoslot, io from DtFeds", consoleLogger);

		createConclusionStream(ep, "misFEDs", "(title String, fedId int)");
		ep.epl("on pattern[ every bfed=BpFeds or dfed=DtFeds ] "
				+ "insert into misFEDs select 'backpressure and deadtime' as title, bfed.id as fedId where bfed is not null and bfed.id in (select id from DtFeds) "
				+ "insert into misFEDs select 'backpressure and deadtime' as title, dfed.id as fedId where dfed is not null and dfed.id in (select id from BpFeds) "
				+ "insert into misFEDs select 'backpressure' as title, bfed.id as fedId where bfed is not null and bfed.id is not null "
				+ "insert into misFEDs select 'deadime' as title, dfed.id as fedId where dfed is not null and dfed.id is not null");

		// the best would be just to detect deletions :-( like "on rstream Sth"
		ep.epl("on pattern[every timer:interval(1 msec)] delete from Conclusions as c where c.title.toString().contains('backpressure') and "
				+ "c.fedId is not null and c.fedId.toString() not in (select id.toString() from BpFeds)");

		ep.epl("on pattern[every timer:interval(1 msec)] delete from Conclusions as c where c.title.toString().contains('deadtime') and "
				+ "c.fedId is not null and c.fedId.toString() not in (select id.toString() from DtFeds)");

		ep.epl("select id.toString() as id from DtFeds", new UpdateListener() {

			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				for (EventBean b : newEvents) {
					console("DtFeds", b.getUnderlying().toString());
				}
			}
		});

		ep.epl("select count(*) as suspiciousFeds from BpFeds as bpf, DtFeds as dtf where bpf.id=dtf.id", watchUpdater);

		ep.epl("select * from BpFeds", consoleLogger);
		ep.epl("select * from DtFeds", consoleLogger);

	}

	public void performers(EventProcessor ep) {
		ep.epl("create variable Double tolerance = 2.79");

		/** create window holding only the most-recent info per context **/

		ep.epl("create window Reads.std:unique(name) as (name String, yield long, units int)");
		ep.epl("insert into Reads select context as name, nbProcessed as yield, epMacroStateInt.size() as units from EventProcessorStatus");

		ep.epl("create window GroupStats.std:unique(units) as (units int, avrg double, sdev double)");

		ep.epl("on pattern[every timer:interval(1000 msec)] "
				+ "insert into GroupStats select r.units as units, avg(r.yield) as avrg, stddev(r.yield) as sdev from Reads as r group by r.units");

		/** simply put, count all **/
		ep.epl("create window Overperformers.std:unique(name) as (name String, units int, yield long)");
		ep.epl("on GroupStats as gs " + "insert into Overperformers select r.* from Reads as r where r.units=gs.units and r.yield > gs.avrg+tolerance*gs.sdev");

		/** try doing something more elaborate for under-achievers **/
		ep.epl("create window Underperformers.std:unique(name) as (name String, units int, yield long)");
		ep.epl("on GroupStats as gs insert into Underperformers" + " select r.* from Reads as r where r.units=gs.units and r.yield < gs.avrg-tolerance*gs.sdev");
		ep.epl("on GroupStats as gs "
				+ "delete from Underperformers u where gs.units = u.units and (select r.yield from Reads as r where r.name = u.name) > gs.avrg-tolerance*gs.sdev");
		ep.epl("on GroupStats as gs "
				+ "delete from Overperformers o where gs.units = o.units and (select r.yield from Reads as r where r.name = o.name) < gs.avrg+tolerance*gs.sdev");

		ep.epl("select * from GroupStats", consoleLogger);
		ep.epl("select 'under' as title, u.*, gs.avrg-tolerance*gs.sdev as threshold from Underperformers as u, GroupStats as gs where u.units = gs.units",
				consoleLogger);
		ep.epl("select 'over' as title, o.*, gs.avrg+tolerance*gs.sdev as threshold from Overperformers as o, GroupStats as gs where o.units = gs.units",
				consoleLogger);

		ep.epl("select count(*) as underperformers from Underperformers", watchUpdater);
		ep.epl("select count(*) as overperformers from Overperformers", watchUpdater);

	}

	public void bx(EventProcessor ep) {
		// measure how long the trigger rate has been 0

		// if long enough, check bxNumber and triggerNumber of frlcontrollerLink
		// + report on the ones standing out! (unless there is dynamic
		// backPressureDyn on them)

		ep.epl("create variable int stuckTime = 2");

		ep.epl("create window frlBxValues.std:unique(bxNumber).win:time(stuckTime sec) as (bxNumber Long, cnt Long)");
		ep.epl("create window frlTrgValues.std:unique(trgNumber).win:time(stuckTime sec) as (trgNumber Long, cnt Long)");

		ep.epl("create window frlBuffer.std:unique(slotNumber, context, linkNumber) as select * from frlcontrollerLink");
		ep.epl("on frlcontrollerLink fcl insert into frlBuffer select fcl.*", consoleLogger);

		ep.epl("on pattern[every timer:interval(500 msec)] "
				+ "insert into frlBxValues select bxNumber as bxNumber, count(*) as cnt from frlBuffer group by bxNumber ");
		ep.epl("on pattern[every timer:interval(500 msec)] "
				+ " insert into frlTrgValues select triggerNumber as trgNumber, count(*) as cnt from frlBuffer group by triggerNumber");

		ep.epl("on pattern[every timer:interval(500 msec)] delete from frlBxValues as fbc where fbc.bxNumber not in (select bxNumber from frlBuffer)");
		ep.epl("on pattern[every timer:interval(500 msec)] delete from frlTrgValues as ftv where ftv.trgNumber not in (select triggerNumber from frlBuffer)");

		ep.epl("select count(*) as frlCL from frlcontrollerLink", watchUpdater);
		ep.epl("select count(*) as frlBuffer from frlBuffer", watchUpdater);

		ep.epl("select count(*) as bxNumbers from frlBxValues", watchUpdater);
		ep.epl("select count(*) as trgNumbers from  frlTrgValues", watchUpdater);

		/** these three should be linked into sth more of a hierarchy **/
		ep.epl("on pattern[every timer:interval(stuckTime sec) and not rates(rate>0)] "
				+ "select 'rates stuck, bxNumbers inconsistent' as problem, count(*) as bxNumbers from frlBxValues where "
				+ "(select count(*) from frlBxValues)>1", errorLogger);

		ep.epl("on pattern[every timer:interval(stuckTime sec) and not rates(rate>0)] "
				+ "select 'rates stuck, triggerNumbers inconsistent' as problem, count(*) as trgNumbers from frlTrgValues where "
				+ "(select count(*) from frlTrgValues)>1", errorLogger);

		ep.epl("select 'rate stuck at 0 for ' || stuckTime.toString() ||' sec' as msg from pattern[every timer:interval(stuckTime sec) and not rates(rate>0) "
				+ "]", consoleLogger);

		/************************** just dummy debugs *****************************/

		ep.epl("on pattern[every timer:interval(1 sec)] select * from frlBxValues", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				StringBuilder sb = new StringBuilder();
				sb.append("current frlBxValues: \n");
				for (EventBean b : newEvents) {
					sb.append(((MapEventBean) b).get("stream_0").toString()).append("\n");
				}
				console("frlBxValues", sb.toString());
			}
		});

		ep.epl("on pattern[every timer:interval(1 sec)] select * from frlTrgValues", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				StringBuilder sb = new StringBuilder();
				sb.append("current frlTrgValues: \n");
				for (EventBean b : newEvents) {
					sb.append(((MapEventBean) b).get("stream_0").toString()).append("\n");
				}
				console("frlTrgValues", sb.toString());
			}
		});

		ep.epl("select * from frlBxValues", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				console("insertedBx", newEvents[0].getUnderlying().toString() + " at: " + new Date().toString());
			}
		});

		ep.epl("select * from frlTrgValues", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				console("insertedTrg", newEvents[0].getUnderlying().toString() + " at: " + new Date().toString());
			}
		});

	}

	/**
	 * sum the rate in EVM as deltaN/deltaT for given session id
	 */

	public void rateJumps(final EventProcessor ep) {
		/**
		 * creates a variable holding the SID associated with current PublicGlobal session
		 **/
		ep.epl("create variable String sid = ''");
		ep.epl("on levelZeroFM_dynamic(SID!=sid, FMURL like '%PublicGlobal%') as l set sid=l.SID");

		/**
		 * create for EVM data. Keep only the most recent record per context-lid pair. Also, discard events older than 30 seconds
		 **/
		ep.epl("create window subrates.std:unique(url,lid).win:time(" + 30000 / pace + " msec) as (subrate double, url String, lid String)");

		ep.epl("insert into subrates select deltaN/deltaT as subrate, context as url, lid from EVM where sessionid=sid and deltaT>0");
		/**
		 * keep the last relevant subrate timestamp in a variable TODO: might arrive out-of-order!
		 **/
		ep.epl("create variable String lastEVMtimestamp = ''");
		ep.epl("on EVM(sessionid=sid) as evm set lastEVMtimestamp=evm.timestamp");
		ep.epl("select lastEVMtimestamp as value, 'lastEVM time' as label from EVM", watchUpdater);

		/** create a window to contain observed rates **/
		ep.epl("create window rates.win:time(" + 60000 / pace + " msec) as (rate double)");
		/** and fill it periodically **/
		ep.epl("on pattern[every timer:interval(" + (1000 / pace) + " msec)] insert into rates select sum(subrate) as rate from subrates");
		ep.epl("select prior(1,rate) as value, 'trigger rate' as label, lastEVMtimestamp as timestamp from rates", watchUpdater);

		StringBuilder script = new StringBuilder();

		script.append("create expression double js:averageTriggerRate(n, m) [");
		script.append("importClass(java.lang.System);");
		script.append("System.out.println(\"received value n: \"+n+\" of type: \"+typeof(n));");
		script.append("System.out.println(\"received value m: \"+m+\" of type: \"+typeof(m));");
		script.append("if(n!=null){ n; }");
		script.append("else if(m!=null){ m; }");
		script.append("else { 0; }");
		script.append("]");
		ep.epl(script);

		ep.epl("create variable Double avgTrgRate=null");
		ep.epl("create variable Double indirectAvgTrgRate = null");
		ep.epl("create variable Double lastEverRate=null");
		ep.epl("on pattern[every r=rates] set " + "lastEverRate = r.rate," + "indirectAvgTrgRate = (select avg(rate) as rate from rates).rate,"
				+ "avgTrgRate = Math.round(averageTriggerRate(indirectAvgTrgRate,lastEverRate))");

		ep.epl("select avgTrgRate as value, lastEVMtimestamp as timestamp, 'avg rate' as label from rates", watchUpdater);

		ep.epl("select avgTrgRate, a.rate, lastEVMtimestamp as timestamp from pattern[every a=rates(rate>(avgTrgRate*1.3) or rate < (avgTrgRate*0.7))]",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						AdHocTapTest.this.console("rate jumps", newEvents[0].getUnderlying().toString());
						ep.getRuntime().executeQuery("delete from rates");
					}
				});

		ep.epl("select count(*) as value, 'totalEVM' as label from EVM", watchUpdater);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		console("default", "starting");
		ec.openTaps();
	}

	public static final void main(String[] args) {
		try {
			AdHocTapTest ahtt = new AdHocTapTest();
			ahtt.setUp();
			ahtt.test();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
