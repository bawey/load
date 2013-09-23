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

	public static void playEvents(EventProcessor ep, long sleepTime) {

		Mock[] mocks = new Mock[] { new Mock("fine", .00001, 400), new Mock("fine", .00009, 401), new Mock("fine", .00004, 403),
				new Mock("fine", .00015, 408), new Mock("fine", .00003, 412), new Mock("fine", .00027, 418), new Mock("fine", .00021, 420),
				new Mock("fine", .00018, 429),

				new Mock("fine", .00018, 429),
				/** trigger rate jump, alert goes off **/
				new Mock("error", .00058, 829), new Mock("error", .00062, 400),
				/** trigger rate back to normal, alert goes down **/
				new Mock("fine", .00089, 429), new Mock("fine", .00101, 429),
				/** fraction busy reaches threshold **/
				new Mock("fine", .00111, 380), new Mock("fine", .00121, 350), new Mock("fine", .00131, 310),
				/** alert goes off **/
				new Mock("error", .00131, 200),

				new Mock("error", .00141, 140), new Mock("error", .00151, 110), new Mock("error", .00149, 110), new Mock("error", .00148, 107), new Mock("error", .00018, 90),
				new Mock("error", .00018, 90), new Mock("error", .00018, 90), new Mock("error", .00018, 90), new Mock("error", .00018, 90),
				new Mock("error", .00018, 90), new Mock("error", .00018, 90), new Mock("error", .00018, 90), new Mock("error", .00185, 75),
				new Mock("error", .00186, 60), new Mock("error", .00187, 45), new Mock("error", .00188, 30), new Mock("error", .00189, 15),
				new Mock("error", .00189, 15), new Mock("error", .00189, 15), new Mock("error", .00189, 15), new Mock("error", .00189, 15),
				new Mock("error", .00189, 15), new Mock("error", .00189, 15), new Mock("error", .00189, 15) };

		for (Mock mock : mocks) {
			ep.sendEvent(mock);
			try {
				Thread.sleep(sleepTime);
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}

	@Override
	public String toString() {
		return "Mock [status=" + status + ", fractionBusy=" + fractionBusy + ", rate=" + rate + "]";
	}

}
