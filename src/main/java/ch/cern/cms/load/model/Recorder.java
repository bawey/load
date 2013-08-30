package ch.cern.cms.load.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import notificationService.NotificationEvent;
import parameterService.ParameterBean;

public class Recorder implements NotificationSubscriber {

	public static class Sample implements Serializable {
		private static final long serialVersionUID = 1L;
		protected ParameterBean[] beans;
		protected NotificationEvent ne;
		protected long timestamp;
	}

	private List<Sample> samples = null;

	private long timeout;

	public Recorder(long timeout) {
		this.timeout = timeout;
		samples = new ArrayList<Sample>((int) (timeout / 20));
	}

	@Override
	public void processNotification(NotificationEvent ne) {
		System.out.println("Update came: " + ne.getTimestamp());
		Sample s = new Sample();
		s.ne = ne;
		synchronized (samples) {
			s.timestamp = new Date().getTime();
			samples.add(s);
		}
	}

	public File run() throws InterruptedException, FileNotFoundException, IOException {
		File output = null;
		long endTime = new Date().getTime() + timeout;
		Sample dump = new Sample();

		LevelZeroNotificationForwarder.subscribe(this);

		dump.beans = LevelZeroDataProvider.getInstance().getRawData();
		System.out.println("Huge data chunk taken");
		synchronized (samples) {
			dump.timestamp = new Date().getTime();
			samples.add(dump);
		}
		while (new Date().getTime() < endTime) {
			System.out.println(((endTime - (new Date().getTime())) / 1000d) + " seconds left");
			Thread.sleep((long) Math.min(2500, endTime - new Date().getTime()));
			System.out.println("working, sleeping, eating");
		}
		System.out.println("not looping anymore");
		synchronized (samples) {
			Calendar cal = new GregorianCalendar().getInstance();
			output = new File("dmp/"+cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DATE) + "-" + cal.get(Calendar.HOUR) + "."
					+ cal.get(Calendar.MINUTE) + ".b");
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(output));
			oos.writeObject(samples);
			oos.close();
		}
		return output;
	}
}
