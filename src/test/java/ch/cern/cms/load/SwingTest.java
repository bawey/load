package ch.cern.cms.load;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.event.map.MapEventBean;

public class SwingTest {
	protected JTextArea[] txts = new JTextArea[6];

	protected JFrame frm = new JFrame();

	protected JPanel watchBox = new JPanel();
	protected JTabbedPane consoleBox = new JTabbedPane();

	private Map<String, JTextField> watchVals = new LinkedHashMap<String, JTextField>();
	private Map<String, JLabel> watchLabels = new LinkedHashMap<String, JLabel>();
	private Map<String, JTextArea> consoles = new HashMap<String, JTextArea>();

	private Runnable updater = new Runnable() {
		@Override
		public void run() {
			if (!(watchVals.keySet().containsAll(watchLabels.keySet()) && watchLabels.keySet().containsAll(watchVals.keySet()))) {
				throw new RuntimeException("AHA!");
			}

			GroupLayout layout = new GroupLayout(watchBox);
			watchBox.setLayout(layout);

			layout.setAutoCreateGaps(true);

			layout.setAutoCreateContainerGaps(true);

			GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

			ParallelGroup texts = layout.createParallelGroup();
			ParallelGroup labels = layout.createParallelGroup();
			GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
			synchronized (this) {

				for (String title : watchVals.keySet()) {
					texts.addComponent(watchVals.get(title));
					labels.addComponent(watchLabels.get(title));
					vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(watchLabels.get(title))
							.addComponent(watchVals.get(title)));
				}
			}
			hGroup.addGroup(labels);
			hGroup.addGroup(texts);
			layout.setHorizontalGroup(hGroup);
			layout.setVerticalGroup(vGroup);

		}
	};

	private synchronized void layWatchesOut() {
		SwingUtilities.invokeLater(updater);
	}

	@Before
	public void setUp() {
		JFrame frame = new JFrame();
		frame.setSize(new Dimension(1800, 1000));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(2, 3));
		for (int i = 0; i < 6; ++i) {
			txts[i] = new JTextArea();
			txts[i].setEditable(false);
			txts[i].setFocusable(false);
			frame.getContentPane().add(new JScrollPane(txts[i]));
		}

		frm.setSize(new Dimension(1800, 1000));
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setVisible(true);
		frm.setTitle("Test output");
		frm.getContentPane().setLayout(new BorderLayout(1, 1));

		// JScrollPane scrollWatch = new JScrollPane(watchBox);
		// scrollWatch.getViewport().setPreferredSize(new Dimension(500, 900));
		// frm.getContentPane().add(scrollWatch, BorderLayout.LINE_START);

		{// try to fix that exception
			Dimension size = new Dimension(700, 1000);
			watchBox.setPreferredSize(size);
			watchBox.setMaximumSize(size);
			watchBox.setSize(size);
			watchBox.setMinimumSize(size);
			// watchBox.set
			frm.getContentPane().add(watchBox, BorderLayout.LINE_START);

		}
		consoleBox.setPreferredSize(new Dimension(100, 400));
		frm.getContentPane().add(consoleBox, BorderLayout.PAGE_END);

	}

	protected void console(String title, String message) {
		if (!consoles.keySet().contains(title)) {
			consoles.put(title, new JTextArea());
			consoles.get(title).setEditable(false);
			consoleBox.addTab(title, new JScrollPane(consoles.get(title)));
		}
		consoles.get(title).setText(consoles.get(title).getText() + "\n" + message);
	}

	protected synchronized void watch(String title, String message) {
		if (!watchVals.containsKey(title)) {
			watchVals.put(title, new JTextField(message, 20));
			watchLabels.put(title, new JLabel(title));
			layWatchesOut();
		} else {
			watchVals.get(title).setText(message);
		}
	}

	@Test
	public void test() {
		for (int i = 1; i <= 100; i += 1) {
			console("progress", i + "%");
			double random = Math.random() * i;
			if (random > 30) {
				console("sufficient", "new random number at " + new Date().toString() + ": " + random);
			} else {
				console("insufficient", "new random number at " + new Date().toString() + ": " + random);
			}
			synchronized (this) {
				watch("random", new Double(random).toString());
				watch("date", new Date().toString());
				watch("bug_" + random, "ooops");
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected UpdateListener watchUpdater = new UpdateListener() {

		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			if (newEvents != null && newEvents.length > 0 && newEvents[0] instanceof MapEventBean) {
				Map<String, Object> props = ((MapEventBean) newEvents[0]).getProperties();
				String[] timeKeys = { "timestamp", "time", "timeStamp", "time_stamp" };
				String timestamp = null;
				for (String key : timeKeys) {
					if (props.containsKey(key)) {
						timestamp = props.get(key).toString();
						props.remove(key);
						break;
					}
				}

				for (Object key : props.keySet()) {
					SwingTest.this.watch(key.toString(), props.get(key).toString() + (timestamp != null ? " (" + timestamp + ")" : ""));
				}
			}
		}
	};

	protected UpdateListener consoleLogger = new UpdateListener() {

		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			SwingTest.this.console("default", newEvents[0].getUnderlying().toString());
		}

	};

}
