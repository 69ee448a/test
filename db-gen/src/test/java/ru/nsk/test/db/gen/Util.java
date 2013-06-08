/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.nsk.test.db.gen;

import java.text.MessageFormat;
import java.util.List;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import org.apache.log4j.Logger;
import ru.nsk.test.db.gen.pojo.Message;

/**
 *
 *
 */
public class Util extends TestCase {

    protected Logger logger = Logger.getLogger(getClass().getName());

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cleanupTable();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        cleanupTable();
    }

    /**
     * Method store N messages (parameter count) to payload table as one group
     * (parameter group). Initial item_id set by 'seed' parameter, subsequent
     * values incremented by one. All records in one group saved in one
     * transaction.
     *
     * @param group
     * @param seed
     * @param count
     */
    protected void store(String group, Long seed, int count) {
        Core c = new Core();
        Core.init();
        for (long l = seed; l < (seed + count); l++) {
            Message m = new Message();
            m.setGroupId(group);
            m.setItemId(l);
            m.setMessage(MessageFormat.format("Group {0}, item {1}",
                    group, l));
            m = (Message) c.setObject(m);
            logger.trace(MessageFormat.format("Saved object {0}", m));
            assertNotNull(m);
            logger.trace(MessageFormat.format("Saved object ID {0}", m.getId()));
            assertTrue(m.getId() > 0);
        }
        Core.destroy();
    }

    /**
     * Method save block of messages in database in one transaction context.
     *
     * @param messages
     */
    protected void store(List messages) {
        Core c = new Core();
        Core.init();
        for (Object o : messages) {
            Message m = (Message) c.setObject((Message) o);
            logger.trace(MessageFormat.format("Saved object {0}", m));
            assertNotNull(m);
            logger.trace(MessageFormat.format("Saved object ID {0}", m.getId()));
            assertTrue(m.getId() > 0);
        }
        Core.destroy();
    }

    /**
     * Wait while RA+MDB process all records in table.
     */
    protected void waitForEmptyTable(Class type) {
        assertNotNull("Entity table type cannot be null", type);
        
        logger.info(MessageFormat.format(
                "Test waiting while table for entity {0} has no records.",
                type.getSimpleName()));
        logger.info("If test freeze too long, please check RA+MDB in application server...");
        
        Core c = new Core();
        try {
            Core.init();

            while (true) {
                if (c.getEntitiesCount(type) == 0) {
                    break;
                }
                Thread.sleep(1000L);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        } finally {
            Core.destroy();
        }
    }

    private void cleanupTable() {
        /**
         * Cleanup messages table.
         */
        Core.init();
        Core c = new Core();
        c.cleanupTable(Message.class);
        Core.destroy();
    }
}
