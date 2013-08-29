package ch.cern.cms.load.model;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ModelTest {

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
		Map<String, Object> data = Model.getInstance().getData();
		while (true) {
			long sleeptime = (long) (1000 * Math.random());
			Thread.sleep(sleeptime);
			if (data.get("DAQ_CC_PARAM_MAP") != null) {
				System.out.println("DAQ_CC_PARAM_MAP size " + ((Map) data.get("DAQ_CC_PARAM_MAP")).size());
			}
		}

	}

}
