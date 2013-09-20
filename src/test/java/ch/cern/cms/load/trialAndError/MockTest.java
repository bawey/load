package ch.cern.cms.load.trialAndError;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.eventData.Mock;
import ch.cern.cms.load.eventProcessing.EventProcessor;

public class MockTest {

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

	public static final String MOCK = Mock.class.getName();

	@Test
	public void test() {
		EventProcessor ep = EventProcessor.getInstance();
		ep.getConfiguration().addEventType(Mock.class);
		ep.registerStatement("select * from " + MOCK, new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println(newEvents[0].getUnderlying());
			}
		});
		Mock.pumpEvents(ep);

	}

}
