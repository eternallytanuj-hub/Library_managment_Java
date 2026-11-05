package com.library.daemon;

import com.library.service.TransactionService;
import com.library.service.impl.TransactionServiceImpl;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background daemon executing periodic sweeps using {@link ScheduledExecutorService}
 * to detect overdue loans, transition states, compute fines, and trigger notifications.
 */
public class DueDateMonitorDaemon {

    private static final Logger LOGGER = Logger.getLogger(DueDateMonitorDaemon.class.getName());
    private static final long DEFAULT_INITIAL_DELAY_SECONDS = 5;
    private static final long DEFAULT_PERIOD_SECONDS = 60;

    private final TransactionService transactionService;
    private final long initialDelaySeconds;
    private final long periodSeconds;

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public DueDateMonitorDaemon() {
        this(TransactionServiceImpl.getInstance(), DEFAULT_INITIAL_DELAY_SECONDS, DEFAULT_PERIOD_SECONDS);
    }

    public DueDateMonitorDaemon(TransactionService transactionService) {
        this(transactionService, DEFAULT_INITIAL_DELAY_SECONDS, DEFAULT_PERIOD_SECONDS);
    }

    public DueDateMonitorDaemon(TransactionService transactionService, long initialDelaySeconds, long periodSeconds) {
        this.transactionService = Objects.requireNonNull(transactionService, "TransactionService must not be null");
        this.initialDelaySeconds = initialDelaySeconds >= 0 ? initialDelaySeconds : DEFAULT_INITIAL_DELAY_SECONDS;
        this.periodSeconds = periodSeconds > 0 ? periodSeconds : DEFAULT_PERIOD_SECONDS;
    }

    /**
     * Starts the scheduled overdue detection daemon.
     */
    public synchronized void start() {
        if (running) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "due-date-monitor-daemon");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(
                this::runOverdueSweep,
                initialDelaySeconds,
                periodSeconds,
                TimeUnit.SECONDS
        );

        running = true;
        LOGGER.info(String.format("DueDateMonitorDaemon started (initialDelay: %ds, period: %ds)",
                initialDelaySeconds, periodSeconds));
    }

    /**
     * Executes an immediate overdue sweep cycle. Exception-safe to preserve daemon scheduler health.
     *
     * @return Number of overdue transactions processed in this cycle
     */
    public int runOverdueSweep() {
        try {
            LOGGER.fine("Running scheduled overdue loan sweep...");
            int processed = transactionService.processOverdueTransactions();
            if (processed > 0) {
                LOGGER.info("DueDateMonitorDaemon processed " + processed + " overdue transactions.");
            }
            return processed;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred during DueDateMonitorDaemon sweep execution", e);
            return 0;
        }
    }

    /**
     * Gracefully stops the scheduled daemon.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("DueDateMonitorDaemon stopped cleanly.");
    }

    public boolean isRunning() {
        return running;
    }

    public long getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public long getPeriodSeconds() {
        return periodSeconds;
    }
}
