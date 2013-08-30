package ch.cern.cms.load.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import parameterService.ParameterBean;
import ch.cern.cms.load.model.Recorder.Sample;

public class RecorderTest {

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
	public void test() throws FileNotFoundException, InterruptedException, IOException, ClassNotFoundException {
		Recorder r = new Recorder(300000);
		File data = r.run();

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(data));
		List<Sample> samples = (List<Sample>) ois.readObject();

		for (Sample sample : samples) {
			System.out.print(new Date(sample.timestamp) + ": "
					+ (sample.beans != null ? "big pack" : (sample.ne != null ? "notification" : "null!")));
			if (sample.ne != null) {
				System.out.println(" >> " + sample.ne.getContent());
			} else if (sample.beans != null) {
				System.out.print(" >> [" + sample.beans.length + "] ");
				for (ParameterBean bean : sample.beans) {
					System.out.print(bean.getName());
				}
				System.out.println(".");
			}
		}
	}

}
