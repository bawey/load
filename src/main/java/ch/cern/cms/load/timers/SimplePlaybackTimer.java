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
			private long loopNanoStart = 0l;
			private double accumulatedTime = 0;

			@Override
			public void run() {
				currentTime = System.currentTimeMillis();
				final CurrentTimeEvent cte = new CurrentTimeEvent(0);
				final EPRuntime runtime = Load.getInstance().getEventProcessor().getRuntime();

				Thread sender = new Thread(new Runnable() {
					@Override
					public void run() {
						long lastTime = 0;
						while (true) {
							synchronized (SimplePlaybackTimer.class) {
								if (cte.getTimeInMillis() > lastTime) {
									lastTime = cte.getTimeInMillis();
									runtime.sendEvent(cte);
								}
							}
						}
					}
				});
				sender.start();
				while (true) {
					loopNanoStart = System.nanoTime();
					if (stepSize > 0) {
						try {
							Thread.sleep(SimplePlaybackTimer.this.stepSize);
						} catch (InterruptedException e) {
							logger.warn("Timer reports sleep deprivation!");
						}
					}
					if (accumulatedTime >= 1) {
						currentTime += Math.round(accumulatedTime);
						synchronized (SimplePlaybackTimer.class) {
							cte.setTimeInMillis(currentTime);
						}
						accumulatedTime = 0;
					}
					accumulatedTime += pace * ((System.nanoTime() - loopNanoStart) / 1000000);
				}
			}
		};
	}

	@Override
	public synchronized long getCurrentTime() {
		return 5678;
	}

}
