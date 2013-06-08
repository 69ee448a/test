package ru.nsk.test.db.ra.api;

import javax.mail.Message.*;
import javax.resource.*;
import javax.resource.cci.*;


/**
 * Interface for obtaining database connections. 
 * 
 */

public interface DbRaConnectionFactory
{
    /**
     * Gets a connection to the database.
     * Passes along database server and user info.
     *
     * @return Connection	Connection instance
     */

    public DbRaConnection createConnection()
        throws ResourceException;

    /**
     * Gets a connection to a database instance. A component should use 
     * the getConnection variant with a javax.resource.cci.ConnectionSpec 
     * parameter if it needs to pass any resource-adapter-specific security 
     * information and connection parameters.
     *
     * @param properties  connection parameters and security information 
     *                    specified as ConnectionSpec instance
     * @return  a DbRaConnection instance
     */

    public DbRaConnection createConnection(ConnectionSpec properties)
        throws ResourceException;
}
