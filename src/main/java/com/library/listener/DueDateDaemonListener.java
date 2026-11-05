package com.library.listener;

import com.library.daemon.AsyncNotificationDispatcher;
import com.library.daemon.DueDateMonitorDaemon;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Logger;

/**
 * Application lifecycle listener managing background daemons (Due Date Monitor &
 * Async Notification Dispatcher) upon web container startup and teardown.
 */
@WebListener
public class DueDateDaemonListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(DueDateDaemonListener.class.getName());

    public static final String DUE_DATE_DAEMON_ATTR = "DUE_DATE_DAEMON";
    public static final String NOTIFICATION_DISPATCHER_ATTR = "NOTIFICATION_DISPATCHER";

    private DueDateMonitorDaemon dueDateDaemon;
    private AsyncNotificationDispatcher notificationDispatcher;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Starting background services and daemons via DueDateDaemonListener...");

        // 1. Initialize and start asynchronous notification dispatcher
        notificationDispatcher = AsyncNotificationDispatcher.getInstance();
        notificationDispatcher.start();
        sce.getServletContext().setAttribute(NOTIFICATION_DISPATCHER_ATTR, notificationDispatcher);

        // 2. Initialize and start periodic due-date monitor daemon
        dueDateDaemon = new DueDateMonitorDaemon();
        dueDateDaemon.start();
        sce.getServletContext().setAttribute(DUE_DATE_DAEMON_ATTR, dueDateDaemon);

        LOGGER.info("Background daemons successfully initialized.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Gracefully stopping background daemons via DueDateDaemonListener...");

        if (dueDateDaemon != null) {
            dueDateDaemon.stop();
        }

        if (notificationDispatcher != null) {
            notificationDispatcher.stop();
        }

        LOGGER.info("Background daemons cleanly terminated.");
    }
}
