package ru.nsk.test.db.ra.inbound;

import java.text.MessageFormat;
import javax.resource.NotSupportedException;
import javax.resource.spi.endpoint.*;
import javax.resource.spi.work.*;

import java.util.*;
import java.util.logging.*;

/**
 *
 *
 */
public class PollingThread implements Work {

    public static final Logger logger =
            Logger.getLogger(PollingThread.class.getPackage().getName());
    static ResourceBundle resource =
            java.util.ResourceBundle.getBundle(PollingThread.class.getPackage().getName()
            + ".LocalStrings");
    private boolean active = false;
    protected transient WorkManager workManager;
    private transient HashMap endpointConsumers = null;
    private static int QUANTUM = 200; // 200 msec between database pooling...
    private static volatile HashSet inProcessGroup = new HashSet();

    /**
     * Constructor.
     */
    public PollingThread(WorkManager workManager) {
        this.active = true;
        this.workManager = workManager;

        logger.finest(MessageFormat.format(
                "[PollingThread::Constructor] work manager {0}.", workManager));

        /* Set up the hash tables for the use of the resource adapter.
         * These tables hold references to MessageEndpointFactory and
         * endpointConsumers. The factoryToConsumer table links the Message
         * factory id to the Consumer Id.
         */

        endpointConsumers = new HashMap(10);

        logger.finest("[PollingThread::Constructor] Leaving");
    }

    /**
     * release: called by the WorkerManager
     */
    public void release() {
        logger.finest("[S] Worker Manager called release for PollingThread ");
        active = false;
    }

    /**
     * run
     */
    public void run() {
        logger.finest("[PT] WorkManager started polling thread ");

        // do not overuse system resources
        //setPriority(Thread.MIN_PRIORITY);

        while (active) {
            try {
                pollEndpoints();
                Thread.sleep(QUANTUM);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        logger.finest("[PT] Polling Thread Leaving");
    }

    private void pollEndpoints() {

        synchronized (endpointConsumers) {
            Collection consumers = endpointConsumers.entrySet();

            if (consumers != null) {
                Iterator iter = consumers.iterator();

                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    EndpointConsumer ec = (EndpointConsumer) entry.getValue();
                    logger.finest(MessageFormat.format(
                            "[PT] found endpoint consumer {0}",
                            ec.getUniqueKey()));
                    try {
                        while (true) {
                            Set<String> groups = ec.hasNewMessages();
                            if (groups != null) {
                                scheduleMessageDeliveryThread(ec, groups);
                            } else {
                                break; // do work for next endpoint
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param message the message to be delivered
     */
    private void scheduleMessageDeliveryThread(EndpointConsumer ec, Set<String> groups)
            throws Exception {
        logger.log(Level.FINEST, "[PT] scheduling a delivery FROM: {0}", ec.getUniqueKey());
        String lastGroup = null;
        try {
            for (String s : groups) {
                lastGroup = s;
                synchronized (inProcessGroup) {
                    if (inProcessGroup.contains(lastGroup)) {
                        logger.finest(MessageFormat.format(
                                "Group {0} already in sheduler, ignored.", lastGroup));
                        lastGroup = null;
                        continue;
                    }
                    inProcessGroup.add(lastGroup);
                    StringBuilder dump = new StringBuilder("|");
                    Iterator i = inProcessGroup.iterator();
                    while (i.hasNext()) {
                        dump.append(i.next() + "|");
                    }
                    logger.finest(MessageFormat.format("in process: {0}",
                            dump.toString()));
                }
                if (lastGroup != null) {
                    Work deliveryThread = new DeliveryThread(ec, lastGroup);
                    workManager.doWork(deliveryThread); // blocked call
                    synchronized (inProcessGroup) {
                        inProcessGroup.remove(lastGroup);
                        lastGroup = null;
                    }
                }
            }
        } catch (WorkRejectedException ex) {
            logger.log(Level.SEVERE, "[PT] scheduling got error:{0}", ex.getMessage());
            throw new NotSupportedException(java.text.MessageFormat.format(
                    resource.getString(
                    "resourceadapterimpl.worker_activation_rejected"),
                    new Object[]{ex.getMessage()}), ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "[PT] scheduling got error:{0}", ex.getMessage());
            throw new NotSupportedException(java.text.MessageFormat.format(
                    resource.getString(
                    "resourceadapterimpl.worker_activation_failed"),
                    new Object[]{ex.getMessage()}), ex);
        } finally {
            if (lastGroup != null) {
                synchronized (inProcessGroup) {
                    inProcessGroup.remove(lastGroup);
                }
            }
        }
        logger.finest("[PT] scheduling a delivery completed.");
    }

    public void stopPolling() {
        removeAllEndpointConsumers();
        this.active = false;
    }

    public void addEndpointConsumer(MessageEndpointFactory endpointFactory, EndpointConsumer ec) {
        logger.finest("[PT.addEndpointConsumer()] Entered");

        synchronized (endpointConsumers) {
            endpointConsumers.put(endpointFactory, ec);
        }
    }

    public void removeEndpointConsumer(MessageEndpointFactory endpointFactory) {
        logger.finest("[PT.removeEndpointConsumer()] Entered");

        EndpointConsumer ec =
                (EndpointConsumer) endpointConsumers.get(endpointFactory);

        synchronized (endpointConsumers) {
            endpointConsumers.remove(ec);
        }
    }

    /**
     * Iterates through the endpointConsumers, shutting them down and preparing
     * for stopping the Resource Adapter.
     */
    private void removeAllEndpointConsumers() {
        synchronized (endpointConsumers) {
            Collection consumers = endpointConsumers.entrySet();

            if (consumers != null) {
                Iterator iter = consumers.iterator();

                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    EndpointConsumer ec = (EndpointConsumer) entry.getValue();
                    try {
                        endpointConsumers.remove(ec);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        endpointConsumers = null;
    }
}
