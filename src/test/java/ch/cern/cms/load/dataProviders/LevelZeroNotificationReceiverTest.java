package ch.cern.cms.load.dataProviders;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import notificationService.NotificationEvent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.utils.Stats;

public class LevelZeroNotificationReceiverTest {

	private static LevelZeroNotificationReceiver lznr = LevelZeroNotificationReceiver.getInstance();

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
	public void test() throws RemoteException {
		int requests = 10000;
		List<Double> rtds = new ArrayList<Double>(requests);
		for (int i = 0; i < requests; ++i) {
			long requestTime = new Date().getTime();
			NotificationEvent ne = lznr.subscribeForNotification();
			// System.out.println(ne.getContent());
			long receptionTime = new Date().getTime();
			rtds.add((receptionTime - requestTime) / 1000d);
			if (i != 0 && i % 100 == 0) {
				System.out.println("Average roundtrip time over " + i + " attempts is " + Stats.mean(rtds) + " seconds ("
						+ Stats.min(rtds) + " - " + Stats.max(rtds) + ").");
			}
		}
		System.out.println("Average roundtrip time over " + requests + " attempts is " + Stats.mean(rtds) + " seconds (" + Stats.min(rtds)
				+ " - " + Stats.max(rtds) + ").");
	}

}
