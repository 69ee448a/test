package ru.nsk.test.db.ra.inbound;

import ru.nsk.test.db.ra.api.GroupMessage;
import javax.resource.spi.endpoint.*;

import java.rmi.*;
import java.util.logging.*;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.Set;

import javax.mail.Message.*;
import ru.nsk.test.db.ra.api.DbRaMessageListener;

/**
 * DbRa Client RMI interface.
 *
 * This is a singleton class that represents the Client interface used by the
 * DbRa Service.
 *
 */
public class EndpointConsumer {

    public Method onMessage = null;
    ActivationSpecImpl activationSpec;
    MessageEndpointFactory endpointFactory;
    PollDatabase pollDatabase = null;
    static Logger logger =
            Logger.getLogger(EndpointConsumer.class.getPackage().getName(),
            EndpointConsumer.class.getPackage().getName() + ".LocalStrings");

    /**
     * Constructor. Creates a DbRa Client Interface object and exports it so
     * that the server can access it.
     *
     * @param endpointFactory a MessageEndpointFactory
     * @param activationSpec the activation spec
     */
    public EndpointConsumer(MessageEndpointFactory endpointFactory,
            ActivationSpecImpl activationSpec)
            throws Exception {
        this.endpointFactory = endpointFactory;
        this.activationSpec = activationSpec;
        try {
            pollDatabase = new PollDatabase(activationSpec);
        } catch (Exception ie) {
            logger.log(Level.SEVERE,
                    MessageFormat.format("[EC] Unexpected Error while opening database: {0} check for typos with JNDI resource, table, etc... in ejb-jar.xml, rebuild and redeploy. Root clause: {1}",
                    getUniqueKey(), ie.getMessage()));
            throw ie;
        }
        logger.log(Level.FINEST, "[EC] Created EndpointConsumer for: {0}", getUniqueKey());
    }

    public void deliverMessages(String groupName)
            throws RemoteException {
        try {
            GroupMessage msgs = pollDatabase.getNewMessages(groupName);
            if (msgs != null) {
                deliverMessage(msgs);
            }
        } catch (Exception ie) {
            logger.info("[EC] deliverMessages caught an exception. Bailing out");
            ie.printStackTrace();
        }
    }

    public Set hasNewMessages()
            throws Exception {
        return pollDatabase.hasNewMessages();
    }

    public String getUniqueKey() {
        return activationSpec.getResource() + "::"
                + activationSpec.getTable() + "@"
                + activationSpec.getFieldGroup();
    }

    /**
     * Delivers it to the appropriate EndPoint.
     *
     * @param message the message to be delivered
     */
    private void deliverMessage(GroupMessage messageGroup)
            throws RemoteException {
        MessageEndpoint endpoint = null;

        try {
            // o Create endpoint, passing XAResource.
            // o Call beforeDelivery to allow the appserver
            //   to engage delivery in transaction, if required.
            // o Deliver Message.
            logger.finest(MessageFormat.format(
                    "Trying to delivery {0} items for group {1} to endpoint.",
                    (messageGroup == null ? null
                    : messageGroup.getItems() == null ? null : messageGroup.getItems().size()),
                    (messageGroup == null ? null : messageGroup.getGroupName())));

            if ((endpoint = endpointFactory.createEndpoint(null)) != null) {
                // If this was an XA capable RA then invoke 
                //  endpoint.beforeDelivery();
                ((DbRaMessageListener) endpoint).onMessage(messageGroup);
                logger.finest("##### delivery success");
            } else {
                logger.severe("##### delivery failed");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "messagereceiver.onmessageexception",
                    e);
        } catch (Error error) {
            logger.log(Level.WARNING, "messagereceiver.onmessageexception",
                    error);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "messagereceiver.onmessageexception",
                    t);
        } finally {
            // o Call afterDelivery to to permit the Application Server to 
            //   complete or rollback transaction on  delivery. This should 
            //   occur even if an exception has been thrown.
            // o Call release to indicate the endpoint can be recycled.

            if (endpoint != null) {
                //If this was an XA capable RA then invoke 
                //  endpoint.afterDelivery();
                endpoint.release();
            }
        }
    }
}
