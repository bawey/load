package ch.cern.cms.load.cep.suites;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.SwingTest;

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class EplSuite extends SwingTest implements CommonColumnNamesDictionary, GenericStreamNamesDictionary {
	protected EventProcessor ep;
	public static final Map<String, Class<?>> types = new HashMap<String, Class<?>>();

	public static final String CONCLUSIONS_STREAM = "CONCLUSIONS_STREAM";
	public static final String CONCLUSIONS_WINDOW = "CONCLUSIONS_WINDOW";

	public EplSuite(EventProcessor ep) {
		this.ep = ep;
	}

	protected EPStatement epl(String epl) {
		return ep.createEPL(epl);
	}

	protected EPStatement epl(String format, Object[] args) {
		return ep.createEPL(new MessageFormat(format).format(args));
	}

	protected void createConclusionStream(EventProcessor ep, String name, String body) {
		epl("create schema " + name + " " + body);
		epl("insert into " + CONCLUSIONS_STREAM + " select * from " + name);
	}

	protected void createConclusionStreams(EventProcessor ep) {
		ep.createEPL("create variant schema " + CONCLUSIONS_STREAM + " as *");
		ep.createEPL("create window Conclusions.win:keepall() as select * from ConclusionsStream");
		ep.createEPL("insert into Conclusions select * from ConclusionsStream");

		ep.registerStatement("select c.* from pattern[every c=Conclusions]", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				EplSuite.this.raiseAlarm(newEvents[0].getUnderlying().toString());
			}
		});

		ep.registerStatement("select rstream * from Conclusions", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				for (EventBean bean : newEvents) {
					EplSuite.this.cancelAlarm(bean.getUnderlying().toString());
				}
			}
		});

		ep.registerStatement("select * from Conclusions", new UpdateListener() {

			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				console("allConclusions", newEvents[0].getUnderlying().toString());
			}
		});
	}

	protected String createWindow(String name, String[] fields, String retentionPolicy) {
		return createWindow(name, fields, retentionPolicy, null);
	}

	protected String createWindow(String name, String[] fields, String retentionPolicy, String from) {
		StringBuilder sb = new StringBuilder("create window ").append(name).append(".").append(retentionPolicy);
		sb.append(" as ");
		if (from != null) {
			sb.append("select ");
		} else {
			sb.append("(");
		}
		for (int i = 0; i < fields.length; ++i) {
			sb.append(fields[i]);
			if (from == null) {
				sb.append(" ").append(types.get(fields[i]).getSimpleName());
			}
			if (i < fields.length - 1) {
				sb.append(",");
			}
		}
		if (from != null) {
			sb.append(" from ").append(from);
		} else {
			sb.append(")");
		}
		return sb.toString();
	}
}
