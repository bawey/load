package ch.cern.cms.load.views;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import ch.cern.cms.load.FieldTypeResolver;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.LoadView;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;

public class FileSink extends LoadView {

	public static final String KEY_OUTPUT_DIRECTORY = "outputDir";

	private final static Logger logger = Logger.getLogger(FileSink.class);

	private StatementAwareUpdateListener dummy = new StatementAwareUpdateListener() {
		@Override
		public void update(EventBean[] arg0, EventBean[] arg1, EPStatement arg2, EPServiceProvider arg3) {

		}
	};

	private StatementAwareUpdateListener recipient = new StatementAwareUpdateListener() {

		@Override
		public void update(EventBean[] arg0, EventBean[] arg1, EPStatement arg2, EPServiceProvider arg3) {
			try {
				queue.put(new UpdateEnvelope(arg0, arg1, arg2, arg3));
			} catch (InterruptedException e) {
				System.err.println("Thread interrupted, details logged.");
				logger.warn(e);
			}
		}
	};

	private BlockingQueue<UpdateEnvelope> queue = new ArrayBlockingQueue<LoadView.UpdateEnvelope>(1000);

	private Runnable worker = new Runnable() {

		@Override
		public void run() {
			Map<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
			try {
				File output = new File(outDir, FieldTypeResolver.dateFormat.format(new Date()));
				output.mkdir();
				System.out.println("saving to folder: " + output.getAbsolutePath());
				while (true) {
					try {
						UpdateEnvelope e = queue.take();

						VerboseAttributes va = new VerboseAttributes(e.statement);
						if (!writers.containsKey(va.label)) {
							writers.put(va.label, new BufferedWriter(new FileWriter(new File(output, va.label))));
						}
						BufferedWriter writer = writers.get(va.label);
						writer.write(getPrintableUpdate(e, null).append("\n").toString());
						writer.flush();

					} catch (InterruptedException e) {
						System.err.println("Thread interrupted, details logged");
						logger.warn(e);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			} finally {
				for (BufferedWriter bw : writers.values()) {
					if (bw != null) {
						try {
							bw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	};

	private File outDir;

	public FileSink() {
		String outputPath = Load.getInstance().getSettings().getProperty(KEY_OUTPUT_DIRECTORY);
		outDir = new File(outputPath);
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		new Thread(worker, "Load FileSink worker").start();
	}

	@Override
	public StatementAwareUpdateListener getVerboseStatementListener() {
		return recipient;
	}

	@Override
	public StatementAwareUpdateListener getWatchedStatementListener() {
		return dummy;
	}

}
