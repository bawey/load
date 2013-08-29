package ch.cern.cms.load.model;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import notificationService.NotificationEvent;

import org.json.simple.JSONValue;

/**
 * 
 * @author Tomasz Bawej let's put it all here and later see how to improve on
 *         that
 * 
 *         initially subscribe with LZNR to pick all the updates and put them to
 *         the map right-away also try to obtain the full parameterSet via
 *         PartameterService. Obtained data should be merged with what could
 *         have been sent in the meantime by LZNR, but not overwrite it (it
 *         takes usually quite long to pull the whole parameter set and god
 *         knows what can be dropped in the meantime
 * 
 */
public class Model implements NotificationSubscriber {
	private static Model instance = null;

	public static Model getInstance() {
		if (instance == null) {
			synchronized (Model.class) {
				if (instance == null) {
					instance = new Model();
				}
			}
		}
		return instance;
	}

	private Map<String, Object> data = new HashMap<String, Object>();

	private Model() {
		LevelZeroNotificationForwarder.subscribe(this);
		Runnable getTheBigSet = new Runnable() {
			@Override
			public void run() {
				try {
					Map<String, Object> bigMap = LevelZeroDataProvider.getInstance().getAsMap();
					synchronized (data) {
						for (Map.Entry<String, Object> entry : bigMap.entrySet()) {
							if (!data.containsKey(entry.getKey())) {
								data.put(entry.getKey(), entry.getValue());
							}
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("I'm defeated", e);
				}
			}
		};
		new Thread(getTheBigSet).start();

	}

	public Map<String, Object> getData() {
		return data;
	}

	@Override
	public void processNotification(NotificationEvent ne) {
		Map<String, Object> map = new HashMap<String, Object>();

		Pattern p = Pattern.compile("<PARAMETER><NAME>(.*?)</NAME>|<VALUE>(.*?)</VALUE></PARAMETER>");
		Matcher matcher = p.matcher(ne.getContent());
		int i = 0;
		String key = null;
		while (matcher.find()) {
			int mod = i++ % 2;
			String val = matcher.group(1 + mod);
			if (mod == 0) {
				key = val;
			} else {
				map.put(key, val != null ? JSONValue.parse(val) : null);
			}
		}
		synchronized (data) {
			data.putAll(map);
		}
	}

}
