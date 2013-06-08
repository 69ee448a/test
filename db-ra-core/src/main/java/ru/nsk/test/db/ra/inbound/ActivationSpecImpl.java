package ru.nsk.test.db.ra.inbound;

import javax.resource.*;
import javax.resource.spi.*;

/**
 * This class implements the Activation Spec class
 * of the database connector (class store MDB-driven RA configuration).
 *
 */

public class ActivationSpecImpl implements javax.resource.spi.ActivationSpec, 
                                           java.io.Serializable
{
    private ResourceAdapter resourceAdapter = null;

    // Database JNDI name
    private String resource = "";
    
    // Table for monitoring
    private String table = "";
    
    // Table field used for query grouping
    private String fieldGroup = "";
    
    // Groups count for one time pooling
    private Integer groupLimit = null;
    
    // Name of table field for item ID
    private String fieldItem = "";
    
    // Name of table field for text message
    private String fieldMessage = "";
    
    /**
     * Constructor. Creates a new instance of the base activation spec.
     */

    public ActivationSpecImpl() { }

    /**
     * Returns the value of the JNDI name property.
     *
     * @return    String containing the value of the JNDI property
     */

    public String getResource() 
    {
        return this.resource;
    }

    /**
     * Sets the value of the JNDI name property.
     *
     * @param serverName  String containing the value to be assigned 
     *                    to JNDI name
     */

    public void setResource(String serverName) 
    {
        this.resource = serverName;
    }
    
    /**
     * Returns the value of the table name.
     *
     * @return    String containing the value of the table property
     */

    public String getTable() 
    {
        return this.table;
    }

    /**
     * Sets the value of the table property.
     *
     * @param protocol    String containing the value to be assigned 
     *                    to table
     */

    public void setTable(String t) 
    {
        this.table = t;
    }

    /**
     * Validates the configuration properties.
     * TBD: verify that a connection to the database can be done
     *
     * @exception    InvalidPropertyException
     */

    public void validate() 
	throws InvalidPropertyException 
    { }

    /**
     * Sets the resource adapter.
     *
     * @param ra  the resource adapter
     */

    public void setResourceAdapter(ResourceAdapter ra)
        throws ResourceException
    {
        this.resourceAdapter = ra;
    }

    /**
     * Gets the resource adapter.
     *
     * @return   the resource adapter
     */
    public ResourceAdapter getResourceAdapter()
    {
        return resourceAdapter;
    }

    /**
     * @return the fieldGroup
     */
    public String getFieldGroup() {
        return fieldGroup;
    }

    /**
     * @param fieldGroup the fieldGroup to set
     */
    public void setFieldGroup(String fieldGroup) {
        this.fieldGroup = fieldGroup;
    }

    /**
     * @return the groupLimit
     */
    public Integer getGroupLimit() {
        return groupLimit;
    }

    /**
     * @param groupLimit the groupLimit to set
     */
    public void setGroupLimit(Integer groupLimit) {
        this.groupLimit = groupLimit;
    }

    /**
     * @return the fieldItem
     */
    public String getFieldItem() {
        return fieldItem;
    }

    /**
     * @param fieldItem the fieldItem to set
     */
    public void setFieldItem(String fieldItem) {
        this.fieldItem = fieldItem;
    }

    /**
     * @return the fieldMessage
     */
    public String getFieldMessage() {
        return fieldMessage;
    }

    /**
     * @param fieldMessage the fieldMessage to set
     */
    public void setFieldMessage(String fieldMessage) {
        this.fieldMessage = fieldMessage;
    }

}
