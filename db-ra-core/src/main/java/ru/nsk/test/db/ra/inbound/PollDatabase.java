package ru.nsk.test.db.ra.inbound;

import ru.nsk.test.db.ra.api.GroupMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.*;
import javax.naming.InitialContext;
import javax.resource.spi.InvalidPropertyException;
import javax.sql.DataSource;

/**
 *
 */
public class PollDatabase {

    private String jndiName, table, fieldGroup, fieldItem, fieldMessage;
    private Integer groupLimit;
    private static DataSource dataSource = null;
    private Connection connection = null;
//    PreparedStatement pstSelect = null;
//    PreparedStatement pstDelete = null;
    private static final Logger logger =
            Logger.getLogger(PollDatabase.class.getPackage().getName());

    /**
     * Constructor.
     *
     * @param spec the ActivationSpec for the MDB
     */
    public PollDatabase(ActivationSpecImpl spec)
            throws Exception {
        jndiName = spec.getResource();
        if (jndiName == null || jndiName.isEmpty()) {
            throw new InvalidPropertyException(
                    "Database JNDI name not set or invalid");
        }
        table = spec.getTable();
        if (table == null || table.isEmpty()) {
            throw new InvalidPropertyException("Table name not set or invalid");
        }
        fieldGroup = spec.getFieldGroup();
        if (fieldGroup == null || fieldGroup.isEmpty()) {
            throw new InvalidPropertyException("Field group name not set or invalid");
        }
        groupLimit = spec.getGroupLimit();
        if (groupLimit == null || groupLimit <= 0) {
            throw new InvalidPropertyException("Group limit not set or invalid");
        }
        fieldItem = spec.getFieldItem();
        if (fieldItem == null || fieldItem.isEmpty()) {
            throw new InvalidPropertyException("Field item name not set or invalid");
        }
        fieldMessage = spec.getFieldMessage();
        if (fieldMessage == null || fieldMessage.isEmpty()) {
            throw new InvalidPropertyException("Field message name not set or invalid");
        }

        try {
            open();
        } catch (Exception te) {
            logger.severe("[S] Caught an exception when opening the "
                    + "Folder");
            //te.printStackTrace();
            throw te;
        }
    }

    /**
     * Closes the folder.
     *
     * @exception Exception if the close fails
     */
    public void close() throws Exception {
    }

    /**
     * Opens a connection to the database by JNDI.
     *
     * @exception Exception if the open fails
     */
    private void open() throws Exception {
        try {
            // Get a datasource from application server
            if (dataSource == null) {
                dataSource = (DataSource) new InitialContext().lookup(jndiName);
                logger.fine(MessageFormat.format("Success obtain datasource {0}",
                        dataSource));
            }

            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (Exception te) {
            logger.severe("[MSF] Caught an exception when obtaining a "
                    + "JNDI data source");
            throw te;
        }
    }

    public Set<String> hasNewMessages()
            throws Exception {


        Set<String> groups = new HashSet();
        PreparedStatement pstSelect = null;
        try {
            if (connection.isClosed()) {
                open();
            }
            // should looks like:
            // SELECT group_id FROM message ORDER BY RANDOM() LIMIT 1
            pstSelect = connection.prepareStatement(
                    MessageFormat.format(
                    "SELECT {0} FROM {1} ORDER BY RANDOM() LIMIT {2}",
                    fieldGroup, table, groupLimit));
            ResultSet rs = pstSelect.executeQuery();
            if (logger.isLoggable(Level.FINEST)) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    logger.finest(MessageFormat.format("Record Set column {0} -> {1}",
                            i, rs.getMetaData().getColumnName(i)));
                }
            }
            while (rs.next()) {
                groups.add(rs.getString(1));
            }
            if (groups.size() > 0 && logger.isLoggable(Level.FINEST)) {
                logger.finest(MessageFormat.format("Found {0} new groups.",
                        groups.size()));
                return groups;
            }
        } catch (SQLException e) {
            logger.severe(MessageFormat.format("SQL exception: code {0}, state {1}, msg {2}",
                    e.getErrorCode(), e.getSQLState(), e.getMessage()));
            throw new Exception(e);
        } finally {
            freeResourcses(pstSelect, null, connection);
            logger.finest(MessageFormat.format(
                    "DB poll complete for thread ID {0}, name {1}, group {2}",
                    Thread.currentThread().getId(),
                    Thread.currentThread().getName(),
                    groups.isEmpty() ? null : groups.iterator().next()));
        }
        return null;
    }

    /**
     * Retrieves new messages.
     *
     * @return an array of messages
     */
    public GroupMessage getNewMessages(String groupName)
            throws Exception {

        GroupMessage gm = null;
        PreparedStatement select = null;
        PreparedStatement delete = null;
        try {
            logger.finest(MessageFormat.format(
                    "Trying to get new messages for group {0}.", groupName));
            if (connection.isClosed()) {
                open();
            }

            select = connection.prepareStatement(MessageFormat.format(
                    "SELECT * FROM {0} WHERE {1} = ? ORDER BY {2} FOR UPDATE",
                    table, fieldGroup, fieldItem));
            select.setString(1, groupName);

            delete = connection.prepareStatement(MessageFormat.format(
                    "DELETE FROM {0} WHERE id = ?", table));

            ResultSet rs = select.executeQuery();
            if (logger.isLoggable(Level.FINEST)) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    logger.finest(MessageFormat.format("Record Set column {0} -> {1}",
                            i, rs.getMetaData().getColumnName(i)));
                }
            }
            while (rs.next()) {
                String group = rs.getString(fieldGroup);
                if (gm == null) {
                    gm = new GroupMessage(group);
                }

                Long item = rs.getLong(fieldItem);
                String msg = rs.getString(fieldMessage);
                gm.addItem(item, msg);
                logger.finest(MessageFormat.format("Got record {0}, {1}, {2}",
                        group, item, msg));
                /**
                 * Delete processed item from database. Used very slow (but
                 * reliable) approach for step-by-step record deletion. If
                 * database side can guaranteed that all items in one group will
                 * be writes to database in single transaction then current
                 * step-by-step approach will be changed to fast implementation:
                 * DELETE FROM message WHERE group_id LIKE ? or: DELETE FROM
                 * message WHERE group_id LIKE ? AND id >= group_id_min AND id
                 * <= group_id_max
                 */
                Long id = rs.getLong("id");
                delete.setLong(1, id);
                if (delete.executeUpdate() != 1) {
                    logger.severe(MessageFormat.format(
                            "Cant delete record ID {0}, group {1}, item {2}.",
                            id, group, item));
                } else {
                    logger.finest(MessageFormat.format(
                            "Success delete record ID {0}, group {1}, item {2}.",
                            id, group, item));
                }
            }
        } catch (SQLException e) {
            logger.severe(MessageFormat.format("SQL exception: code {0}, state {1}, msg {2}",
                    e.getErrorCode(), e.getSQLState(), e.getMessage()));
            throw new Exception(e);
        } finally {
            connection.commit();
            freeResourcses(select, delete, connection);
        }
        return gm;
    }

    private void freeResourcses(PreparedStatement select, PreparedStatement delete, Connection con) throws Exception {
        try {
            if (select != null) {
                select.close();
            }
            if (delete != null) {
                delete.close();
            }
            if (con != null) {
                con.commit();
            }
        } catch (Exception e) {
            logger.severe(MessageFormat.format(
                    "Database resource cannot be closed, error {0}.",
                    e.getMessage()));
            throw new Exception(e);
        }
    }
}
