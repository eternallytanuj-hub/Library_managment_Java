package com.library.daemon;

import com.library.dao.NotificationDao;
import com.library.dao.impl.NotificationDaoJdbcImpl;
import com.library.model.Notification;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background worker thread polling an asynchronous {@link BlockingQueue} of {@link Notification}
 * alerts and persisting/dispatching them without blocking web request worker threads.
 */
public class AsyncNotificationDispatcher {

    private static final Logger LOGGER = Logger.getLogger(AsyncNotificationDispatcher.class.getName());
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;
    private static volatile AsyncNotificationDispatcher instance;

    private final BlockingQueue<Notification> queue;
    private final NotificationDao notificationDao;
    private ExecutorService workerExecutor;
    private volatile boolean running = false;

    public AsyncNotificationDispatcher() {
        this(new NotificationDaoJdbcImpl(), DEFAULT_QUEUE_CAPACITY);
    }

    public AsyncNotificationDispatcher(NotificationDao notificationDao) {
        this(notificationDao, DEFAULT_QUEUE_CAPACITY);
    }

    public AsyncNotificationDispatcher(NotificationDao notificationDao, int queueCapacity) {
        this.notificationDao = Objects.requireNonNull(notificationDao, "NotificationDao must not be null");
        this.queue = new LinkedBlockingQueue<>(queueCapacity > 0 ? queueCapacity : DEFAULT_QUEUE_CAPACITY);
    }

    public static AsyncNotificationDispatcher getInstance() {
        if (instance == null) {
            synchronized (AsyncNotificationDispatcher.class) {
                if (instance == null) {
                    instance = new AsyncNotificationDispatcher();
                }
            }
        }
        return instance;
    }

    /**
     * Starts the asynchronous background worker thread.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        workerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "async-notification-worker");
            t.setDaemon(true);
            return t;
        });

        workerExecutor.submit(this::processQueueLoop);
        LOGGER.info("AsyncNotificationDispatcher background worker started successfully.");
    }

    /**
     * Submits a notification for asynchronous background processing.
     *
     * @param notification Notification entity to dispatch
     * @return true if enqueued successfully, false if queue is full
     */
    public boolean enqueue(Notification notification) {
        if (notification == null) {
            return false;
        }
        boolean offered = queue.offer(notification);
        if (!offered) {
            LOGGER.warning("Notification queue full! Dropping async notification: " + notification.getTitle());
        }
        return offered;
    }

    /**
     * Main background loop draining notifications from the queue.
     */
    private void processQueueLoop() {
        while (running || !queue.isEmpty()) {
            try {
                Notification notif = queue.poll(200, TimeUnit.MILLISECONDS);
                if (notif != null) {
                    try {
                        notificationDao.save(notif);
                        LOGGER.fine("Async notification persisted for user " + notif.getUserId());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to persist async notification for user " + notif.getUserId(), e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Gracefully stops the worker thread, allowing buffered notifications to drain.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (workerExecutor != null && !workerExecutor.isShutdown()) {
            workerExecutor.shutdown();
            try {
                if (!workerExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    workerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("AsyncNotificationDispatcher stopped cleanly.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getQueuedCount() {
        return queue.size();
    }

    public BlockingQueue<Notification> getQueue() {
        return queue;
    }
}
