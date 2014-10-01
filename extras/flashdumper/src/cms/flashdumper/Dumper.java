package cms.flashdumper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Dumper {
	private File rootDir = null;

	Map<String, String> listUrls = new ConcurrentHashMap<String, String>();
	Map<String, List<String>> lastSeen = new ConcurrentHashMap<String, List<String>>();
	private Map<String, BufferedWriter> listWriters = new ConcurrentHashMap<String, BufferedWriter>();
	private Map<String, Integer> fileNumbers = new ConcurrentHashMap<String, Integer>();

	public static final String CATALOG_SUFFIX = "retrieveCatalog?fmt=plain";
	public static final String LIST_SUFFIX = "retrieveCollection?fmt=plain&flash=";

	public static final String[] sources = new String[] { "http://srv-c2d04-19.cms:9941/urn:xdaq-application:lid=400/",
			"http://srv-c2d04-19.cms:9942/urn:xdaq-application:lid=400/", "http://l1ts-gt.cms:5005/urn:xdaq-application:lid=10/" };

	private long max_length = 1024 * 1024 * 256;
	long interval = 1000;

	Properties props = new Properties();

	private Dumper() throws IOException {
		props.load(getClass().getClassLoader().getResourceAsStream("flashdumper.properties"));
		rootDir = new File(props.getProperty("dumpDir", "/stuff/flashlists"));
		if (rootDir.exists() && !rootDir.isDirectory()) {
			throw new RuntimeException("target not a dir");
		} else if (!rootDir.exists()) {
			rootDir.mkdirs();
		}
		System.out.println("Root dir: " + rootDir.getAbsolutePath());
		setUpSOCKSProxy();
	}

	private void setUpSOCKSProxy() {
		for (String key : new String[] { "socksProxyHost", "proxySet", "socksProxyPort" }) {
			if (props.containsKey(key)) {
				System.getProperties().put(key, props.getProperty(key));
				System.out.println(key + ": " + props.getProperty(key));
			}
		}
	}

	public static final void main(String[] args) throws IOException {
		Dumper d = new Dumper();

		for (String root : sources) {

			String catalog = Kit.getHTML(root + CATALOG_SUFFIX);
			String[] lists = catalog.split("\n");
			for (int i = 1; i < lists.length; ++i) {
				d.listUrls.put(lists[i], root + LIST_SUFFIX + lists[i]);
				d.getTargetFile(lists[i]).getParentFile().mkdirs();
			}
		}
		// from now on, just fetch the flashlists repeatedly

		if (d.props.containsKey("multithreaded") && Boolean.parseBoolean(d.props.getProperty("multithreaded"))) {
			System.out.println("Using " + d.listUrls.size() + " threads");
			for (String listName : d.listUrls.keySet()) {
				new Thread(new DumpRunnable(listName, d)).start();
			}
		} else {
			System.out.println("Using single thread");
			int totalLines = 0;
			int totalWritten = 0;
			while (true) {
				long fetchingTime = 0l;
				long startTime = System.currentTimeMillis();
				for (String title : d.listUrls.keySet()) {
					// make sure there is a list on the other side
					if (!d.lastSeen.keySet().contains(title)) {
						d.lastSeen.put(title, new LinkedList<String>());
					}
					// get the flashlist, split to list of strings
					long fetchStart = System.currentTimeMillis();
					List<String> lines = Kit.getHTMLAsLines(d.listUrls.get(title));
					fetchingTime += (System.currentTimeMillis() - fetchStart);
					long receptionTime = System.currentTimeMillis();
					BufferedWriter bw = d.getWriter(title);
					// write to file all he new lines
					int written = 0;
					for (String line : lines) {
						if (!d.lastSeen.get(title).contains(line)) {
							bw.write(receptionTime + ": " + line + "\n");
							++totalWritten;
						}
					}
					totalLines += lines.size();
					bw.flush();
					// save current as last
					d.getLastSeenLines(title).clear();
					d.getLastSeenLines(title).addAll(lines);
				}
				long loopTime = System.currentTimeMillis() - startTime;
				long sleepTime = d.interval - loopTime;
				System.out.println("Written so far: " + totalWritten + "/" + totalLines + ". Loop time: " + loopTime + ", sleeptime: " + sleepTime
						+ ", fetchTime: " + fetchingTime);

				if (sleepTime > 0) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}

	List<String> getLastSeenLines(String listName) {
		if (!this.lastSeen.containsKey(listName)) {
			this.lastSeen.put(listName, new LinkedList<String>());
		}
		return this.lastSeen.get(listName);
	}

	BufferedWriter getWriter(String listName) throws IOException {
		if (!this.listWriters.keySet().contains(listName)) {
			listWriters.put(listName, new BufferedWriter(new FileWriter(getTargetFile(listName))));
		} else if (getTargetFile(listName).length() > max_length) {
			this.fileNumbers.put(listName, this.fileNumbers.get(listName) + 1);
			this.listWriters.put(listName, new BufferedWriter(new FileWriter(getTargetFile(listName))));
		}
		return this.listWriters.get(listName);
	}

	private int getFileNumber(String listName) {
		if (!this.fileNumbers.containsKey(listName)) {
			this.fileNumbers.put(listName, 0);
		}
		return this.fileNumbers.get(listName);
	}

	private File getTargetFile(String listName) {
		return new File(rootDir.getAbsolutePath() + "/" + listName + "/" + getFileNumber(listName));
	}

	private static void debug(String s) {
		System.out.println(s);
	}

}
