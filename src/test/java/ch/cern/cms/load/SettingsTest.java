package ch.cern.cms.load;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.taps.flashlist.OfflineFlashlistEventsTap;

public class SettingsTest {

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
		ExpertController e = ExpertController.getInstance();
		for (String s : e.getSettings().getMany(OfflineFlashlistEventsTap.SETTINGS_KEY_FLASHLIST_DIR)) {
			System.out.println(s);
		}
	}

}
