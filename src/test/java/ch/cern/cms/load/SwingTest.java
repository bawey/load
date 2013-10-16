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
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.event.WrapperEventBean;
import com.espertech.esper.event.map.MapEventBean;

public class SwingTest {
	protected JTextArea[] txts = new JTextArea[6];

	protected JFrame frm = new JFrame();

	protected JPanel watchBox = new JPanel();
	protected JTabbedPane consoleBox = new JTabbedPane();
	protected JPanel alarmBox = new JPanel();

	private Map<String, JTextField> watchVals = new LinkedHashMap<String, JTextField>();
	private Map<String, JLabel> watchLabels = new LinkedHashMap<String, JLabel>();
	private Map<String, JTextArea> consoles = new HashMap<String, JTextArea>();
	private Map<String, JComponent> alarms = new HashMap<String, JComponent>();

	private Runnable updater = new Runnable() {
		@Override
		public void run() {

			if (!(watchVals.keySet().containsAll(watchLabels.keySet()) && watchLabels.keySet().containsAll(watchVals.keySet()))) {
				throw new RuntimeException("Watched values contain a key not present among the labels or the other way round");
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
			Dimension size = new Dimension(650, 1000);
			JScrollPane pane = new JScrollPane(watchBox);
			pane.getViewport().setPreferredSize(size);
			frm.getContentPane().add(pane, BorderLayout.LINE_START);
			/** add the alarms box **/
			alarmBox.setLayout(new BoxLayout(alarmBox, BoxLayout.PAGE_AXIS));
			pane = new JScrollPane(alarmBox);
			pane.getViewport().setPreferredSize(size);
			frm.getContentPane().add(pane, BorderLayout.LINE_END);

		}
		consoleBox.setPreferredSize(new Dimension(100, 500));
		frm.getContentPane().add(consoleBox, BorderLayout.PAGE_END);

	}

	protected void console(String title, String message) {
		if (!consoles.keySet().contains(title)) {
			consoles.put(title, new JTextArea());
			consoles.get(title).setEditable(false);
			consoleBox.addTab(title, new JScrollPane(consoles.get(title)));
		}
		consoles.get(title).setText(message + "\n" + consoles.get(title).getText());
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

	protected void raiseAlarm(String alarm) {
		if (alarms.keySet().contains(alarm)) {
			console("warning", "raising an alarm already active");
		} else {
			alarms.put(alarm, new JLabel(alarm));
			alarmBox.add(alarms.get(alarm));
			alarmBox.revalidate();
			alarmBox.repaint();
		}
	}

	protected void cancelAlarm(String alarm) {
		if (!alarms.keySet().contains(alarm)) {
			console("warning", "cancelling an alarm that does not exist");
		} else {
			alarmBox.remove(alarms.get(alarm));
			alarms.remove(alarm);
			alarmBox.revalidate();
			alarmBox.repaint();
		}
	}

	@Test
	public void test() {

		String alarms[] = "Current complex[edit] Map of the CERN accelerator complex Map of the Large Hadron Collider together with the Super Proton Synchrotron at CERN CERN operates a network of six accelerators and a decelerator. Each machine in the chain increases the energy of particle beams before delivering them to experiments or to the next more powerful accelerator. Currently active machines are: Two linear accelerators generate low energy particles. Linac2 accelerates protons to 50 MeV for injection into the Proton Synchrotron Booster (PSB), and Linac3 provides heavy ions at 4.2 MeV/u for injection into the Low Energy Ion Ring (LEIR).[16] The Proton Synchrotron Booster increases the energy of particles generated by the proton linear accelerator before they are transferred to the other accelerators.The Low Energy Ion Ring (LEIR) accelerates the ions from the ion linear accelerator, before transferring them to the Proton Synchrotron (PS). This accelerator was commissioned in 2005, after having been reconfigured from the previous Low Energy Antiproton Ring (LEAR).The 28 GeV Proton Synchrotron (PS), built in 1959 and still operating as a feeder to the more powerful SPS.The Super Proton Synchrotron (SPS), a circular accelerator with a diameter of 2 kilometres built in a tunnel, which started operation in 1976. It was designed to deliver an energy of 300 GeV and was gradually upgraded to 450 GeV. As well as having its own beamlines for fixed-target experiments (currently COMPASS and NA62), it has been operated as a proton–antiproton collider (the SppS collider), and for accelerating high energy electrons and positrons which were injected into the Large Electron–Positron Collider (LEP). Since 2008, it has been used to inject protons and heavy ions into the Large Hadron Collider (LHC).The On-Line Isotope Mass Separator (ISOLDE), which is used to study unstable nuclei. The radioactive ions are produced by the impact of protons at an energy of 1.0–1.4 GeV from the Proton Synchrotron Booster. It was first commissioned in 1967 and was rebuilt with major upgrades in 1974 and 1992.REX-ISOLDE increases the charge states of ions coming from the ISOLDE targets, and accelerates them to a maximum energy of 3 MeV/u. The Antiproton Decelerator (AD), which reduces the velocity of antiprotons to about 10% of the speed of light for research into antimatter.The Compact Linear Collider Test Facility, which studies feasibility issues for the future normal conducting linear collider project."
				.split(" ");

		for (int i = 1; i <= 10000; i += 1) {
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
			for (String alarm : alarms) {
				if (this.alarms.keySet().contains(alarm)) {
					if (Math.random() < 0.1) {
						cancelAlarm(alarm);
						console("alarms", "cancelling " + alarm);
					}
				} else {
					if (Math.random() < 0.1) {
						console("alarms", "raising " + alarm);
						raiseAlarm(alarm);
					}
				}
			}
			try {
				Thread.sleep(600);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected UpdateListener watchUpdater = new UpdateListener() {

		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			Map<?, ?> props = null;
			if (newEvents != null && newEvents.length > 0) {
				Object src = newEvents[0].getUnderlying();

				if (src instanceof WrapperEventBean) {
					src = ((WrapperEventBean) src).getUnderlyingEvent();
				}

				if (src instanceof MapEventBean) {
					props = ((MapEventBean) src).getProperties();
				} else if (src instanceof Map<?, ?>) {
					props = (Map<?, ?>) src;
				} else {
					console("warning", "received sth strange in watchUpdater");
				}
			}
			if (props != null) {
				if (props.size() == 1) {
					Object key = props.keySet().iterator().next();
					SwingTest.this.watch(key.toString(), props.get(key).toString());
				} else {
					String[] timeKeys = { "timestamp", "time", "timeStamp", "time_stamp" };
					String[] labelKeys = { "label" };
					String[] valueKeys = { "value", "val" };
					String timestamp = null;
					String value = null;
					String label = null;
					for (String key : timeKeys) {
						if (props.containsKey(key)) {
							timestamp = props.get(key).toString();
							props.remove(key);
							break;
						}
					}
					for (String key : labelKeys) {
						if (props.containsKey(key)) {
							label = (props.get(key) != null) ? props.get(key).toString() : null;
							break;
						}
					}
					for (String key : valueKeys) {
						if (props.containsKey(key)) {
							value = (props.get(key) != null) ? props.get(key).toString() : null;
							break;
						}
					}

					SwingTest.this.watch(label, value + (timestamp != null ? " (" + timestamp + ")" : ""));
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
