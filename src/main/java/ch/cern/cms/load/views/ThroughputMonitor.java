package ch.cern.cms.load.views;

import javax.swing.JFrame;

import ch.cern.cms.load.LoadView;

import com.espertech.esper.client.StatementAwareUpdateListener;

public class ThroughputMonitor implements LoadView {

	private JFrame frame;

	public ThroughputMonitor() {
		super();
		frame = new JFrame() {
			{
				setSize(320, 180);
				setTitle("Throughput monitor");
				setVisible(true);
			}
		};
	}

	@Override
	public StatementAwareUpdateListener getVerboseStatementListener() {
		return null;
	}

	@Override
	public StatementAwareUpdateListener getWatchedStatementListener() {
		return null;
	}

}
