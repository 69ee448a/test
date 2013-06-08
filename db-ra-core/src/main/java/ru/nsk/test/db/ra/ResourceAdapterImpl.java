package ru.nsk.test.db.ra;

import javax.resource.NotSupportedException;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.*;
import javax.resource.spi.work.*;
import javax.resource.*;

import javax.naming.*;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.*;
import ru.nsk.test.db.ra.api.DbRaMessageListener;
import ru.nsk.test.db.ra.api.GroupMessage;

import ru.nsk.test.db.ra.inbound.*;

/**
 * Resource Adapter for Database pooling
 *
 */
public class ResourceAdapterImpl extends ResourcseAdapterConfig
        implements ResourceAdapter, java.io.Serializable {

    protected transient BootstrapContext bootCtx;
    protected transient WorkManager workManager;
    public transient Context jndiContext = null;
    private Set<Work> pollingThread = new HashSet();
    public Method onMessage = null;
    static final Logger logger =
            Logger.getLogger(ResourceAdapterImpl.class.getPackage().getName());
    ResourceBundle resource =
            ResourceBundle.getBundle(ResourceAdapterImpl.class.getPackage().getName()
            + ".LocalStrings");
    private final int DEFAULTPOOLSIZE = 4;

    /**
     * Constructor.
     */
    public ResourceAdapterImpl() {
    }

    /**
     * Called by the AppServer to initialize the Resource Adapter.
     *
     * @param ctx the BootstrapContext
     */
    public void start(BootstrapContext ctx)
            throws ResourceAdapterInternalException {
        /* Bootstrap context - used to acquire WorkManager, Timer, 
         * or XATerminator
         */

        logger.info("RA trying to start...");

        if (getPoolSize() == null || getPoolSize() == 0) {
            setPoolSize(DEFAULTPOOLSIZE);
        }
        logger.info(MessageFormat.format(
                "Configuration: poll threads count {0}", getPoolSize()));

        this.bootCtx = ctx;

        try {
            // Get the initial JNDI Context
            this.jndiContext = new InitialContext();

            // Get Work Manager
            this.workManager = ctx.getWorkManager();
        } catch (Exception ex) {
            logger.severe(resource.getString("resourceadapterimpl.noservice"));
            ex.printStackTrace();
            throw new ResourceAdapterInternalException(
                    resource.getString("resourceadapterimpl.noservice"));
        }

        setOnMessageMethod();

        /* Start the polling threads */

        try {
            for (int i = 0; i < getPoolSize(); i++) {
                Work worker = new PollingThread(workManager);
                workManager.scheduleWork(worker);
                pollingThread.add(worker);
            }
        } catch (WorkRejectedException ex) {
            throw new ResourceAdapterInternalException(java.text.MessageFormat.format(
                    resource.getString(
                    "resourceadapterimpl.worker_activation_rejected"),
                    new Object[]{ex.getMessage()}), ex);
        } catch (Exception ex) {
            throw new ResourceAdapterInternalException(java.text.MessageFormat.format(
                    resource.getString(
                    "resourceadapterimpl.worker_activation_failed"),
                    new Object[]{ex.getMessage()}), ex);
        }
    }

    /**
     * Sets the method for the onMessage method used in MessageListener.
     */
    private void setOnMessageMethod() {
        Method onMessageMethod = null;

        try {
            Class msgListenerClass = DbRaMessageListener.class;
            Class[] paramTypes = {GroupMessage.class};
            onMessageMethod = msgListenerClass.getMethod("onMessage", paramTypes);
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        onMessage = onMessageMethod;
    }

    /**
     * Gets the method used to deliver messages.
     *
     * @return the onMessage method
     */
    public Method getOnMessageMethod() {
        return onMessage;
    }

    /**
     * Called by the Application Server to indicate shutdown is imminent. The
     * Application Server should have undeployed all the message endpoints prior
     * to this call, but the DB-RA will iterate through them and ensure that all
     * the message endpoints are no longer consuming messages.
     */
    public void stop() {
        logger.info("[RA.stop()] Stopping the polling thread");
        try {
            for (Object p : pollingThread) {
                ((PollingThread) p).stopPolling();
            }
        } catch (Exception ex) {
            logger.severe(resource.getString("resourceadapterimpl.noservice"));
        }
    }

    /**
     * Called by the Application Server when a message-driven bean
     * (MessageEndpoint) is deployed. Causes the resource adapter instance to do
     * the necessary setup (setting up message delivery for the message endpoint
     * with a message provider).
     *
     * @param endpointFactory a message endpoint factory instance
     * @param spec an ActivationSpec instance
     *
     * @exception NotSupportedException if message endpoint activation is
     * rejected because of incorrect activation setup information
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory,
            ActivationSpec spec)
            throws NotSupportedException {
        logger.info("[RA.endpointActivation()] Entered");

        try {
            EndpointConsumer ec =
                    new EndpointConsumer(endpointFactory,
                    (ActivationSpecImpl) spec);
            for (Object p : pollingThread) {
                ((PollingThread) p).addEndpointConsumer(endpointFactory, ec);
            }
        } catch (Exception ex) {
            logger.finest("[RA.endpointActivation()] An Exception was caught while activating the endpoint");
            logger.finest("[RA.endpointActivation()] Please check the server logs for details");
            NotSupportedException newEx = new NotSupportedException(
                    java.text.MessageFormat.format(
                    resource.getString(
                    "resourceadapterimpl.endpoint_activation_fail"),
                    new Object[]{ex.getMessage()}));
            newEx.initCause(ex);
            // TODO remove code below
            //throw newEx; // UNCOMMENT THIS LINE TO LET THE SERVER KNOW an error happened
            // Has been commented out to avoid deployment failures during QA testing
            // As a real user/password is required on ejb-jar.xml
        }
    }

    /**
     * Called by Application Server when the MessageEndpoint (message-driven
     * bean) is undeployed. The instance passed as arguments to this method call
     * should be identical to that passed in for the corresponding
     * endpointActivation call. This causes the resource adapter to stop
     * delivering messages to the message endpoint.
     *
     * @param endpointFactory a message endpoint factory instance
     * @param spec an activation spec instance
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory,
            ActivationSpec spec) {
        logger.info("[RA.endpointdeactivation()] Entered");

        for (Object p : pollingThread) {
            ((PollingThread) p).removeEndpointConsumer(endpointFactory);
        }

    }

    /**
     * This method is called by the Application Server on the restart of the
     * Application Server when there are potential pending transactions. For
     * example, it may be called after a server crash. The Application Server
     * requests the XA Resources that correspond to the Activation Specs for the
     * endpoints that it is restarting. It may use those XA Resources to
     * determine transaction status and attempt to commit or rollback.
     *
     * Because this implementation does not support transactions, this method
     * does nothing.
     *
     * @param specs an array of ActivationSpec objects
     *
     * @return an XAResource
     */
    public javax.transaction.xa.XAResource[] getXAResources(ActivationSpec[] specs)
            throws ResourceException {
        /*
         * Do nothing
         */

        return null;
    }
}
