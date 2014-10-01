package cms.flashdumper;

import java.io.BufferedWriter;
import java.util.LinkedList;
import java.util.List;

public class DumpRunnable implements Runnable {

	private String listname;
	private Dumper d;

	public DumpRunnable(String listname, Dumper d) {
		super();
		this.listname = listname;
		this.d = d;
	}

	@Override
	public void run() {

		int totalLines = 0;
		int totalWritten = 0;
		while (true) {
			long fetchingTime = 0l;
			long startTime = System.currentTimeMillis();
			// make sure there is a list on the other side
			if (!d.lastSeen.keySet().contains(listname)) {
				d.lastSeen.put(listname, new LinkedList<String>());
			}
			// get the flashlist, split to list of strings
			long fetchStart = System.currentTimeMillis();
			List<String> lines = Kit.getHTMLAsLines(d.listUrls.get(listname));
			fetchingTime += (System.currentTimeMillis() - fetchStart);
			long receptionTime = System.currentTimeMillis();
			try {
				BufferedWriter bw = d.getWriter(listname);
				// write to file all he new lines
				int written = 0;
				for (String line : lines) {
					if (!d.lastSeen.get(listname).contains(line)) {
						bw.write(receptionTime + ": " + line + "\n");
						++totalWritten;
					}
				}
				totalLines += lines.size();
				bw.flush();
				// save current as last
				d.getLastSeenLines(listname).clear();
				d.getLastSeenLines(listname).addAll(lines);
				long loopTime = System.currentTimeMillis() - startTime;
				long sleepTime = d.interval - loopTime;
				System.out.println(listname + " wrote so far: " + totalWritten + "/" + totalLines + ". Loop time: " + loopTime
						+ ", sleeptime: " + sleepTime + ", fetchTime: " + fetchingTime);

				if (sleepTime > 0) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(5678);
			}

		}

	}
}
