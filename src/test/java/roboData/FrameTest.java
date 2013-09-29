package roboData;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;

public class FrameTest {

	public static final String FRAME = RoboFrame.class.getName();
	private static String skidCondition = "(transVel>=0 and (transVel>(lWheelVel+rWheelVel)*0.751 or transVel<(lWheelVel+rWheelVel)*0.249)) or (transVel<=0 and (transVel>0.249*(lWheelVel+rWheelVel) or transVel<0.751*(lWheelVel+rWheelVel)))";

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
	public void test() throws FileNotFoundException {
		EventProcessor ep = ExpertController.getInstance().getEventProcessor();
		ep.getConfiguration().addEventType(RoboFrame.class);
		ep.registerStatement("select count(*) from " + FRAME, new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				// System.out.println(newEvents[0].getUnderlying());
			}
		});

		/** alarm when robot is not reporting for 2 sec **/
		ep.registerStatement("select * from pattern [every " + FRAME + "-> (timer:interval(2 sec) and not " + FRAME + ")]",
				new PrintingListener("Connection lost?!"));

		ep.registerStatement("select * from pattern [every (timer:interval(100 msec) and "+FRAME+"(transVel>"+FRAME+"(lWheelVel)))]", new PrintingListener("pattern triggered, %b"));
		/**
		 * skidding alert: linear velocity deviates significantly from the
		 * average of left and right wheel, condition persists for at least 0.1
		 * s
		 **/
		ep.registerStatement("select transVel, lWheelVel as lVel, rWheelVel as rVel from " + FRAME + " where " + skidCondition,
				new PrintingListener("skidding? %b"));

		RoboFrame.fromFile("dmp/roboMove.data", ep);
	}

	public static final class PrintingListener implements UpdateListener {
		private final String msg;

		public PrintingListener(String msg) {
			super();
			this.msg = msg;
		}

		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			System.out.println(msg.replace("%b", newEvents[0].getUnderlying().toString()));
		}
	}
}
