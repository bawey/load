package ch.cern.cms.load.taps.flashlist;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rcms.common.db.DBConnectorException;
import rcms.utilities.hwcfg.eq.FED;
import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.FieldTypeResolver;
import ch.cern.cms.load.SwingTest;
import ch.cern.cms.load.hwdb.HwInfo;
import ch.cern.cms.load.taps.EventsTap;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class AdHocTapTest extends SwingTest {

	ExpertController ec;
	EventProcessor ep;
	EventsTap tap;
	private double pace = 30;
	HwInfo nn = HwInfo.getInstance();

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
			ep = ec.getEventProcessor();
			ep.getConfiguration().addImport(HwInfo.class.getName());
			FieldTypeResolver ftr = ec.getResolver();
			tap = new OfflineFlashlistEventsTap(ec, "/home/bawey/Desktop/flashlists/41/");
			((OfflineFlashlistEventsTap) tap).setPace(pace);
			ec.registerTap(tap);
			createConclusionStreams(ep);
			task1(ep);
			backpressure(ep);
			deadtime(ep);
			deadVsPressure(ep);
			registerSillyDebugs(ep);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void registerSillyDebugs(EventProcessor ep) {
		ep.registerStatement(
				"on pattern[every bf=BackpressureFilter] select count(*) as value, 'BP 4 current 3plet' as label from Backpressure where kontext=bf.kontext and linkNumber=bf.linkNumber and slotNumber=bf.slotNumber",
				watchUpdater);
		ep.registerStatement("select count(*) as value, 'BP3lets' as label from Backpressure", watchUpdater);
		ep.registerStatement("select HWCFG_KEY as value, 'HWCFG_KEY' as label from levelZeroFM_static", watchUpdater);
		ep.registerStatement("select GLOBAL_CONF_KEY as value, 'GLOBAL_CONF_KEY' as label from levelZeroFM_static", watchUpdater);

		ep.createEPL("create variable String FED_INFO_STR = ''");
		ep.createEPL("on levelZeroFM_static(FED_INFO_MAP!=FED_INFO_STR) as s set FED_INFO_STR=s.FED_INFO_MAP");
		ep.registerStatement("select FED_INFO_STR as value, 'FED INFO MAP' as label from EVM as evm", watchUpdater);

	}

	private void createConclusionStream(EventProcessor ep, String name, String body) {
		ep.createEPL("create schema " + name + " " + body);
		// forward conclusions to the general conclusion stream
		ep.createEPL("insert into ConclusionsStream select * from " + name);

	}

	private void createConclusionStreams(EventProcessor ep) {
		ep.createEPL("create variant schema ConclusionsStream as *");

		ep.createEPL("create window Conclusions.win:keepall() as select * from ConclusionsStream");
		ep.createEPL("insert into Conclusions select * from ConclusionsStream");

		ep.registerStatement("select c.* from pattern[every c=Conclusions]", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				AdHocTapTest.this.raiseAlarm(newEvents[0].getUnderlying().toString());
			}
		});

		ep.registerStatement("select rstream * from Conclusions", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				AdHocTapTest.this.cancelAlarm(newEvents[0].getUnderlying().toString());
			}
		});

		ep.registerStatement("select * from Conclusions", new UpdateListener() {

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
				+ "insert into BackpressureAlarm select 'backpressure' as title, bf.kontext as kontext, bf.linkNumber as linkNumber, bf.slotNumber as slotNumber where (select count(*) as cnt from Backpressure where kontext=bf.kontext and linkNumber=bf.linkNumber and slotNumber=bf.slotNumber)=0"
				+ " insert into Backpressure select bf.*" + " output all");
		/** cancel the associated alarm when bpFraction goes back down for a triplet **/
		ep.createEPL("on BackpressureFilter(bpFraction=0) as bf delete from Backpressure b where bf.linkNumber = b.linkNumber and bf.slotNumber = b.slotNumber and bf.kontext=b.kontext");
		ep.createEPL("on BackpressureFilter(bpFraction=0) as bf delete from Conclusions c where c.kontext.toString()=bf.kontext.toString() and c.slotNumber.toString()=bf.slotNumber.toString() and c.linkNumber.toString()=bf.linkNumber.toString()");

		ep.registerStatement(
				"select b.bpFraction as value, 'BP '||b.kontext||'L'||b.linkNumber.toString()||'S'||b.slotNumber.toString() as label from pattern[every b=Backpressure]",
				watchUpdater);

	}

	public void deadtime(EventProcessor ep) {
		ep.registerStatement(
				"select 'frBsy: '||context||'G'||geoslot.toString()||'IO'||io.toString() as label, timestamp, fractionBusy as value from FMMInput where fractionBusy > 0",
				watchUpdater);
		ep.registerStatement(
				"select 'frWrn: '||context||'G'||geoslot.toString()||'IO'||io.toString() as label, timestamp, fractionWarning as value from FMMInput where fractionWarning > 0",
				watchUpdater);

		/** want to copy datatypes, hence the stream will be created from window **/
		ep.createEPL("create window DeadtimeTmp.win:keepall() as select context as kontext, geoslot, io from FMMInput");
		createConclusionStream(ep, "DeadtimeAlarm", "(title String) copyfrom DeadtimeTmp ");

		ep.createEPL("on FMMInput(fractionWarning>0 or fractionBusy>0) as fmi insert into DeadtimeAlarm select "
				+ "'deadtime' as title, fmi.context as kontext, fmi.geoslot as geoslot, fmi.io as io where "
				+ "(select count(*) from DeadtimeAlarm.win:keepall() where kontext=fmi.context and geoslot=fmi.geoslot and io = fmi.io)=0");

		ep.createEPL("on FMMInput(fractionWarning<0.000001, fractionBusy < 0.00001) as fmi delete from Conclusions as c where"
				+ " c.kontext.toString() = fmi.context and c.io.toString() = fmi.io.toString() and c.geoslot.toString() = fmi.geoslot.toString()");

	}

	/** KeepItSimple, for now **/
	public void deadVsPressure(final EventProcessor ep) {

		ep.createEPL("create window BpFeds.win:keepall() as (id String)");
		ep.createEPL("create window DtFeds.win:keepall() as (id String)");

		HwInfo.esperCheck();

		/** naively hold bp-guilty feds **/
		ep.createEPL("on pattern[every c=Conclusions(title='backpressure')] insert into BpFeds(id) select HwInfo.getInstance().getFedId(c.kontext, c.slotNumber.toString() ,c.linkNumber.toString() ,CmsHw.FRL).toString()");
		ep.createEPL("on BackpressureFilter(bpFraction<0.000000000000001) as bf delete from BpFeds where id = HwInfo.getInstance().getFedId(bf.kontext, bf.slotNumber.toString(), bf.linkNumber, CmsHw.FRL).toString()");
		ep.registerStatement("select count(*) as bpFeds from BpFeds", watchUpdater);

		/** naively hold dt-guilty feds **/
		ep.createEPL("on pattern[every c=Conclusions(title='deadtime')] insert into DtFeds(id) select"
				+ " HwInfo.getInstance().getFedId(c.kontextm, c.geoslot.toString(), c.io.toString(), CmsHw.FMM, 'inserting DT').toString()");
		ep.createEPL("on FMMInput(fractionWarning=0, fractionBusy=0) as fmi delete from DtFeds where" +
				" id = HwInfo.getInstance().getFedId(fmi.context, fmi.geoslot.toString(), fmi.io, CmsHw.FMM, 'removing DT').toString()");
		
		ep.registerStatement("select count(*) as dtFeds from DtFeds", watchUpdater);
		ep.registerStatement("select id as dtFedId from DtFeds", consoleLogger);

		ep.registerStatement("select * from BpFeds as bpf, DtFeds as dtf where bpf.id=dtf.id", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				console("backpressure FEDS", newEvents[0].getUnderlying().toString());
			}
		});

		ep.registerStatement("select * from BpFeds", consoleLogger);
		ep.registerStatement("select * from DtFeds", consoleLogger);

	}

	/**
	 * sum the rate in EVM as deltaN/deltaT for given session id
	 */

	public void task1(final EventProcessor ep) {
		/**
		 * creates a variable holding the SID associated with current PublicGlobal session
		 **/
		ep.createEPL("create variable String sid = ''");
		ep.createEPL("on levelZeroFM_dynamic(SID!=sid, FMURL like '%PublicGlobal%') as l set sid=l.SID");

		/**
		 * create for EVM data. Keep only the most recent record per context-lid pair. Also, discard events older than 30 seconds
		 **/
		ep.createEPL("create window subrates.std:unique(url,lid).win:time(" + 30000 / pace + " msec) as (subrate double, url String, lid String)");

		ep.createEPL("insert into subrates select deltaN/deltaT as subrate, context as url, lid from EVM where sessionid=sid and deltaT>0");
		/**
		 * keep the last relevant subrate timestamp in a variable TODO: might arrive out-of-order!
		 **/
		ep.createEPL("create variable String lastEVMtimestamp = ''");
		ep.createEPL("on EVM(sessionid=sid) as evm set lastEVMtimestamp=evm.timestamp");
		ep.registerStatement("select lastEVMtimestamp as value, 'lastEVM time' as label from EVM", watchUpdater);

		/** create a window to contain observed rates **/
		ep.createEPL("create window rates.win:time(" + 60000 / pace + " sec) as (rate double)");
		/** and fill it periodically **/
		ep.createEPL("on pattern[every timer:interval(" + (1000 / pace) + " msec)] insert into rates select sum(subrate) as rate from subrates");
		ep.registerStatement("select prior(1,rate) as value, 'trigger rate' as label, lastEVMtimestamp as timestamp from rates", watchUpdater);
		/**
		 * create variable for rate reference level (avg rate observed over last minute)
		 **/
		ep.createEPL("create variable double avgRate=0");
		ep.createEPL("on rates set avgRate=Math.round((select avg(rate) from rates))");

		ep.registerStatement("select avgRate as value, lastEVMtimestamp as timestamp, 'avg rate' as label from rates", watchUpdater);

		ep.registerStatement("select avgRate, a.rate, lastEVMtimestamp as timestamp from pattern[every a=rates(rate>(avgRate*1.3) or rate < (avgRate*0.7))]",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						AdHocTapTest.this.console("rate jumps", newEvents[0].getUnderlying().toString());
						ep.getRuntime().executeQuery("delete from rates");
					}
				});

		ep.registerStatement("select count(*) as value, 'totalEVM' as label from EVM", watchUpdater);

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		((OfflineFlashlistEventsTap) tap).openStreams(ep, 600000);
	}

	public static final void main(String[] args) {
		try {
			AdHocTapTest ahtt = new AdHocTapTest();
			ahtt.setUp();
			((OfflineFlashlistEventsTap) ahtt.tap).openStreams(ahtt.ep, 700000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
