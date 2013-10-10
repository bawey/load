package ch.cern.cms.load.hwdb;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rcms.common.db.DBConnectorException;

public class NoNameTest {

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
		NoName nn = NoName.getInstance();
		System.out.println(nn.getFedIdForFrl("http://frlpc-s2d10-14.cms:11100", 1, 0));
//		
	}

}
