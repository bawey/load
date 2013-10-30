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

import org.apache.log4j.Logger;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerEvent;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;

public class OfflineFlashlistEventsTap extends AbstractFlashlistEventsTap {

	public static final String SETTINGS_KEY_FLASHLIST_DIR = "offlineFlashlistDir";

	private File rootFolder;
	private double pace = 100;
	private long fastForward = 0;
	private static final Logger logger = Logger.getLogger(OfflineFlashlistEventsTap.class);
	private static long startPoint = Long.MAX_VALUE;
	private static int totalInstances = 0;
	private static int readyInstances = 0;

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

					synchronized (OfflineFlashlistEventsTap.class) {
						++readyInstances;
					}

					while (readyInstances != totalInstances) {
						logger.info(this.hashCode() + " waiting for all instances to be ready");
						Thread.sleep(100);
					}

					logger.info("ready instances: " + readyInstances);

					if (ownStart > startPoint) {
						Thread.sleep(ownStart - startPoint);
					}
					for (Long time : dumpFiles.keySet()) {
						if (fastForward > 0) {
							if (lastSkipped != 0) {
								fastForward -= (time - lastSkipped);
							}
							lastSkipped = time;
							continue;
						}
						if (lastTime != null) {
							try {
								Thread.sleep((long) ((time - lastTime) / getPace()));
							} catch (InterruptedException e) {
								System.err.println("can't sleep");
							}
						}

						//ep.getProvider().getEPRuntime().sendEvent(new CurrentTimeEvent(time));
						for (File f : dumpFiles.get(time)) {
							Flashlist fl = new Flashlist(new URL("file://" + f.getAbsolutePath()), f.getParentFile().getName());
							logger.info("Sending event: " + f.getParentFile().getName());
							fl.emit(ep);
						}
						lastTime = time;
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

	public synchronized double getPace() {
		return pace;
	}

	public synchronized void setPace(double pace) {
		this.pace = pace;
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
