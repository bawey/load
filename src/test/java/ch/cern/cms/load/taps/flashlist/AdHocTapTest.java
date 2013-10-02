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
	private double pace = 1000;

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

		ep.registerStatement("on levelZeroFM_static(FED_INFO_MAP!=FED_INFO_STR) as s set FED_INFO_STR=s.FED_INFO_MAP",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						String maptxt = (String) ((Map) newEvents[0].getUnderlying()).get("FED_INFO_STR");
						String[] tokens = maptxt.split(",");
						for (String token : tokens) {
							AdHocTapTest.this.output(token, 0);
						}
					}
				});

		// FMMInput fractionBusy(or Warning) > 0
		ep.registerStatement("select * from FMMInput where fractionBusy > 0 or fractionWarning > 0", new UpdateListener() {

			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				AdHocTapTest.this.output("fractions exceeded: " + newEvents[0].getUnderlying().toString(), 3);
			}
		});

		ep.createEPL("create window Backpressure.win:keepall() as (bpFraction double, slotNumber String, sessionid String)");
		ep.createEPL("on pattern[every a=frlcontrollerLink(fifoAlmostFullCnt>0)->b=frlcontrollerLink(fifoAlmostFullCnt>0,context=a.context,slotNumber=a.slotNumber,sessionid=a.sessionid,clockCount>a.clockCount)]"
				+ " insert into Backpressure select (b.fifoAlmostFullCnt-a.fifoAlmostFullCnt)/(b.clockCount-a.clockCount) as bpFraction, b.slotNumber as slotNumber, b.sessionid as sessionid");
		// 2 back-to-back events with fifoAlmostFullCnt > 0.

		ep.createEPL("create window PositiveBackpressure.win:keepall() as select * from Backpressure");
		ep.createEPL("insert into PositiveBackpressure select * from Backpressure where bpFraction>0");

		ep.registerStatement("select * from PositiveBackpressure", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				AdHocTapTest.this.output(newEvents[0].getUnderlying().toString(), 2);
			}
		});

		// ep.registerStatement(
		// "select (b.fifoAlmostFullCnt-a.fifoAlmostFullCnt)/(b.clockCount-a.clockCount) as bpFraction, b.slotNumber as slotNumber, b.sessionid as sessionid from pattern"
		// +
		// "[every a=frlcontrollerLink(fifoAlmostFullCnt>0)->b=frlcontrollerLink(fifoAlmostFullCnt>0,context=a.context,slotNumber=a.slotNumber,sessionid=a.sessionid,clockCount>a.clockCount)]",
		// new UpdateListener() {
		// @Override
		// public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		// AdHocTapTest.this.output(newEvents[0].getUnderlying().toString(), 2);
		// }
		// });

		// get the FED by link number

	}

	/**
	 * sum the rate in EVM as deltaN/deltaT for given session id
	 */

	public void task1(final EventProcessor ep) {
		/**
		 * creates a variable holding the SID associated with current
		 * PublicGlobal session
		 **/
		ep.createEPL("create variable String sid = ''");
		ep.registerStatement("on levelZeroFM_dynamic(SID!=sid, FMURL like '%PublicGlobal%') as l set sid=l.SID", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println("SID value updated to: " + newEvents[0].getUnderlying());
			}
		});

		/**
		 * create for EVM data. Keep only the most recent record per context-lid
		 * pair. Also, discard events older than 30 seconds
		 **/
		ep.createEPL("create window subrates.std:unique(url,lid).win:time(" + 30000 / pace
				+ " msec) as (subrate double, url String, lid String)");
		ep.createEPL("insert into subrates select deltaN/deltaT as subrate, context as url, lid from EVM where sessionid=sid and deltaT>0");

		/** create a window to contain observed rates **/
		ep.createEPL("create window rates.win:time(" + 60000 / pace + " sec) as (rate double)");
		/** and fill it periodically **/
		ep.createEPL("on pattern[every timer:interval(1)] insert into rates select sum(subrate) as rate from subrates");

		/**
		 * create variable for rate reference level (avg rate observed over last
		 * minute)
		 **/
		ep.createEPL("create variable double avgRate=0");
		ep.registerStatement("on rates set avgRate=(select avg(rate) from rates)", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				//System.out.println("average rate value updated to: " + newEvents[0].getUnderlying());
			}
		});
		// ep.registerStatement("select * from rates", verbose);

		ep.registerStatement("select avgRate, a.rate from pattern[every (a=rates(rate>1.3*avgRate) or a=rates(rate<0.7*avgRate)) ]",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						System.out.println("Deviation!" + newEvents[0].getUnderlying());
						ep.getRuntime().executeQuery("delete from rates");
					}
				});

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		tap.openStreams(ep);
	}

	private UpdateListener verbose = new UpdateListener() {

		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			System.out.println("# # # " + (newEvents != null ? newEvents.length : "null") + " # # # ");
			for (int i = 0; i < (newEvents != null ? newEvents.length : 0); ++i) {
				System.out.println(newEvents[i].getUnderlying());
			}
		}
	};

	public static final void main(String[] args) {
		try {
			AdHocTapTest ahtt = new AdHocTapTest();
			ahtt.setUp();
			ahtt.tap.openStreams(ahtt.ep);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
