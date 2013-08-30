package ch.cern.cms.load.dataProviders;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.configuration.Settings;
import ch.cern.cms.load.configuration.Settings.Runmode;
import ch.cern.cms.load.model.Model;

/**
 * Assembles a quick setup for reading data from a file.
 * 
 * @author Tomasz Bawej
 * 
 */

public class OfflineProcessingTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Settings.getInstance().setRunmode(Runmode.OFFLINE);
		Settings.getInstance().setDataSource(new File("dmp/data.bt"));
		Settings.getInstance().setPlaybackRate(10d);
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
		Model model = Model.getInstance();
		Thread.sleep(100000);
	}

}
