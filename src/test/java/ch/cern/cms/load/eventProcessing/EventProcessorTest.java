package ch.cern.cms.load.eventProcessing;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.eventProcessing.events.SubsystemCrossCheckerEvent;

public class EventProcessorTest {

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
	public void test() {
		EventProcessor ep = ExpertController.getInstance().getEventProcessor();
		ep.epl("select * from " + SubsystemCrossCheckerEvent.class.getSimpleName(), new UpdateListener() {

			@Override
			public void update(EventBean[] arg0, EventBean[] arg1) {
				System.out.println("A relief!");
			}
		});
		ep.sendEvent(new SubsystemCrossCheckerEvent("DAQ", true, 666, false));
	}
}
