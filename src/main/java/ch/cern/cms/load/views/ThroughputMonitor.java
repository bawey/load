package ch.cern.cms.load.views;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ch.cern.cms.esper.Trx;
import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.LoadView;

import com.espertech.esper.client.StatementAwareUpdateListener;

public class ThroughputMonitor implements LoadView {

	private JFrame frame;
	private EventProcessor ep;
	private JLabel esperTime = new JLabel();
	private JLabel esperDate = new JLabel();
	private JLabel playbackSpeed = new JLabel();
	private Long initSystemTime;
	private Long initEsperTime;

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

	}

	@Override
	public StatementAwareUpdateListener getVerboseStatementListener() {
		return null;
	}

	@Override
	public StatementAwareUpdateListener getWatchedStatementListener() {
		return null;
	}

	class EngineTimeSubscriber {
		public void update(Long time) {
			if (initSystemTime != null && initEsperTime != null) {
				playbackSpeed.setText("SpeedUp factor: " + ((time - initEsperTime) / (double) (System.currentTimeMillis() - initSystemTime)));
			} else {
				initSystemTime = System.currentTimeMillis();
				initEsperTime = time;
			}
			esperTime.setText("Engine time: " + time);
			esperDate.setText("Engine date: " + Trx.toDate(time));
		}
	}
}
