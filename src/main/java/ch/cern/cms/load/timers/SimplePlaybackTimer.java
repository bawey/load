package ch.cern.cms.load.timers;

import java.util.Date;

import org.apache.log4j.Logger;

import ch.cern.cms.load.EPTimer;
import ch.cern.cms.load.Load;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.time.CurrentTimeEvent;

public class SimplePlaybackTimer extends EPTimer {

	private final static Logger logger = Logger.getLogger(SimplePlaybackTimer.class);
	private EPRuntime epRuntime = Load.getInstance().getEventProcessor().getRuntime();
	private long currentTime = System.currentTimeMillis();

	public SimplePlaybackTimer() {
		this.timerJob = new Runnable() {
			private long loopStartTime = 0l;
			private long loopTime = 0l;

			@Override
			public void run() {
				currentTime = System.currentTimeMillis();
				while (true) {
					loopStartTime = System.currentTimeMillis();
					Load.getInstance().getEventProcessor().getRuntime().sendEvent(new CurrentTimeEvent(currentTime));
					try {
						Thread.sleep(SimplePlaybackTimer.this.stepSize);
					} catch (InterruptedException e) {
						logger.warn("Timer reports sleep deprivation!");
					}
					if (Math.random() > 0.9995) {
						System.out.println(new Date(currentTime).toString());
					}
					// }
					loopTime = System.currentTimeMillis() - loopStartTime;
					currentTime += ((long) loopTime * pace);
				}
			}
		};
	}

	@Override
	public synchronized long getCurrentTime() {
		return 5678;
	}

}
