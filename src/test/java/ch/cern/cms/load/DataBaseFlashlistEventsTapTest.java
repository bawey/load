package ch.cern.cms.load;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.taps.flashlist.DataBaseFlashlistEventsTap;

public class DataBaseFlashlistEventsTapTest {

	private Load load = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		load = Load.getInstance();
		DataBaseFlashlistEventsTap tap = new DataBaseFlashlistEventsTap(load);
		load.openTaps();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
	}

}
