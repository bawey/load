package ch.cern.cms.load.taps.flashlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.collections.list.SynchronizedList;
import org.apache.log4j.Logger;

import ch.cern.cms.load.Load;

public class OfflineFlashlistEventsTap extends AbstractFlashlistEventsTap {

	public static final int QUEUE_CAPACITY = 1000;

	public static final String SETTINGS_KEY_FLASHLIST_DIR = "offlineFlashlistDir";

	private File rootFolder;
	private long fastForward = 0;
	private static final Logger logger = Logger.getLogger(OfflineFlashlistEventsTap.class);
	private static long startPoint = Long.MAX_VALUE;
	private static int totalInstances = 0;
	private static int readyInstances = 0;
	private static final Object equalStartLock = new Object();
	boolean speedCam = (Load.getInstance().getSettings().containsKey("speedCam") && Boolean.parseBoolean(Load.getInstance().getSettings()
			.getProperty("speedCam")));

	private static synchronized void submitLocalStartPoint(long sp) {
		startPoint = Math.min(sp, startPoint);
	}

	public OfflineFlashlistEventsTap(Load expert, String path) {
		super(expert, path);
		synchronized (OfflineFlashlistEventsTap.class) {
			++totalInstances;
		}
		job = new Runnable() {
			@Override
			public void run() {
				try {
					TreeMap<Long, List<File>> dumpFiles = new TreeMap<Long, List<File>>();
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
					logger.info("Files ready for playback in " + dumpFiles.size() + " buckets");
					Long lastTime = null;
					long lastSkipped = 0;
					logger.info("This tap is ready to start sending the events: " + rootFolder.getAbsolutePath());
					// sleep to align the starting point with other offline taps
					long ownStart = dumpFiles.keySet().iterator().next();
					submitLocalStartPoint(ownStart);

					synchronized (equalStartLock) {
						++readyInstances;
						if (readyInstances != totalInstances) {
							logger.info(this.hashCode() + " waiting for all instances to be ready");
							equalStartLock.wait();
						} else {
							equalStartLock.notifyAll();
						}
					}

					logger.info("ready instances: " + readyInstances);

					if (ownStart > startPoint) {
						Thread.sleep((long) ((ownStart - startPoint) / Load.getInstance().getPace()));
					}

					long lastDelivery = 0l;
					long deliveryStart = 0;

					final ArrayBlockingQueue<Flashlist> queue = new ArrayBlockingQueue<Flashlist>(QUEUE_CAPACITY);

					Thread flashlistDispatcher = new Thread(new Runnable() {
						private Flashlist fl = null;

						@Override
						public void run() {
							while (true) {
								try {
									fl = queue.take();
									fl.emit(ep);
								} catch (InterruptedException e) {
									logger.error("Flashlist dispatcher interrupted!", e);
								}
							}
						}
					});

					flashlistDispatcher.start();

					for (Long time : dumpFiles.keySet()) {
						if (fastForward > 0) {
							if (lastSkipped != 0) {
								fastForward -= (time - lastSkipped);
							}
							lastSkipped = time;
							continue;
						}
						if (lastTime != null) {
							double sleepTime = (time - lastTime) / Load.getInstance().getPace() - lastDelivery;
							if (sleepTime > 0) {
								try {
									Thread.sleep((long) sleepTime);
								} catch (InterruptedException e) {
									System.err.println("can't sleep");
								}
							} else if (speedCam && sleepTime < 0) {
								logger.warn("Negative (" + sleepTime
										+ ") for OfflineEventsTap. Consider lowering the pace or reimplementing the playback facility.");
							}
						}
						deliveryStart = System.currentTimeMillis();

						// ep.getProvider().getEPRuntime().sendEvent(new CurrentTimeEvent(time));
						for (File f : dumpFiles.get(time)) {
							Flashlist fl = new Flashlist(new URL("file://" + f.getAbsolutePath()), f.getParentFile().getName());
							queue.put(fl);
							if (queue.remainingCapacity() < (int) (0.1 * QUEUE_CAPACITY)) {
								logger.warn("Flashlists queue 90% full");
							}
						}
						lastTime = time;
						lastDelivery = System.currentTimeMillis() - deliveryStart;
					}
					logger.info("This tap is done sending events: " + rootFolder.getAbsolutePath());
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
		} catch (IOException e) {
			logger.fatal("IOException while registering event types", e);
			throw new RuntimeException("IOException while registering event types", e);
		}
	}

	public synchronized void setPosition(long position) {
		this.fastForward = position;
	}

	@Override
	public void preRegistrationSetup() {
		try {
			rootFolder = new File(path);
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
