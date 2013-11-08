package ch.cern.cms.load.taps.flashlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ch.cern.cms.load.Load;
import ch.cern.cms.load.Settings;

import com.espertech.esper.client.time.CurrentTimeSpanEvent;

public class OfflineFlashlistEventsTap extends AbstractFlashlistEventsTap {

	public static final int QUEUE_CAPACITY = 1;

	public static final String SETTINGS_KEY_FLASHLIST_DIR = "offlineFlashlistDir";

	private double pace = 1;
	private File[] rootFolders;
	private long timerStart = (Load.getInstance().getSettings().containsKey("timerStart") ? Long.parseLong(Load.getInstance().getSettings()
			.getProperty("timerStart")) : 1);
	private static final Logger logger = Logger.getLogger(OfflineFlashlistEventsTap.class);
	boolean speedCam = (Load.getInstance().getSettings().containsKey("speedCam") && Boolean.parseBoolean(Load.getInstance().getSettings()
			.getProperty("speedCam")));
	boolean detachedTimeSender = (Load.getInstance().getSettings().containsKey("detachedTimeSender") && Boolean.parseBoolean(Load.getInstance().getSettings()
			.getProperty("detachedTimeSender")));

	public OfflineFlashlistEventsTap(Load expert, String path) {
		super(expert, path);
		this.pace = (expert.getSettings().containsKey(Settings.KEY_TIMER_PACE) ? Double.parseDouble(expert.getSettings().getProperty(Settings.KEY_TIMER_PACE))
				: 1);
		job = new Runnable() {
			@Override
			public void run() {
				try {
					TreeMap<Long, List<File>> dumpFiles = new TreeMap<Long, List<File>>();
					for (File rootFolder : rootFolders) {
						for (File d : rootFolder.listFiles()) {
							if (d.isDirectory()) {
								for (File f : d.listFiles()) {
									try {
										Long timestamp = Long.parseLong(f.getName());
										if (!dumpFiles.containsKey(timestamp)) {
											dumpFiles.put(timestamp, new LinkedList<File>());
										}
										dumpFiles.get(timestamp).add(f);
									} catch (Exception e) {
										logger.warn("Filename: " + f.getName() + " not a number. Ignoring.");
									}
								}
							}
						}
					}
					Long lastTime = null;
					logger.info("Offline tap ready. buckets: " + dumpFiles.size() + ", detachedTimeSender: " + detachedTimeSender + ", startPosition: "
							+ timerStart + ", pace: " + pace);
					// sleep to align the starting point with other offline taps

					long lastDelivery = 0l;
					long deliveryStart = 0;

					final CurrentTimeSpanEvent ctse = new CurrentTimeSpanEvent(0);
					Runnable sender = new Runnable() {
						private long lastTime = 0l;

						@Override
						public void run() {
							while (true) {
								synchronized (ctse) {
									long newTime = ctse.getTargetTimeInMillis();
									if (newTime == lastTime) {
										try {
											ctse.wait();
										} catch (InterruptedException e) {
											logger.warn("time sender interrupted");
										}
									}
								}
								ep.sendEvent(ctse);
							}
						}
					};

					if (detachedTimeSender) {
						new Thread(sender).start();
					}

					for (Long time : dumpFiles.keySet()) {
						long loopStart = System.currentTimeMillis();
						if (timerStart > time) {
							logger.info("Skipping time position <" + time + "> to reach <" + timerStart + ">");
							continue;
						}
						long timeSendStart = System.currentTimeMillis();
						if (detachedTimeSender) {
							synchronized (ctse) {
								ctse.setTargetTimeInMillis(time);
								ctse.notify();
							}
						} else {
							ctse.setTargetTimeInMillis(time);
							ep.getRuntime().sendEvent(ctse);
						}
						long timeSendEnd = System.currentTimeMillis();
						if (lastTime != null) {
							double sleepTime = (time - lastTime) / pace - lastDelivery;
							if (sleepTime > 1) {
								try {
									// logger.info("sleeping for " + sleepTime + " msec. time: " + time + ", last: " + lastTime + ", pace: "
									// + pace);
									Thread.sleep(Math.round(sleepTime));
								} catch (InterruptedException e) {
									System.err.println("can't sleep");
								}
							}
						}
						deliveryStart = System.currentTimeMillis();

						long sendSum = 0l;
						long loadSum = 0l;

						for (File f : dumpFiles.get(time)) {
							long flstart = System.currentTimeMillis();
							Flashlist fl = new Flashlist(new URL("file://" + f.getAbsolutePath()), f.getParentFile().getName());
							long flmid = System.currentTimeMillis();
							fl.emit(ep);
							long flend = System.currentTimeMillis();
							sendSum += (flend - flmid);
							loadSum += (flmid - flstart);
						}
						// logger.info(String.format("times: loop: %d, load: %d, send: %d, setTime: %d", System.currentTimeMillis() -
						// loopStart, loadSum, sendSum,
						// timeSendEnd - timeSendStart));
						lastTime = time;
						lastDelivery = System.currentTimeMillis() - deliveryStart;
					}
					logger.info("The offline tap is done sending events");
				} catch (Exception e) {
					logger.error("Damn!", e);
					throw new RuntimeException("things went wrong", e);
				}
			}
		};
	}

	@Override
	public void registerEventTypes() {
		try {
			BufferedReader br = null;
			for (File rootFolder : rootFolders) {
				for (File d : rootFolder.listFiles()) {
					if (d.isDirectory()) {
						for (File f : d.listFiles()) {
							Map<String, Object> types = new HashMap<String, Object>();
							br = new BufferedReader(new FileReader(f));
							for (String field : br.readLine().split(",")) {
								types.put(field, Load.getInstance().getResolver().getFieldType(field, d.getName()));
								logger.info("event: " + d.getName() + ", field: " + field);
							}
							logger.info("registering event type: " + d.getName());
							ep.getAdministrator().getConfiguration().addEventType(d.getName(), types);
							break;
						}
					}
				}
			}
		} catch (IOException e) {
			logger.fatal("IOException while registering event types", e);
			throw new RuntimeException("IOException while registering event types", e);
		}
	}

	public synchronized void setPosition(long position) {
		this.timerStart = position;
	}

	@Override
	public void preRegistrationSetup() {
		try {
			Collection<String> dirPaths = this.controller.getSettings().getMany(OfflineFlashlistEventsTap.SETTINGS_KEY_FLASHLIST_DIR);
			this.rootFolders = new File[dirPaths.size()];
			int i = 0;
			for (String dirPath : dirPaths) {
				rootFolders[i++] = new File(dirPath);
			}
		} catch (Exception e) {
			throw new RuntimeException("Path issue", e);
		}
	}
}

class FlashlistWrapper {
	private Flashlist fl;

	public Flashlist getFl() {
		return fl;
	}

	public void setFl(Flashlist fl) {
		this.fl = fl;
	}

}
