package roboData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import ch.cern.cms.load.EventProcessor;

public class RoboFrame {
	/**
	 * TRIAL-ID : categorical, the trial id of the experience that the
	 * observation belongs to DESCRIPTION : a symbolic description of the
	 * experience design TIME-SECS : a reading of the Pioneer's internal clock,
	 * in seconds BATTERY-LEVEL : a reading of battery level, in volts SONAR-0 :
	 * sonar depth reading, in mm, of the left (90) pointing sonar SONAR-1 :
	 * sonar depth reading, in mm, of a (15) pointing sonar SONAR-2 : sonar
	 * depth reading, in mm, of a (7.5) pointing sonar SONAR-3 : sonar depth
	 * reading, in mm, of a forward (0) pointing sonar SONAR-4 : sonar depth
	 * reading, in mm, of a (-7.5) pointing sonar SONAR-5 : sonar depth reading,
	 * in mm, of a (-15) pointing sonar SONAR-6 : sonar depth reading, in mm, of
	 * a right (-90) pointing sonar HEADING : heading reading, in degrees, from
	 * the robot's "true north" R-WHEEL-VEL : right wheel velocity, in mm/sec
	 * L-WHEEL-VEL : left wheel velocity, in mm/sec TRANS-VEL : translational
	 * velocity, mm/sec ROT-VEL : rotational velocity, mm/sec R-STALL : right
	 * wheel stall sensor, binary (0/1) L-STALL : left wheel stall sensor,
	 * binary (0/1) ROBOT-STATUS : robot status, 2.0 = stationary, 3.0 = moving
	 */

	public final String trialId;
	public final String desc;
	public final double time;
	public final double battery;
	public final double sonar0;
	public final double sonar1;
	public final double sonar2;
	public final double sonar3;
	public final double sonar4;
	public final double sonar5;
	public final double sonar6;
	public final double heading;
	public final double rWheelVel;
	public final double lWheelVel;
	public final double transVel;
	public final double rotVel;
	public final double rStall;
	public final double lStall;
	public final double status;

	private RoboFrame(String trialId, String desc, double time, double battery, double sonar0, double sonar1, double sonar2, double sonar3,
			double sonar4, double sonar5, double sonar6, double heading, double rWheelVel, double lWheelVel, double transVel,
			double rotVel, double rStall, double lStall, double status) {
		super();
		this.trialId = trialId;
		this.desc = desc;
		this.time = time;
		this.battery = battery;
		this.sonar0 = sonar0;
		this.sonar1 = sonar1;
		this.sonar2 = sonar2;
		this.sonar3 = sonar3;
		this.sonar4 = sonar4;
		this.sonar5 = sonar5;
		this.sonar6 = sonar6;
		this.heading = heading;
		this.rWheelVel = rWheelVel;
		this.lWheelVel = lWheelVel;
		this.transVel = transVel;
		this.rotVel = rotVel;
		this.rStall = rStall;
		this.lStall = lStall;
		this.status = status;
	}

	public String getTrialId() {
		return trialId;
	}

	public String getDesc() {
		return desc;
	}

	public double getTime() {
		return time;
	}

	public double getBattery() {
		return battery;
	}

	public double getSonar0() {
		return sonar0;
	}

	public double getSonar1() {
		return sonar1;
	}

	public double getSonar2() {
		return sonar2;
	}

	public double getSonar3() {
		return sonar3;
	}

	public double getSonar4() {
		return sonar4;
	}

	public double getSonar5() {
		return sonar5;
	}

	public double getSonar6() {
		return sonar6;
	}

	public double getHeading() {
		return heading;
	}

	public double getrWheelVel() {
		return rWheelVel;
	}

	public double getlWheelVel() {
		return lWheelVel;
	}

	public double getTransVel() {
		return transVel;
	}

	public double getRotVel() {
		return rotVel;
	}

	public double getrStall() {
		return rStall;
	}

	public double getlStall() {
		return lStall;
	}

	public double getStatus() {
		return status;
	}

	public static void fromFile(String path, EventProcessor ep) throws FileNotFoundException {

		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		double prevTime = -1;
		try {
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String trialId = tokens[0];
				String desc = tokens[1];
				double time = Double.parseDouble(tokens[2]);
				double battery = Double.parseDouble(tokens[3]);
				double sonar0 = Double.parseDouble(tokens[4]);
				double sonar1 = Double.parseDouble(tokens[5]);
				double sonar2 = Double.parseDouble(tokens[6]);
				double sonar3 = Double.parseDouble(tokens[7]);
				double sonar4 = Double.parseDouble(tokens[8]);
				double sonar5 = Double.parseDouble(tokens[9]);
				double sonar6 = Double.parseDouble(tokens[10]);
				double heading = Double.parseDouble(tokens[11]);
				double rWheelVel = Double.parseDouble(tokens[12]);
				double lWheelVel = Double.parseDouble(tokens[13]);
				double transVel = Double.parseDouble(tokens[14]);
				double rotVel = Double.parseDouble(tokens[15]);
				double rStall = Double.parseDouble(tokens[16]);
				double lStall = Double.parseDouble(tokens[17]);
				double status = Double.parseDouble(tokens[18]);
				RoboFrame frame = new RoboFrame(trialId, desc, time, battery, sonar0, sonar1, sonar2, sonar3, sonar4, sonar5, sonar6,
						heading, rWheelVel, lWheelVel, transVel, rotVel, rStall, lStall, status);

				if (prevTime > 0) {
					try {
						Thread.sleep((long) (Math.max((time - prevTime) * 10, 0)));
					} catch (InterruptedException e) {
						System.err.println("no sleep!");
					}
				}
				prevTime = time;
				ep.sendEvent(frame);
			}
		} catch (IOException e) {
			System.err.println("ioexception!");
		} finally {
			try {
				br.close();
			} catch (IOException ioe) {
				System.err.println("Ooops while closing the reader");
			}
		}
	}

	@Override
	public String toString() {
		return "RoboFrame [trialId=" + trialId + ", desc=" + desc + ", time=" + time + ", battery=" + battery + ", sonar0=" + sonar0
				+ ", sonar1=" + sonar1 + ", sonar2=" + sonar2 + ", sonar3=" + sonar3 + ", sonar4=" + sonar4 + ", sonar5=" + sonar5
				+ ", sonar6=" + sonar6 + ", heading=" + heading + ", rWheelVel=" + rWheelVel + ", lWheelVel=" + lWheelVel + ", transVel="
				+ transVel + ", rotVel=" + rotVel + ", rStall=" + rStall + ", lStall=" + lStall + ", status=" + status + "]";
	}
}
