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

	@Test
	public void test() throws IOException {
		File file = new File("dmp/" + Settings.getInstance().flashlistDumpName);
		Assert.assertTrue(file.exists());
		MockEPSEventParser parser = new MockEPSEventParser();
		List<EventProcessorStatus> list = parser.bruteParse(file);

		List<Long> accepted = new LinkedList<Long>();
		List<Long> processed = new LinkedList<Long>();
		List<Integer> pes = new LinkedList<Integer>();

		EventProcessor ep = EventProcessor.getInstance();
		ep.registerStatement("select irstream * from " + EventProcessorStatus.class.getSimpleName(), new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				// System.out.println("trivial condition met, new events: " +
				// (newEvents != null ? newEvents.length : "null")
				// + ", old events: " + (oldEvents != null ? oldEvents.length :
				// "null"));
			}
		});

		/** try putting the processed-to-cores info first **/
		ep.registerStatement("insert into Stats select nbProcessed as processed, epMicroStateInt.size() as cores from " + EPS,
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						// System.out.println("inserted new data");
					}
				});

		ep.registerStatement("insert into SthNew select avg(processed) as throughput, cores from Stats group by cores", dummySubscriber);
		ep.registerStatement("insert into Ref select avg(processed) as ref from Stats", dummySubscriber);
		ep.registerStatement(
				"select y.cores, y.throughput as avgThroughput from SthNew.win:time_batch(1000 msec).std:unique(cores) as y where y.throughput > (select x.ref from Ref.std:lastevent() as x)",
				subscriber);

		// ep.registerStatement("select avg(processed) as average, cores from Stats group by cores",
		// new UpdateListener() {
		// @Override
		// public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		// System.out.println("selected and grouped, new events: " + (newEvents
		// != null ? newEvents.length : "null")
		// + ", old events: " + (oldEvents != null ? oldEvents.length :
		// "null"));
		// System.out.println(newEvents[0].getUnderlying().toString());
		// }
		// });

		// EPStatement statement = ep.registerStatement(
		// "select {avg(s.processed)} as average, {s.cores} from Stats.win:length_batch(100) as s group by cores",
		// subscriber);

		// ep.registerStatement("select avg(eps.nbProcessed) from " + EPS +
		// ".win:keepall() as eps", subscriber);

		// EPStatement statement =
		// ep.registerStatement("select s.cores, s.processed from Stats as s where processed > (select avg(eps.nbProcessed) from "
		// + EPS
		// + ".win:keepall() as eps)", subscriber);

		// EPStatement statement = ep
		// .registerStatement(
		// "select s.cores, avg(s.processed) from Stats as s having avg(s.processed) > (select average from Stats.win:length(10).stat:uni(processed)) from "
		// + EPS + ".win:keepall() as eps)", subscriber);

		// ep.registerStatement("select {avg(s.processed)} as average, {s.cores} from Stats.win:time_batch(1 sec) as s,  "
		// + EPS
		// +
		// " as eps group by cores having avg(s.processed) > avg(eps.nbProcessed)",
		// subscriber);

		/** respond to a new insert into stats **/
		// ep.registerStatement("select average from Averages", new
		// UpdateListener() {
		// @Override
		// public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		// System.out.println("selected from averages: " +
		// newEvents[0].get("average"));
		// }
		// });

		// ep.registerStatement("select * from " +
		// EventProcessorStatus.class.getSimpleName()
		// + " where nbProcessed > select avg(nbProcessed) from " +
		// EventProcessorStatus.class.getSimpleName(), new UpdateListener() {
		// @Override
		// public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		// System.out.println("average triggered");
		// for (EventBean newEvent : newEvents) {
		// System.out.println("avg: " + newEvent.get("average"));
		// }
		// }
		// });

		for (EventProcessorStatus eps : list) {
			ep.sendEvent(eps);
			accepted.add(eps.getNbAccepted());
			processed.add(eps.getNbProcessed());
			pes.add(eps.getEpMicroStateInt() != null ? eps.getEpMicroStateInt().size() : 0);
		}
		System.out.println("BY_HAND: accepted: " + Stats.min(accepted) + ", " + Stats.mean(accepted) + ", " + Stats.max(accepted));
		System.out.println("BY_HAND: processed: " + Stats.min(processed) + ", " + Stats.mean(processed) + ", " + Stats.max(processed));
		System.out.println("BY_HAND: PES list: " + Stats.min(pes) + ", " + Stats.mean(pes) + ", " + Stats.max(pes));

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
