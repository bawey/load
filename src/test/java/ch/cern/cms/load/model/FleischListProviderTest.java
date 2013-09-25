package ch.cern.cms.load.model;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.eventProcessing.EventProcessor;

public class FleischListProviderTest {

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
		EventProcessor ep = EventProcessor.getInstance();
		FleischListProvider flp = new FleischListProvider("/home/bawey/Desktop/flists/");
		flp.registerEventTypes(ep);
		
		
		ep.registerStatement("select * from FMMStatus", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println(newEvents[0].getUnderlying());
			}
		});
		flp.emit(ep, 1000000);
		
		//System.out.println("registered events: " + ep.getConfiguration().getEventTypes());
	}
}
