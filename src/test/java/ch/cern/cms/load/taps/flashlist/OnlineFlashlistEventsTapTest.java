package ch.cern.cms.load.taps.flashlist;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UnmatchedListener;

import ch.cern.cms.load.Load;

public class OnlineFlashlistEventsTapTest {

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
	public void test() throws InterruptedException {
		// ExpertController ec = ExpertController.getInstance();
		// for (String flRoot : ec.getSettings().getFlashlistsRoots())
		// ec.registerTap(new OnlineFlashlistEventsTap(ec, flRoot));
		//
		// ec.getEventProcessor().getRuntime().setUnmatchedListener(new UnmatchedListener() {
		// @Override
		// public void update(EventBean theEvent) {
		// System.out.println(theEvent.getUnderlying().toString());
		// }
		// });
		//
		// ec.openTaps();
		//
		// Thread.sleep(10000);
	}

}
