/**
 * NotificationService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package rcms.services.NotificationService;

public interface NotificationService extends java.rmi.Remote {
    public NotificationService.NotificationEvent onNotificationEvent(NotificationService.NotificationEvent event, java.lang.String notificationManagerIdentifier) throws java.rmi.RemoteException;
    public NotificationService.NotificationEvent subscribe(long timestamp, java.lang.String notificationManagerIentifier) throws java.rmi.RemoteException;
}
