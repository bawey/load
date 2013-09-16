package ch.cern.cms.load.trialAndError;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.configuration.Settings;
import ch.cern.cms.load.eventData.EventProcessorStatus;
import ch.cern.cms.load.eventProcessing.EventProcessor;
import ch.cern.cms.load.mocks.MockEPSEventParser;
import ch.cern.cms.load.utils.Stats;

public class FlaslistLoadTest {

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
		// ep.registerStatement("select * from " +
		// EventProcessorStatus.class.getSimpleName(), new UpdateListener() {
		// @Override
		// public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		// System.out.println("something happened! "+newEvents[0].getUnderlying());
		// }
		// });

		ep.registerStatement("select avg(nbProcessed) as average from " + EventProcessorStatus.class.getSimpleName()
				+ " group by epMicroStateInt.size()", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println("something happened with avg: !!" + newEvents[0].get("average"));
			}
		});

		for (EventProcessorStatus eps : list) {
			ep.sendEvent(eps);
			accepted.add(eps.getNbAccepted());
			processed.add(eps.getNbProcessed());
			pes.add(eps.getEpMicroStateInt() != null ? eps.getEpMicroStateInt().size() : 0);
		}
		System.out.println("BY_HAND: accepted: " + Stats.min(accepted) + ", " + Stats.mean(accepted) + ", " + Stats.max(accepted));
		System.out.println("BY_HAND: processed: " + Stats.min(processed) + ", " + Stats.mean(processed) + ", " + Stats.max(processed));
		System.out.println("BY_HAND: PES list: " + Stats.min(pes) + ", " + Stats.mean(pes) + ", " + Stats.max(pes));

	}

}
