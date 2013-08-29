package ch.cern.cms.load.model;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import notificationService.NotificationEvent;
import parameterService.ParameterBean;

public class Recorder implements NotificationSubscriber {

	private long timeout;
	private List<Sample> samples = new ArrayList<Sample>((int) (timeout / 20));

	public Recorder(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public void processNotification(NotificationEvent ne) {
		Sample s = new Sample();
		s.ne = ne;
		synchronized (samples) {
			s.timestamp = new Date().getTime();
			samples.add(s);
		}
	}

	public static class Sample implements Serializable {
		private static final long serialVersionUID = 1L;
		protected long timestamp;
		protected ParameterBean[] beans;
		protected NotificationEvent ne;
	}

	public void run() throws MalformedURLException, RemoteException, InterruptedException {
		long endTime = new Date().getTime() + timeout;
		Sample dump = new Sample();
		dump.beans = LevelZeroDataProvider.getInstance().getRawData();
		synchronized (samples) {
			dump.timestamp = new Date().getTime();
			samples.add(dump);
		}
		while (new Date().getTime() < endTime) {
			Thread.sleep((long) Math.min(2500, new Date().getTime() - endTime));
			System.out.println("working, sleeping, eating");
		}
		synchronized (samples) {
			// write to file, goodbye
		}
	}
}
