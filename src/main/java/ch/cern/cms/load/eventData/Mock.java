package ch.cern.cms.load.eventData;

import ch.cern.cms.load.eventProcessing.EventProcessor;

public class Mock {
	// if a fed in alarm for 5sec then alarm, otherwise ignore\
	public final String status;
	// if fmmio.fractionBusy > threshold (~.001) and observed over > 5 sec. then
	// scream
	public final double fractionBusy;
	// jump in overall trigger rate
	public final double rate;

	public Mock(String status, double fractionBusy, double rate) {
		super();
		this.status = status;
		this.fractionBusy = fractionBusy;
		this.rate = rate;
	}

	public String getStatus() {
		return status;
	}

	public double getFractionBusy() {
		return fractionBusy;
	}

	public double getRate() {
		return rate;
	}

	public static void pumpEvents(EventProcessor ep) {
		int count = 0;
		Mock m = new Mock("fine", .00001d, 100d);
		while (m != null) {
			double d = count * 0.01;
			double var = d / (Math.sqrt(1 + d * d));
			boolean flip = Math.random() > var;
			System.out.println(count + ": " + flip + "(" + var + ")");
			ep.sendEvent(m);
			try {
				Thread.sleep((long) (400 * Math.random()));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			m = new Mock(flip ? (m.getStatus().equals("fine") ? "error" : "fine") : m.getStatus(), m.getFractionBusy() + .0001d,
					m.getRate() * 1.25);
			++count;
		}
	}

	@Override
	public String toString() {
		return "Mock [status=" + status + ", fractionBusy=" + fractionBusy + ", rate=" + rate + "]";
	}

}
