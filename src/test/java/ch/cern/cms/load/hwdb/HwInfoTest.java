package ch.cern.cms.load.hwdb;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rcms.common.db.DBConnectorException;

public class HwInfoTest {

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
	public void test() throws DBConnectorException {
		String ctx = "http://fmmpc-s1d12-08.cms:11100";
		int gslot = 7;
		HwInfo nn = HwInfo.getInstance();
		for (Integer i = 0; i < 20; ++i) {
			System.out.println("connected FED: " + nn.getFedId(ctx, gslot, i.toString(), CmsHw.FMM));
			System.out.println("source FMM: " + nn.getSrcFMM(nn.getFMM(ctx, gslot), i));
			System.out.println(nn.getDeadtimeRelevantFedIds("http://fmmpc-s1d12-08.cms:11100", 7, i));
		}
		//
	}

}
