package ch.cern.cms.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.load.taps.flashlist.DataBaseFlashlistEventsTap;
import ch.cern.cms.load.taps.flashlist.NovemberFlashlistToolkit;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

@Deprecated
public class MongoDumper {

	private final static Logger logger = Logger.getLogger(MongoDumper.class);

	private MongoClient mongoClient;
	private DB db;
	private List<File> rootDirs = new LinkedList<File>();
	private Map<String, String[]> columns = new HashMap<String, String[]>();
	private DBCollection fetchstamps;

	private MongoDumper() throws UnknownHostException {
		mongoClient = new MongoClient(Load.getInstance().getSettings().getProperty(DataBaseFlashlistEventsTap.KEY_DB_HOST));

		db = mongoClient.getDB(Load.getInstance().getSettings().getProperty(DataBaseFlashlistEventsTap.KEY_DB_NAME));
		for (String path : Load.getInstance().getSettings().getMany("flashlistForDbDir")) {
			rootDirs.add(new File(path));
		}
		fetchstamps = db.createCollection("fetchstamp", null);
		// fetchstamps.ensureIndex(new BasicDBObject("_id", 1), "fetchstamp_index", true);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MongoDumper md = new MongoDumper();

			for (File dir : md.rootDirs) {
				for (File subdir : dir.listFiles(MysqlDumper.FF_DIRECTORY)) {
					String eventName = MysqlDumper.dirToEventName(subdir.getName());
					System.out.println("DIR: " + eventName);
					md.readStructure(subdir);
				}
			}

			for (File dir : md.rootDirs) {
				for (File subdir : dir.listFiles(MysqlDumper.FF_DIRECTORY)) {
					System.out.println("In the " + subdir.getAbsolutePath() + " dir");
					for (File dump : subdir.listFiles()) {
						md.pushIntoDb(dump, MysqlDumper.dirToEventName(subdir.getName()));
					}
				}
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void pushIntoDb(File src, String eventName) throws Exception {
		DBCollection collection = db.createCollection(eventName, null);

		BufferedReader br = new BufferedReader(new FileReader(src));

		String[] tokens = null;
		String[] labels = columns.get(eventName);
		while ((tokens = NovemberFlashlistToolkit.getNextValidRow(br, null)) != null) {
			long time = Long.parseLong(tokens[0]);
			try {
				if (tokens.length - columns.get(eventName).length != 1) {
					for (int i = 1; i < tokens.length; ++i) {
						logger.error(columns.get(eventName)[Math.min(i, columns.get(eventName).length - 1)] + ": "
								+ tokens[i].substring(0, Math.min(100, tokens[i].length())));
					}
					throw new RuntimeException("Got more fields than columns " + eventName + " than expected");
				}

				BasicDBObject dbo = new BasicDBObject("fetchstamp", time);
				for (int i = 1; i < tokens.length; ++i) {
					dbo.append(labels[i - 1], tokens[i]);
				}

				collection.insert(dbo);
				fetchstamps.insert(new BasicDBObject().append("fetchstamp", time));

			} catch (Exception th) {
				logger.warn("Something went wrong when parsing " + eventName, th);
			}
		}
		br.close();
	}

	// private public void t() {
	//
	// DBCollection fakeList = db.createCollection("fakeList", null);
	//
	// int i = 10000000;
	//
	// long stime = System.currentTimeMillis();
	// while (--i > 0) {
	// int fakeId = (int) (Math.random() * 5000000);
	// // fakeStamps.insert(new BasicDBObject().append("_id", fakeId));
	// fakeList.insert(new BasicDBObject().append("time", new Date().toString()).append("stamp", fakeId));
	// fakeStamp.update(new BasicDBObject().append("_id", fakeId), new BasicDBObject().append("_id", fakeId), true, false);
	// }
	// System.out.println("took: " + (System.currentTimeMillis() - stime));
	//
	// }

	private void readStructure(File dir) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(dir.listFiles(MysqlDumper.FF_0)[0]));
		String eventName = MysqlDumper.dirToEventName(dir.getName());
		columns.put(eventName, NovemberFlashlistToolkit.timeAndLine(br.readLine())[1].split(","));
		br.close();
	}

}
