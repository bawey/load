package ch.cern.cms.load.taps.flashlist;

import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class MongoDBTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		MongoClient mongoClient = new MongoClient("localhost");
		DB db = mongoClient.getDB("flashlists");
		System.out.println(db.getName());

		DBCollection fakeList = db.createCollection("fakeList", null);

		DBCollection fakeStamp = db.createCollection("fakeStamp", null);

		fakeStamp.ensureIndex(new BasicDBObject("_id", 1), "fakestamp_index", true);

		int i = 10000000;

		long stime = System.currentTimeMillis();
		while (--i > 0) {
			int fakeId = (int) (Math.random() * 5000000);
			// fakeStamps.insert(new BasicDBObject().append("_id", fakeId));
			fakeList.insert(new BasicDBObject().append("time", new Date().toString()).append("stamp", fakeId));
			fakeStamp.update(new BasicDBObject().append("_id", fakeId), new BasicDBObject().append("_id", fakeId), true, false);
		}
		System.out.println("took: " + (System.currentTimeMillis() - stime));
	}

}
