package ch.cern.cms.load.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.cern.cms.load.eventProcessing.EventProcessor;

public class FleischListProvider {
	private File rootFolder;

	public FleischListProvider(String rootFolderPath) {
		this.rootFolder = new File(rootFolderPath);
		if (!rootFolder.exists() || !rootFolder.isDirectory()) {
			throw new RuntimeException("root folder doesn't exists");
		}
	}

	public void registerEventTypes(EventProcessor ep) throws IOException {
		BufferedReader br = null;
		for (File d : rootFolder.listFiles()) {
			if (d.isDirectory()) {
				for (File f : d.listFiles()) {
					Map<String, Object> types = new HashMap<String, Object>();
					br = new BufferedReader(new FileReader(f));
					for(String field : br.readLine().split(",")){
						types.put(field, String.class);
					}
					System.out.println("registering event "+d.getName()+" as "+types.toString());
					ep.getAdministrator().getConfiguration().addEventType(d.getName(), types);
					break;
				}
			}
		}
	}

	public void emit(EventProcessor ep, double pace) {
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
					Thread.sleep((long) ((time - lastTime) / pace));
				} catch (InterruptedException e) {
					System.err.println("can't sleep");
				}
				for (File f : dumpFiles.get(time)) {
					FleischList fl = new FleischList(f);
					fl.emit(ep);
				}
			}
			lastTime = time;
		}
	}
}
