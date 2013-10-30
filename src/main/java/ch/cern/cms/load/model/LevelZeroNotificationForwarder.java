package ch.cern.cms.load.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import notificationService.NotificationEvent;
import notificationService.NotificationServiceSoapBindingStub;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.Settings;
import ch.cern.cms.load.model.Recorder.Sample;

/**
 * @author Tomasz Bawej This class, as opposed to {@link LevelZeroDataProvider}
 *         is intended to receive notifications from lzFM
 * 
 *         LZFM needs to perform the first subscription passing a 0 timestamp.
 *         In return a server-side timestamp is received. Each following
 *         subscription should then be timestamped with InitialTimestamp + (now
 *         - InitialTimestampReceptionTime)
 */
public class LevelZeroNotificationForwarder {
	private static LevelZeroNotificationForwarder instance = null;

	private static LevelZeroNotificationForwarder getInstance() {
		if (instance == null) {
			synchronized (LevelZeroNotificationForwarder.class) {
				if (instance == null) {
					instance = new LevelZeroNotificationForwarder();
				}
			}
		}
		return instance;
	}

	public static boolean subscribe(NotificationSubscriber subscriber) {
		return getInstance().doSubscribe(subscriber);
	}

	public static boolean unsubscribe(NotificationSubscriber subscriber) {
		return getInstance().doUnsubscribe(subscriber);
	}

	private Runnable jobDescription = new Runnable() {
		@Override
		public void run() {
			switch (settings.getRunmode()) {
			case OFFLINE:
				ObjectInputStream ois;
				try {
					ois = new ObjectInputStream(new FileInputStream(Load.getInstance().getSettings().getDataSource()));
					@SuppressWarnings("unchecked")
					List<Sample> samples = (List<Sample>) ois.readObject();
					long previousTimestamp = 0;
					for (Sample sample : samples) {
						if (sample.ne == null) {
							continue;
						}
						if (previousTimestamp > 0) {
							try {
								long sleeptime = (long) ((sample.timestamp - previousTimestamp) / settings.getPlaybackRate());
								//System.out.println("Sent notification and falling asleep for " + sleeptime + " ms");
								Thread.sleep(sleeptime);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						previousTimestamp = sample.timestamp;
						notifySubscribers(sample.ne);
					}
				} catch (Exception e) {
					throw new RuntimeException("Doomed during playback", e);
				}

				break;
			case ONLINE_RECORD:
				break;

			case ONLINE:
			default:
				while (!subscribers.isEmpty()) {
					try {
						notifySubscribers(pullNotification());
					} catch (RemoteException e) {
						throw new RuntimeException("I panic", e);
					}
				}
				break;
			}
		}
	};

	long previousReturnTimestamp = 0;
	Settings settings = Load.getInstance().getSettings();
	NotificationServiceSoapBindingStub stub = null;
	private Set<NotificationSubscriber> subscribers;
	private Thread worker = new Thread(this.jobDescription);

	private LevelZeroNotificationForwarder() {
		try {
			stub = new NotificationServiceSoapBindingStub(new URL(settings.getNotificationEndpoint()), null);
		} catch (Throwable e) {
			throw new RuntimeException("There should be a recovery mechanism for that in place, but well...", e);
		}
		this.subscribers = new HashSet<NotificationSubscriber>();
		worker.start();
	}

	private boolean doSubscribe(NotificationSubscriber subscriber) {
		synchronized (subscribers) {
			boolean result = subscribers.add(subscriber);
			subscribers.notify();
			return result;
		}
	}

	private boolean doUnsubscribe(NotificationSubscriber subscriber) {
		synchronized (subscribers) {
			return subscribers.remove(subscriber);
		}
	}

	private NotificationEvent pullNotification() throws RemoteException {
		NotificationEvent ne = stub.subscribe(previousReturnTimestamp, settings.getIdForNotificationsRetrieval());
		previousReturnTimestamp = ne.getTimestamp();
		return ne;
	}

	private void notifySubscribers(NotificationEvent ne) {
		synchronized (subscribers) {
			for (NotificationSubscriber subscriber : subscribers) {
				subscriber.processNotification(ne);
			}
			if (subscribers.isEmpty()) {
				try {
					//System.out.println("No subscribers, sleeping");
					subscribers.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
