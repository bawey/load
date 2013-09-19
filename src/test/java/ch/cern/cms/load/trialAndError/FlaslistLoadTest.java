package ch.cern.cms.load.trialAndError;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.configuration.Settings;
import ch.cern.cms.load.eventData.EventProcessorStatus;
import ch.cern.cms.load.eventProcessing.EventProcessor;
import ch.cern.cms.load.eventProcessing.EventProcessor.MultirowSubscriber;
import ch.cern.cms.load.mocks.MockEPSEventParser;
import ch.cern.cms.load.utils.Stats;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class FlaslistLoadTest {

	public static final String EPS = EventProcessorStatus.class.getSimpleName();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private void pumpEvents(EventProcessor ep) throws IOException {
		File file = new File("dmp/" + Settings.getInstance().flashlistDumpName);
		Assert.assertTrue(file.exists());
		MockEPSEventParser parser = new MockEPSEventParser();
		List<EventProcessorStatus> list = parser.bruteParse(file);

		// List<Long> accepted = new LinkedList<Long>();
		// List<Long> processed = new LinkedList<Long>();
		// List<Integer> pes = new LinkedList<Integer>();

		for (EventProcessorStatus eps : list) {
			ep.sendEvent(eps);
			/** this block could send some info for printouts **/
			// accepted.add(eps.getNbAccepted());
			// processed.add(eps.getNbProcessed());
			// pes.add(eps.getEpMicroStateInt() != null ?
			// eps.getEpMicroStateInt().size() : 0);
		}

		// System.out.println("BY_HAND: accepted: " + Stats.min(accepted) + ", "
		// + Stats.mean(accepted) + ", " + Stats.max(accepted));
		// System.out.println("BY_HAND: processed: " + Stats.min(processed) +
		// ", " + Stats.mean(processed) + ", " + Stats.max(processed));
		// System.out.println("BY_HAND: PES list: " + Stats.min(pes) + ", " +
		// Stats.mean(pes) + ", " + Stats.max(pes));

	}

	// @Test
	public void testOnDemandQueries() throws IOException {
		EventProcessor ep = EventProcessor.getInstance();
		pumpEvents(ep);

		EPOnDemandQueryResult rslt = ep.getRuntime().executeQuery("select * from " + EPS);
		System.out.println(rslt.getArray().length);
		for (EventBean eb : rslt.getArray()) {
			System.out.println(eb.getUnderlying());
		}

	}

	private void print(EPOnDemandQueryResult rslt) {
		if (rslt != null && rslt.getArray() != null) {
			System.out.println("Results size: " + rslt.getArray().length);
			for (EventBean eb : rslt.getArray()) {
				System.out.println(eb.getUnderlying());
			}
		} else {
			if (rslt == null) {
				System.out.println("rslt is null");
			} else {
				System.out.println("results array is null");
			}
		}

	}

	private int simpleCount = 0;

	// @Test
	public void testWithNamedWindowsAndOnDemandQueries() throws IOException {
		final EventProcessor ep = EventProcessor.getInstance();
		/** stuff for some debug, irrelevant **/
		ep.registerStatement("select * from " + EPS, new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				FlaslistLoadTest.this.simpleCount++;
			}
		});

		/**
		 * create Records stream containing individual cores - nbProcessed
		 * records
		 **/
		// ep.registerStatement("insert into Records select epMicroStateInt.size() as cores, nbProcessed as processed from "
		// + EPS,
		// dummySubscriber);

		/** create window so that the Records stream events don't get dropped **/
		ep.getAdministrator().createEPL("create window MyWindow.win:keepall() as (cores int, processed long)");
		ep.getAdministrator().createEPL(
				"insert into MyWindow select x.nbProcessed as processed, x.epMicroStateInt.size() as cores from " + EPS + " as x");

		/** create window holding groped (by cores) processed averages **/
		ep.getAdministrator().createEPL("create window Stats.std:unique(cores) as (cores int, throughput double)");
		ep.getAdministrator().createEPL("insert into Stats select cores, avg(processed) as throughput from MyWindow group by cores");

		/** query doesn't see qvrg l8r **/
		// ep.registerStatement("select avg(processed) as avrg from MyWindow",
		// new UpdateListener() {
		// @Override
		// public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		// ep.getConfiguration().addVariable("avrg", Double.class,
		// newEvents[0].get("avrg"));
		// }
		// });

		/**
		 * create window containing average and stddev of all nbProcessed and
		 * variables to hold these values (on-demand queries support no
		 * subqueries)
		 **/
		ep.getAdministrator().createEPL("create window JustAvg.std:lastevent() as(procAvg double, procStdev double)");
		ep.getAdministrator().createEPL(
				"insert into JustAvg select average as procAvg, stddev as procStdev from MyWindow.stat:uni(processed)");
		ep.getAdministrator().createEPL("create variable double avrg=0");
		ep.getAdministrator().createEPL("on JustAvg set avrg=procAvg");
		ep.getAdministrator().createEPL("create variable double sdev=0");
		ep.getAdministrator().createEPL("on JustAvg set sdev=procStdev");

		/** push all the events into engine **/
		pumpEvents(ep);

		/** run the on-demand queries **/
		print(ep.getRuntime().executeQuery("select s.cores, s.throughput from Stats as s where s.throughput > avrg+sdev"));
		print(ep.getRuntime().executeQuery("select * from JustAvg"));
		System.out.println("simple update count: " + simpleCount);

	}

	@Test
	public void findOutliersWithinGroups() throws IOException {
		EventProcessor ep = EventProcessor.getInstance();
		/** create window holding only the most-recent info per context **/
		ep.createEPL("create window Reads.std:unique(name) as (name String, yield long, units int)");
		ep.createEPL("insert into Reads select context as name, nbProcessed as yield, epMicroStateInt.size() as units from " + EPS+"");

		ep.createEPL("create window GroupStats.std:unique(units) as (units int, avrg double, sdev double)");
		ep.createEPL("insert into GroupStats select units, avg(yield) as avrg, stddev(yield) as sdev from Reads group by units");
		
		/** simply put, count all **/
		ep.createEPL("create window Overperformers.std:unique(name) as (name String, units int, yield long)");
		ep.createEPL("on GroupStats as gs delete from Overperformers o where o.yield < gs.avrg+3*gs.sdev and o.units=gs.units");
		ep.createEPL("insert into Overperformers select r.* from Reads as r, GroupStats as gs where r.units = gs.units and r.yield > gs.avrg+3*gs.sdev");
		
		
		/** try doing something more elaborate for underachievers **/
		ep.createEPL("create window Underperformers.std:unique(name) as (name String, units int, yield long)");
		ep.createEPL("on GroupStats as gs delete from Underperformers u where gs.units = u.units and u.yield > gs.avrg-3*gs.sdev");
		ep.createEPL("insert into Underperformers select r.* from Reads as r, GroupStats as gs where r.units = gs.units and r.yield < gs.avrg-3*gs.sdev");
		
		pumpEvents(ep);
		print(ep.getRuntime().executeQuery("select count(*) from Reads"));
		print(ep.getRuntime().executeQuery("select * from GroupStats"));
		System.out.println("overperformers:");
		print(ep.getRuntime().executeQuery("select * from Overperformers"));
		System.out.println("underperformers:");
		print(ep.getRuntime().executeQuery("select * from Underperformers"));
	}

	// @Test
	public void test() throws IOException {

		EventProcessor ep = EventProcessor.getInstance();
		ep.registerStatement("select irstream * from " + EventProcessorStatus.class.getSimpleName(), new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			}
		});

		/** try putting the processed-to-cores info first **/
		ep.getAdministrator()
				.createEPL("insert into CoreProc select nbProcessed as processed, epMicroStateInt.size() as cores from " + EPS);
		ep.getAdministrator().createEPL("insert into Performance select avg(processed) as throughput, cores from CoreProc group by cores");
		ep.getAdministrator().createEPL("insert into Ref select average as ref, stddev as sdev from CoreProc.stat:uni(processed)");
		ep.registerStatement(
				"select y.cores, y.throughput as avgThroughput from Performance.win:time_batch(1000 msec).std:unique(cores) as y where y.throughput > (select ref from Ref.std:lastevent()) + (select sdev from Ref.std:lastevent())",
				subscriber);
		pumpEvents(ep);
		try {
			Thread.sleep(1000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private MultirowSubscriber dummySubscriber = new EventProcessor.MultirowSubscriber() {

		@Override
		public void updateStart(int insertStreamLength, int removeStreamLength) {

		}

		@Override
		public void updateEnd() {
			// TODO Auto-generated method stub

		}

		@Override
		public void update(Map<?, ?> data) {
			// TODO Auto-generated method stub

		}
	};

	private MultirowSubscriber subscriber = new EventProcessor.MultirowSubscriber() {
		@Override
		public void update(Map<?, ?> data) {
			if (!data.isEmpty()) {
				// System.out.println(data.values().iterator().next().getClass().getSimpleName());
				if (data.values().iterator().next() instanceof Object[]) {
					// System.out.println("Map contains lists of size: ");
					for (int i = 0; i < ((Object[]) data.values().iterator().next()).length; ++i) {
						for (Object key : data.keySet()) {
							System.out.print(" " + key + "-" + ((Object[]) data.get(key))[i]);
						}
						System.out.println(".");
					}
				} else {
					System.out.println(data);
				}
			}
		}

		@Override
		public void updateStart(int insertStreamLength, int removeStreamLength) {
			System.out.println("Starting update with " + insertStreamLength + " and " + removeStreamLength + " events------**");
		}

		@Override
		public void updateEnd() {
			System.out.println("update end");
		}
	};
}
