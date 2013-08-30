package ch.cern.cms.load.trialAndError;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileWriteTest {

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
	public void test() throws IOException, ClassNotFoundException {
		File file = new File("testFile");
		List<Object> a = new LinkedList<Object>();
		a.add("A test string");
		a.add(167);
		a.add(new Date());
		FileWriter writer = new FileWriter(file);

		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

		ObjectOutputStream ous = new ObjectOutputStream(bos);
		ous.writeObject(a);
		ous.close();

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		List<Object> readout = (List<Object>) ois.readObject();
		for (Object o : readout) {
			System.out.println(o.toString());
		}

	}
}
