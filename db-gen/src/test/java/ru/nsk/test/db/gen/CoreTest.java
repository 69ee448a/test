package ru.nsk.test.db.gen;

import java.text.MessageFormat;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.junit.Test;
import ru.nsk.test.db.gen.pojo.Message;

/**
 *
 */
public class CoreTest extends Util {
    
    Logger logger = Logger.getLogger(CoreTest.class);
    
    public CoreTest() {
        super();
    }
   
    @Test
    public void testInitDestroy() {
        System.out.println("Init+Destroy");
        Core.init();
        Core.destroy();
    }
    
    @Test
    public void testMergeMessage() {
        System.out.println("MergeMessage");
        
        Message m = new Message();
        m.setGroupId("AUDIT");
        m.setItemId(Math.round(Math.random()));
        m.setMessage("Test record");
        
        Core c = new Core();
        Core.init();
        m = (Message)c.setObject(m);
        logger.info(MessageFormat.format("Saved object {0}", m));
        assertNotNull(m);
        logger.info(MessageFormat.format("Saved object ID {0}", m.getId()));
        assertTrue(m.getId()>0);
        Core.destroy();
    }
}
