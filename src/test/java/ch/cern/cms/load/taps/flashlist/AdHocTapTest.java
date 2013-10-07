package ch.cern.cms.load.taps.flashlist;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.FieldTypeResolver;
import ch.cern.cms.load.SwingTest;
import ch.cern.cms.load.taps.EventsTap;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class AdHocTapTest extends SwingTest {

	ExpertController ec;
	EventProcessor ep;
	EventsTap tap;
	private double pace = 7;

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
			ep = ec.getEventProcessor();
			FieldTypeResolver ftr = ec.getResolver();
			tap = new OfflineFlashlistEventsTap(ec, "/home/bawey/Desktop/flashlists/41/");
			((OfflineFlashlistEventsTap) tap).setPace(pace);
			ec.registerTap(tap);

			task1(ep);
			task2(ep);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * deadtime - detect when fractionBusy/fractionWarning is non-zero
	 */

	public void task2(EventProcessor ep) {
		// I need FED_INFO_MAP: FMM-FED_FRL connection
		ep.createEPL("create variable String FED_INFO_STR = ''");

		ep.registerStatement("on levelZeroFM_static(FED_INFO_MAP!=FED_INFO_STR) as s set FED_INFO_STR=s.FED_INFO_MAP", this.watchUpdater);

		// FMMInput fractionBusy(or Warning) > 0
		ep.registerStatement(
				"select context, geoslot, io, sid, timestamp, fractionBusy, fractionWarning from FMMInput where fractionBusy > 0 or fractionWarning > 0",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						AdHocTapTest.this.console("deadtime", "fractions exceeded: " + newEvents[0].getUnderlying().toString());
					}
				});

		ep.createEPL("create window BackpressureFilter.std:unique(kontext, slotNumber, linkNumber) as (bpFraction double, kontext String, slotNumber String, linkNumber String, timestamp String)");
		ep.createEPL("on pattern[every a=frlcontrollerLink(fifoAlmostFullCnt>0)->b=frlcontrollerLink(fifoAlmostFullCnt>0,context=a.context,slotNumber=a.slotNumber,sessionid=a.sessionid,clockCount>a.clockCount)]"
				+ " insert into BackpressureFilter select (b.fifoAlmostFullCnt-a.fifoAlmostFullCnt)/(b.clockCount-a.clockCount) as bpFraction, b.slotNumber as slotNumber, b.linkNumber as linkNumber, b.timestamp as timestamp, b.context as kontext");
		// 2 back-to-back events with fifoAlmostFullCnt > 0.

		ep.createEPL("create window Backpressure.std:unique(kontext, linkNumber, slotNumber) as select * from BackpressureFilter");

		// on every positive BackpressureFilter.bpFraction only if (kontext,
		// link, slot) not yet giving backpressure
		
		
		ep.registerStatement("on pattern[every bf=BackpressureFilter] select count(*) as backpressures4triplet from Backpressure where kontext=bf.kontext and linkNumber=bf.linkNumber and slotNumber=bf.slotNumber", watchUpdater);
		
		ep.createEPL("on BackpressureFilter(bpFraction>0) as bf insert into Backpressure select bf.* where (select count(*) as cnt from Backpressure where kontext=bf.kontext and linkNumber=bf.linkNumber and slotNumber=bf.slotNumber)=0");
				

		
		/** playground **/
		
//		ep.registerStatement(
//				"on BackpressureFilter(bpFraction>0) as bf insert into Backpressure select bf.*",
//				new UpdateListener() {
//					@Override
//					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
//						console("BP update", "updating Backpressure");
//						//AdHocTapTest.this.raiseAlarm(backpressureDesc(newEvents[0].getUnderlying()));
//					}
//				});
		
		/** playground ed **/
		
		
		
		
		
		
		
		ep.registerStatement("select count(*) as rowsInBackpressure from Backpressure", watchUpdater);


		ep.registerStatement("select b.* from pattern[every b=Backpressure]", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				console("debug", "type: " + newEvents[0].getUnderlying().toString());
				AdHocTapTest.this.watch(backpressureDesc(newEvents[0].getUnderlying()),
						((Map<?, ?>) newEvents[0].getUnderlying()).get("bpFraction").toString());
			}
		});

		// should fire only if deleting... and does
		ep.registerStatement(
				"on BackpressureFilter(bpFraction<0.0000000000000000000000000000000000000000000000000000000000000001) as bf delete from Backpressure b where b.kontext=bf.kontext and b.slotNumber=bf.slotNumber and b.linkNumber=bf.linkNumber",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						AdHocTapTest.this.console("backpressure", "removing backpressure");
						AdHocTapTest.this.cancelAlarm(backpressureDesc(newEvents[0].getUnderlying()));
					}
				});
	}

	/**
	 * sum the rate in EVM as deltaN/deltaT for given session id
	 */

	private String backpressureDesc(Object underlying) {
		Map<?, ?> meb = (Map<?, ?>) underlying;
		StringBuffer sb = new StringBuffer("BP! [ctx:").append(meb.get("kontext")).append("|s:").append(meb.get("slotNumber"))
				.append("|l:").append(meb.get("linkNumber")).append("]");
		return sb.toString();
	}

	public void task1(final EventProcessor ep) {
		/**
		 * creates a variable holding the SID associated with current
		 * PublicGlobal session
		 **/
		ep.createEPL("create variable String sid = ''");
		ep.registerStatement("on levelZeroFM_dynamic(SID!=sid, FMURL like '%PublicGlobal%') as l set sid=l.SID", watchUpdater);

		/**
		 * create for EVM data. Keep only the most recent record per context-lid
		 * pair. Also, discard events older than 30 seconds
		 **/
		ep.createEPL("create window subrates.std:unique(url,lid).win:time(" + 30000 / pace
				+ " msec) as (subrate double, url String, lid String)");
		ep.createEPL("insert into subrates select deltaN/deltaT as subrate, context as url, lid from EVM where sessionid=sid and deltaT>0");

		/**
		 * keep the last relevant subrate timestamp in a variable TODO: might
		 * arrive out-of-order!
		 **/
		ep.createEPL("create variable String lastEVMtimestamp = ''");
		ep.registerStatement("on EVM(sessionid=sid) as evm set lastEVMtimestamp=evm.timestamp", watchUpdater);

		/** create a window to contain observed rates **/
		ep.createEPL("create window rates.win:time(" + 60000 / pace + " sec) as (rate double)");
		/** and fill it periodically **/
		ep.registerStatement("on pattern[every timer:interval(" + (1000 / pace)
				+ " msec)] insert into rates select sum(subrate) as rate from subrates", watchUpdater);
		ep.registerStatement("select prior(1,rate) as trgRate, lastEVMtimestamp as timestamp from rates", watchUpdater);
		/**
		 * create variable for rate reference level (avg rate observed over last
		 * minute)
		 **/
		ep.createEPL("create variable double avgRate=0");
		ep.createEPL("on rates set avgRate=Math.round((select avg(rate) from rates))");

		ep.registerStatement("select avgRate, lastEVMtimestamp as timestamp from rates", watchUpdater);

		ep.registerStatement(
				"select avgRate, a.rate, lastEVMtimestamp as timestamp from pattern[every a=rates(rate>(avgRate*1.3) or rate < (avgRate*0.7))]",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						AdHocTapTest.this.console("rate jumps", newEvents[0].getUnderlying().toString());
						ep.getRuntime().executeQuery("delete from rates");
					}
				});

		ep.registerStatement("select count(*) as totalEVM from EVM", watchUpdater);

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
