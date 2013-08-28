package ch.cern.cms.load.dataProviders;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Date;

import notificationService.NotificationEvent;
import notificationService.NotificationServiceSoapBindingStub;
import ch.cern.cms.load.configuration.Settings;

/**
 * @author Tomasz Bawej This class, as opposed to {@link LevelZeroDataProvider}
 *         is intended to receive notifications from lzFM
 */
public class LevelZeroNotificationReceiver {
	private static LevelZeroNotificationReceiver instance = null;

	public static LevelZeroNotificationReceiver getInstance() {
		if (instance == null) {
			synchronized (LevelZeroNotificationReceiver.class) {
				if (instance == null) {
					instance = new LevelZeroNotificationReceiver();
				}
			}
		}
		return instance;
	}

	private LevelZeroNotificationReceiver() {
		try {
			stub = new NotificationServiceSoapBindingStub(new URL(settings.getEndpoint()), null);
		} catch (Throwable e) {
			throw new RuntimeException("There should be a recovery mechanism for that in place, but well...", e);
		}
	}

	/** SINGLETON DONE **/

	Settings settings = Settings.getInstance();
	NotificationServiceSoapBindingStub stub = null;

	public NotificationEvent subscribeForNotification() throws RemoteException {
		return stub.subscribe(new Date().getTime(), settings.getIdForNotificationsRetrieval());
	}
}
