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
 * 
 *         LZFM needs to perform the first subscription passing a 0 timestamp.
 *         In return a server-side timestamp is received. Each following
 *         subscription should then be timestamped with InitialTimestamp + (now
 *         - InitialTimestampReceptionTime)
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
			stub = new NotificationServiceSoapBindingStub(new URL(settings.getNotificationEndpoint()), null);
		} catch (Throwable e) {
			throw new RuntimeException("There should be a recovery mechanism for that in place, but well...", e);
		}
	}

	/** SINGLETON DONE **/

	Settings settings = Settings.getInstance();
	NotificationServiceSoapBindingStub stub = null;
	long previousReturnTimestamp = 0;

	public NotificationEvent subscribeForNotification() throws RemoteException {
		NotificationEvent ne = stub.subscribe(previousReturnTimestamp, settings.getIdForNotificationsRetrieval());
		previousReturnTimestamp = ne.getTimestamp();
		return ne;
	}
}
