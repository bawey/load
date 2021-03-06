package ch.cern.cms.load.sinks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import ch.cern.cms.esper.annotations.Verbose;
import ch.cern.cms.esper.annotations.Watched;
import ch.cern.cms.load.EventsSink;
import ch.cern.cms.load.LoadLogCollector;
import ch.cern.cms.load.LogSink;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.event.WrapperEventBean;
import com.espertech.esper.event.map.MapEventBean;

public class SwingGui extends EventsSink implements LogSink {

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");

	private class MessageEvnelope {
		public final String content;
		public final String console;

		protected MessageEvnelope(String content, String console) {
			this.content = content;
			this.console = console;
		}
	}

	public final static int CONSOLE_MAX_LINES = 100;
	public final static int CONSOLE_MAX_LENGTH = 1000000;

	private static final Logger logger = Logger.getLogger(SwingGui.class);

	@Override
	public StatementAwareUpdateListener getVerboseStatementListener() {
		return consoleLogger;
	}

	@Override
	public StatementAwareUpdateListener getWatchedStatementListener() {
		return watchUpdater;
	}

	protected JTextArea[] txts = new JTextArea[6];

	protected JFrame frm = new JFrame();

	protected JPanel watchBox = new JPanel();
	protected JTabbedPane consoleBox = new JTabbedPane();
	protected JPanel loggingBox = new JPanel() {
		{
			setBackground(Color.BLACK);
		}
	};
	protected JScrollPane loggingScrollPane;

	private Map<String, JTextField> watchVals = new LinkedHashMap<String, JTextField>();
	private Map<String, JLabel> watchLabels = new LinkedHashMap<String, JLabel>();
	private Map<String, JTextArea> consoles = new HashMap<String, JTextArea>();
	private Map<String, JComponent> alarms = new HashMap<String, JComponent>();

	private final BlockingQueue<MessageEvnelope> msgQueue = new ArrayBlockingQueue<SwingGui.MessageEvnelope>(1000, true);
	private final BlockingQueue<UpdateEnvelope> watchedUpdates = new ArrayBlockingQueue<UpdateEnvelope>(10000, true);
	private final BlockingQueue<UpdateEnvelope> verboseUpdates = new ArrayBlockingQueue<UpdateEnvelope>(10000, true);

	private Runnable verboseUpdatesConsumer = new Runnable() {

		@Override
		public void run() {
			while (true) {
				try {
					UpdateEnvelope ue = verboseUpdates.take();
					if (ue == null || ue.newEvents == null) {
						continue;
					}

					VerboseAttributes va = new VerboseAttributes(ue.statement);
					if (!va.append) {
						clearConsole(va.label);
					}

					SwingGui.this.console(va.label, getPrintableUpdate(ue, va).toString());

				} catch (InterruptedException e) {
					System.out.println("Thread interrupted, see logs");
					logger.warn(e);
				}

			}
		}
	};

	private Runnable watchedUpdatesConsumer = new Runnable() {

		@Override
		public void run() {
			while (true) {
				try {
					UpdateEnvelope ue = watchedUpdates.take();

					String label = null;
					String labelName = "label";
					for (Annotation ann : ue.statement.getAnnotations()) {
						if (ann.annotationType().equals(Watched.class)) {
							Watched watched = (Watched) ann;
							if (watched.labelName().equals("")) {
								label = watched.label();
							} else {
								labelName = watched.labelName();
							}
						}
					}
					Map<?, ?> props = null;
					if (ue.newEvents != null && ue.newEvents.length > 0) {
						Object src = ue.newEvents[0].getUnderlying();

						if (src instanceof WrapperEventBean) {
							src = ((WrapperEventBean) src).getUnderlyingEvent();
						}

						if (src instanceof MapEventBean) {
							props = ((MapEventBean) src).getProperties();
						} else if (src instanceof Map<?, ?>) {
							props = (Map<?, ?>) src;
						} else {
							console("warning", "received sth strange in watchUpdater..." + src);
						}
					}
					if (props != null) {
						if (props.size() == 1) {
							Object key = props.keySet().iterator().next();
							SwingGui.this.watch(label.length() > 0 ? label : key.toString(), props.get(key) != null ? props.get(key).toString() : "null");
						} else {
							String[] timeKeys = { "timestamp", "time", "timeStamp", "time_stamp" };
							String[] labelKeys = { labelName };
							String[] valueKeys = { "value", "val" };
							String timestamp = null;
							String value = null;
							for (String key : timeKeys) {
								if (props.containsKey(key)) {
									timestamp = props.get(key).toString();
									props.remove(key);
									break;
								}
							}
							if (label == null || label.length() == 0) {
								for (String key : labelKeys) {
									if (props.containsKey(key)) {
										label = (props.get(key) != null) ? props.get(key).toString() : null;
										break;
									}
								}
							}
							for (String key : valueKeys) {
								if (props.containsKey(key)) {
									value = (props.get(key) != null) ? props.get(key).toString() : null;
									break;
								}
							}

							SwingGui.this.watch(label, value + (timestamp != null ? " (" + timestamp + ")" : ""));
						}
					}

				} catch (InterruptedException e) {
					System.out.println("Thread interrupted, more details in logs");
					logger.warn(e);
				}

			}
		}
	};

