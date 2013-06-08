package ru.nsk.test.db.ra.api;

import javax.mail.Message.*;
import javax.resource.*;

/**
 * Application-level connection handle that is used by a 
 * client component to access an EIS instance.
 * 
 */

public interface DbRaConnection
{
    /**
     * Fetches new records from the database. Application-specific method. 
     *
     * @return an array of messages
     */

    public javax.mail.Message[] getNewMessages()
        throws ResourceException;

    /**
     * Fetches new records from the database. Application-specific 
     * method. 
     *
     * @return a String array of message headers
     */
// TODO rename method
    public String[] getNewMessageHeaders()
        throws ResourceException;

    /**
     * Closes the connection.
     */
    public void close() 
	throws ResourceException;
}
