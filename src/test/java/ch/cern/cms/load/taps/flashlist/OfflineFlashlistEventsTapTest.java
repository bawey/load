package ch.cern.cms.load.taps.flashlist;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.eventProcessing.EventProcessor;

public class OfflineFlashlistEventsTapTest {

	private ExpertController expert;
	private OfflineFlashlistEventsTap tap;
	private EventProcessor ep;

	private boolean thermometerCalled = false;
	private boolean positionCalled = false;

	@Before
	public void setUp() {
		expert = ExpertController.getInstance();
		ep = EventProcessor.getInstance();
		tap = new OfflineFlashlistEventsTap("/home/bawey/Workspace/load/dmp/offlineFL/");
		tap.defineProperties(expert);
		tap.registerEventTypes(ep);

		ep.registerStatement("select * from thermometer", new UpdateListener() {
			@Override
			public void update(EventBean[] arg0, EventBean[] arg1) {
				thermometerCalled = true;
			}
		});

		ep.registerStatement("select * from position", new UpdateListener() {

			@Override
			public void update(EventBean[] arg0, EventBean[] arg1) {
				positionCalled = true;
			}
		});

	}

	@Test
	public void test() {
		tap.openStreams(ep);
		Assert.assertTrue(thermometerCalled);
		Assert.assertTrue(positionCalled);
	}

}
