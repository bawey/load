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

import ch.cern.cms.load.eventProcessing.EventProcessor;

public class OfflineFlashlistEventsTap extends AbstractFlashlistEventsTap {

	private File rootFolder;
	private double pace = 1;

	public OfflineFlashlistEventsTap(String path) {
		try {
			rootFolder = new File(path);
		} catch (Exception e) {
			throw new RuntimeException("Path issue", e);
		}
	}

	@Override
	public void registerEventTypes(EventProcessor ep) {
		try {
			BufferedReader br = null;
			for (File d : rootFolder.listFiles()) {
				if (d.isDirectory()) {
					for (File f : d.listFiles()) {
						Map<String, Object> types = new HashMap<String, Object>();
						br = new BufferedReader(new FileReader(f));
						for (String field : br.readLine().split(",")) {
							types.put(field, resolver.getFieldType(field, d.getName()));
						}
						ep.getAdministrator().getConfiguration().addEventType(d.getName(), types);
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("IOException while registering event types", e);
		}
	}

	@Override
	public void openStreams(EventProcessor eps) {
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
							System.err.println("Apparently this is not a number:, " + f.getName());
						}
					}
				}
			}
			System.out.println("Files ready for playback in " + dumpFiles.size() + " buckets");
			Long lastTime = null;
			for (Long time : dumpFiles.keySet()) {
				if (lastTime != null) {
					try {
						Thread.sleep((long) ((time - lastTime) / getPace()));
					} catch (InterruptedException e) {
						System.err.println("can't sleep");
					}
				}
				for (File f : dumpFiles.get(time)) {
					Flashlist fl = new Flashlist(new URL("file://" + f.getAbsolutePath()), f.getParentFile().getName());
					fl.emit(eps);
				}
				lastTime = time;
			}
		} catch (Exception e) {
			throw new RuntimeException("things went wrong", e);
		}
	}

	public synchronized double getPace() {
		return pace;
	}

	public synchronized void setPace(double pace) {
		this.pace = pace;
	}

}
