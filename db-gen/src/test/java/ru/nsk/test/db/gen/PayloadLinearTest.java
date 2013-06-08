package ru.nsk.test.db.gen;

import org.junit.Test;
import ru.nsk.test.db.gen.pojo.Message;

/**
 *
 * Test store some predefined amount of messages to payload table. Next, RA+MDB
 * should read table and process this stored messages.
 */
public class PayloadLinearTest extends Util {

    public PayloadLinearTest() {
        super();
    }

    @Test
    public void testPayload() {
        System.out.println("testPayloadLinear");

        store("LinearTest", 1024L, 100);
        waitForEmptyTable(Message.class);
        store("AUDIT", 1024L+100L, 1);
        waitForEmptyTable(Message.class);
    }
}