	private Runnable consoleUpdater = new Runnable() {

		@Override
		public void run() {
			while (true) {
				MessageEvnelope me;
				try {
					me = msgQueue.take();
					JTextArea console = consoles.get(me.console);
					if (console.getText().length() > CONSOLE_MAX_LENGTH) {
						StringBuffer txt = new StringBuffer(console.getText());
						console.setText(txt.substring(txt.indexOf("\n", txt.length() - CONSOLE_MAX_LENGTH / 2) + 1, txt.length()));
					}
					console.append(me.content + "\n");

				} catch (InterruptedException e) {
					logger.warn(e);
				}
			}
		}

	};

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

	public SwingGui() {
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
		frm.setTitle("L0AD SwingGui");
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
			loggingBox.setLayout(new BoxLayout(loggingBox, BoxLayout.PAGE_AXIS));
			loggingScrollPane = new JScrollPane(loggingBox);
			loggingScrollPane.getViewport().setPreferredSize(size);
			frm.getContentPane().add(loggingScrollPane, BorderLayout.CENTER);
		}
		{

		}
		consoleBox.setPreferredSize(new Dimension(100, 500));
		frm.getContentPane().add(consoleBox, BorderLayout.PAGE_END);

		new Thread(consoleUpdater) {
			{
				setName("SwingGuiConsoleUpdater");
			}
		}.start();

		new Thread(verboseUpdatesConsumer) {
			{
				setName("Verbose Updates Consumer");
			}
		}.start();

		new Thread(watchedUpdatesConsumer) {
			{
				setName("Watched Updates Consumer");
			}
		}.start();

	
		loggingBox.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
			}

			@Override
			public void componentResized(ComponentEvent arg0) {
				loggingScrollPane.getVerticalScrollBar().setValue(
						loggingScrollPane.getVerticalScrollBar().getMaximum());
			}

			@Override
			public void componentMoved(ComponentEvent arg0) {
			}

			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
		});

	}

	protected void console(String title, String message) {
		if (!consoles.keySet().contains(title)) {
			JTextArea area = new JTextArea();
			area.setFont(new Font("monospaced", Font.PLAIN, 12));
			consoles.put(title, area);
			consoles.get(title).setEditable(false);
			consoleBox.addTab(title, new JScrollPane(consoles.get(title)));
		}
		try {
			this.msgQueue.put(new MessageEvnelope(message, title));
		} catch (InterruptedException e) {
			logger.warn("Interrupted message proxy", e);
		}

	}

	protected void clearConsole(String title) {
		if (consoles.containsKey(title)) {
			consoles.get(title).setText("");
		}
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
			loggingBox.add(alarms.get(alarm));
			loggingBox.revalidate();
			loggingBox.repaint();
		}
	}

	protected void cancelAlarm(String alarm) {
		if (!alarms.keySet().contains(alarm)) {
			console("warning", "cancelling an alarm that does not exist");
		} else {
			loggingBox.remove(alarms.get(alarm));
			alarms.remove(alarm);
			loggingBox.revalidate();
			loggingBox.repaint();
		}
	}

	protected StatementAwareUpdateListener watchUpdater = new StatementAwareUpdateListener() {
		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
			try {
				watchedUpdates.put(new UpdateEnvelope(newEvents, oldEvents, statement, epServiceProvider));
			} catch (InterruptedException e) {
				System.out.println("Thread interrupted, check logs");
				logger.warn(e);
			}
		}
	};

	protected StatementAwareUpdateListener consoleLogger = new StatementAwareUpdateListener() {
		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
			try {
				verboseUpdates.put(new UpdateEnvelope(newEvents, oldEvents, statement, epServiceProvider));
			} catch (InterruptedException e) {
				System.out.println("Thread interrupted, consult logs");
				logger.warn(e);
			}
		}
	};

	@Override
	public boolean log(String message) {
		return false;
	}

	@Override
	public boolean log(LoggingEvent loggingEvent) {
		StringBuffer txt = new StringBuffer("<html><i>");
		txt.append(sdf.format(new Date(loggingEvent.timeStamp)));
		txt.append("</i> ").append(loggingEvent.getRenderedMessage());

		if (loggingEvent.getThrowableInformation() != null) {
			for (String throwRep : loggingEvent.getThrowableInformation().getThrowableStrRep()) {
				txt.append("<br />").append(throwRep.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"));
			}
		}
		txt.append("</html>");

		Color textColor;
		switch (loggingEvent.getLevel().toInt()) {
		case Level.DEBUG_INT:
			textColor = Color.CYAN;
			break;
		case Level.ERROR_INT:
		case Level.FATAL_INT:
			textColor = Color.RED;
			break;
		case Level.TRACE_INT:
		case Level.ALL_INT:
		case Level.INFO_INT:
			textColor = Color.GREEN;
			break;
		case Level.WARN_INT:
			textColor = Color.ORANGE;
			break;
		case Level.OFF_INT:
		default:
			textColor = Color.WHITE;
		}

		JLabel label = new JLabel(txt.toString());
		label.setForeground(textColor);

		loggingBox.add(label);

		loggingBox.invalidate();
		loggingBox.revalidate();

		return true;
	}
}
