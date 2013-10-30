package ch.cern.cms.load.taps.flashlist;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class OfflineFlashlistEventsTapTestRealData {

	private Load expert;
	private OfflineFlashlistEventsTap tap;
	private EventProcessor ep;

	@Before
	public void setUp() {
		expert = Load.getInstance();
		ep = Load.getInstance().getEventProcessor();
		tap = new OfflineFlashlistEventsTap(expert, "/home/bawey/Documents/flashlists/41/");
		tap.preRegistrationSetup();
		tap.registerEventTypes();
	}

	@Test
	public void test() {
		ep.epl("select * from hostInfo", new UpdateListener() {

			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println(newEvents[0].getUnderlying());
			}
		});
		tap.openStreams();
	}

}
