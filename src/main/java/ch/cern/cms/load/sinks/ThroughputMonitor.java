package ch.cern.cms.load.sinks;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ch.cern.cms.esper.Trx;
import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.EventSink;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;

public class ThroughputMonitor extends EventSink {
	private class MeasurementEnvelope {
		public final long engineTime;
		public final long systemTime;

		private MeasurementEnvelope(long engineTime, long systemTime) {
			super();
			this.engineTime = engineTime;
			this.systemTime = systemTime;
		}
	}

	private JFrame frame;
	private EventProcessor ep;
	private JLabel esperTime = new JLabel();
	private JLabel esperDate = new JLabel();
	private JLabel playbackSpeed = new JLabel();
	private Long initSystemTime;
	private Long initEsperTime;
	private BlockingQueue<MeasurementEnvelope> queue = new ArrayBlockingQueue<ThroughputMonitor.MeasurementEnvelope>(1000);

	private Runnable worker = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					MeasurementEnvelope e = queue.take();

					if (initSystemTime != null && initEsperTime != null) {
						playbackSpeed
								.setText("SpeedUp factor: " + String.format("%1$,.2f",(e.engineTime - initEsperTime) / (double) (e.systemTime - initSystemTime)));
					} else {
						initSystemTime = e.systemTime;
						initEsperTime = e.engineTime;
					}
					esperTime.setText("Engine time: " + e.engineTime);
					esperDate.setText("Engine date: " + Trx.toDate(e.engineTime));

				} catch (InterruptedException e) {
					System.out.println("Thread interrupted... no logs.");
					e.printStackTrace();
				}
			}
		}
	};

	private StatementAwareUpdateListener dummyUpdateListener = new StatementAwareUpdateListener() {
		@Override
		public void update(EventBean[] arg0, EventBean[] arg1, EPStatement arg2, EPServiceProvider arg3) {

		}
	};

	public ThroughputMonitor() {
		super();
		frame = new JFrame() {
			{
				setSize(320, 180);
				setTitle("Throughput monitor");
				setVisible(true);
				add(new JPanel() {
					{
						add(esperTime);
						add(esperDate);
						add(playbackSpeed);
						add(new JLabel("EPL dirs: " + Load.getInstance().getSettings().getMany("eplDir").toString()));
					}
				});
			}
		};
		ep = Load.getInstance().getEventProcessor();

		ep.registerStatement("select current_timestamp() from pattern[every timer:interval(333 msec)]", new EngineTimeSubscriber());

		new Thread(worker) {
			{
				setName("Throughput Monitor Worker");
			}
		}.start();

	}

	@Override
	public StatementAwareUpdateListener getVerboseStatementListener() {
		return dummyUpdateListener;
	}

	@Override
	public StatementAwareUpdateListener getWatchedStatementListener() {
		return dummyUpdateListener;
	}

	class EngineTimeSubscriber {
		public void update(Long time) {
			try {
				queue.put(new MeasurementEnvelope(time, System.currentTimeMillis()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
