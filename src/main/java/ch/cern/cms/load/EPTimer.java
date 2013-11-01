package ch.cern.cms.load;

import org.apache.log4j.Logger;

public abstract class EPTimer {

	private static final Logger logger = Logger.getLogger(EPTimer.class);

	protected Runnable timerJob = null;

	protected long startTime;
	protected double pace;
	protected long stepSize;

	public void setPace(double pace) {
		logger.info("Timer pace: " + pace);
		this.pace = pace;
	}

	public double getPace() {
		return pace;
	}

	public void setStepSize(long stepSize) {
		logger.info("Timer step: " + stepSize);
		this.stepSize = stepSize;
	}

	public abstract long getCurrentTime();

	public final void start() {
		new Thread(timerJob).start();
	}
}
