package ch.cern.cms.load.taps.flashlist;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;

public class OfflineFlashlistEventsTapTest {

	private ExpertController expert;
	private OfflineFlashlistEventsTap tap;
	private EventProcessor ep;

	private boolean thermometerCalled = false;
	private boolean positionCalled = false;

	@Before
	public void setUp() {
		expert = ExpertController.getInstance();
		ep = ExpertController.getInstance().getEventProcessor();
		tap = new OfflineFlashlistEventsTap(expert, "/home/bawey/Workspace/LOAD/dmp/offlineFL/");
		tap.preRegistrationSetup();
		tap.registerEventTypes();

		ep.epl("select * from thermometer", new UpdateListener() {
			@Override
			public void update(EventBean[] arg0, EventBean[] arg1) {
				thermometerCalled = true;
			}
		});

		ep.epl("select * from position", new UpdateListener() {

			@Override
			public void update(EventBean[] arg0, EventBean[] arg1) {
				positionCalled = true;
			}
		});

	}

	@Test
	public void test() {
		tap.openStreams();
		Assert.assertTrue(thermometerCalled);
		Assert.assertTrue(positionCalled);
	}

}
