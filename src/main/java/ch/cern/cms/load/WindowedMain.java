package ch.cern.cms.load;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import ch.cern.cms.load.configuration.Settings;
import ch.cern.cms.load.configuration.Settings.Runmode;
import ch.cern.cms.load.model.Model;
import ch.cern.cms.load.model.ModelListener;

public class WindowedMain implements ModelListener {

	private static WindowedMain instance = null;
	private JFrame frame = null;
	private Model model;
	private JScrollPane leftScrollPane = null;

	private WindowedMain(JFrame frame, Model model) {
		super();
		this.frame = frame;
		this.model = model;
	}

	private static void switchToOfflineMode() {
		Settings.getInstance().setRunmode(Runmode.OFFLINE);
		Settings.getInstance().setPlaybackRate(100d);
		Settings.getInstance().setDataSource(new File("dmp/data.bt"));
	}

	public static final void main(String[] args) {
		switchToOfflineMode();
		Model model = Model.getInstance();
		JFrame frame = new JFrame("FrameDemo");
		instance = new WindowedMain(frame, model);
		model.registerListener(instance);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setSize(1024, 768);

		frame.setVisible(true);
	}

	@Override
	public void react(EventType changeType) {
		if (changeType.equals(EventType.DATA_SET_CHANGED)) {
			System.out.println("setting list");
			setList();
		}
	}

	private void setList() {
		Container cnt = frame.getContentPane();

		// model.getData().keySet().toArray()

		JList list = new JList(model.getData().keySet().toArray());
		list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(-1);

		leftScrollPane = new JScrollPane(list);
		// leftScrollPane.add(list);
		JPanel jp = new JPanel();
		jp.add(leftScrollPane);
		cnt.add(jp, BorderLayout.LINE_START);
		cnt.add(list, BorderLayout.LINE_END);
		leftScrollPane.revalidate();
		list.revalidate();
		leftScrollPane.repaint();
		cnt.repaint();
	}
}
