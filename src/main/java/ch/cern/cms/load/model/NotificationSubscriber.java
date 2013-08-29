package ch.cern.cms.load.model;

import notificationService.NotificationEvent;

public interface NotificationSubscriber {
	public void processNotification(NotificationEvent ne);
}
