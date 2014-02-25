package ch.cern.cms.load;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import ch.cern.cms.load.sinks.VerboseAttributes;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;

public abstract class EventSink {

	private final static Logger logger = Logger.getLogger(EventSink.class);
	protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");

	public class UpdateEnvelope {
		public final EventBean[] newEvents;
		public final EventBean[] oldEvents;
		public final EPStatement statement;
		public final EPServiceProvider epServiceProvider;

		public UpdateEnvelope(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
			this.newEvents = newEvents;
			this.oldEvents = oldEvents;
			this.statement = statement;
			this.epServiceProvider = epServiceProvider;
		}

	}

	public abstract StatementAwareUpdateListener getVerboseStatementListener();

	public abstract StatementAwareUpdateListener getWatchedStatementListener();

	protected StringBuilder getPrintableUpdate(UpdateEnvelope ue, VerboseAttributes va) {
		if (va == null) {
			va = new VerboseAttributes(ue.statement);
		}

		if (ue == null || ue.newEvents == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (EventBean eventBean : ue.newEvents) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			if (va.extrMsg.length() > 0) {
				sb.append(String.format("%1$-32s", va.extrMsg)).append(" | ");
			}
			Object bundled = eventBean.getUnderlying();
			for (String pathElement : va.streamPath) {
				try {
					Method m = bundled.getClass().getMethod("get", Object.class);
					bundled = m.invoke(bundled, pathElement);
					// logger.info("descended into " + pathElement + " and fot " + bundled.getClass());
					if (bundled instanceof EventBean) {
						// logger.info("excavating further");
						bundled = ((EventBean) bundled).getUnderlying();
					}

				} catch (Exception e) {
					logger.error("Failed to unbundle the stream for path: " + va.streamPath);
				}
			}
			if (va.fields != null && va.fields.length > 0) {
				for (String field : va.fields) {
					for (Class<?> ptype : new Class[] { Object.class, String.class }) {
						try {
							Method m = bundled.getClass().getMethod("get", Object.class);
							Object desired = m.invoke(bundled, field);
							if (desired instanceof Date) {
								Date date = (Date) desired;
								desired = sdf.format(date);
							}
							sb.append(String.format("%1$-10s", field + ":") + String.format("%1$-26s", desired != null ? desired.toString() : "null"));
							break;
						} catch (Exception e) {
							logger.error("Ah, failed to invoke the get method using " + ptype + " as parameter type for statement: " + ue.statement.getText());
							for (StackTraceElement el : e.getStackTrace()) {
								logger.error(el);
							}
						}
					}
				}
			} else {
				sb.append(bundled.toString());
			}
		}
		return sb;
	}

}
