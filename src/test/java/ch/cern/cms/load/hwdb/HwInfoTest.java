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
		HwInfo nn = HwInfo.getInstance();
		System.out.println(nn.getFedForFrl("http://frlpc-s2d10-14.cms:11100", 1, 0));
		// {geoslot=7, title=deadtime, kontext=http://fmmpc-s1d12-08.cms:11100, io=0}
		for (Integer i = 0; i < 20; ++i) {
			System.out.println(nn.getFedId("http://fmmpc-s1d12-08.cms:11100", "7", i.toString(), CmsHw.FMM, "testing"));
			System.out.println(nn.getDeadtimeRelevantFedIds("http://fmmpc-s1d12-08.cms:11100", 7, i));
		}
		//
	}

}
