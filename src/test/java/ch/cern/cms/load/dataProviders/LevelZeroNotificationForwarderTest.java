package ch.cern.cms.load.dataProviders;

import java.rmi.RemoteException;

import notificationService.NotificationEvent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.model.LevelZeroNotificationForwarder;
import ch.cern.cms.load.model.NotificationSubscriber;

public class LevelZeroNotificationForwarderTest {

	private static NotificationSubscriber reader = new NotificationSubscriber() {
		@Override
		public void processNotification(NotificationEvent ne) {
			System.out.println("Notification received");
		}

	};

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
	public void test() throws RemoteException, InterruptedException {
		int tries = 10;
		int sleepBase = 10000;
		for (int i = 0; i < tries; ++i) {
			LevelZeroNotificationForwarder.subscribe(reader);
			long sleeptime = (long) (Math.random() * sleepBase);
			System.out.println("Subscribed, sleeping for " + sleeptime + " ms");
			Thread.sleep(sleeptime);
			LevelZeroNotificationForwarder.unsubscribe(reader);
			sleeptime = (long) (Math.random() * sleepBase);
			System.out.println("Unsubscribed, sleeping for " + sleeptime + " ms");
			Thread.sleep(sleeptime);
		}
	}
}
