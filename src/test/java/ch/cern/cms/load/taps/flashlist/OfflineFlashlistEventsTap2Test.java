package ch.cern.cms.load.taps.flashlist;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.Load;

public class OfflineFlashlistEventsTap2Test {

	private Load app = null;
	private OfflineFlashlistEventsTap2 tap = null;

	@Before
	public void setUp() throws Exception {
		app = Load.getInstance();
		
		app.registerTap(new OfflineFlashlistEventsTap2(app));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		System.out.println("All fine...");
	}

}
