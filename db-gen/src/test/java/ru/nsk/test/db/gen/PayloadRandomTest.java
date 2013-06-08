package ru.nsk.test.db.gen;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import ru.nsk.test.db.gen.pojo.Message;

/**
 *
 * Test store some predefined amount of messages to payload table. Records
 * writes with random sequence. Next, RA+MDB should read table and process this
 * stored messages.
 */
public class PayloadRandomTest extends Util {

    private final int GROUPS = 1000;
    private final int ITEMSPERGROUP = 100;

    public PayloadRandomTest() {
        super();
    }

    @Test
    public void testPayload() {
        System.out.println("testPayloadRandom");
        long timeStart = System.currentTimeMillis();
        Set<Message> acc = new HashSet(GROUPS * ITEMSPERGROUP);
        Long seed = new Long(1L);
        int count = 10000;
        int blockSize = count / 10;

        for (int i = 0; i < GROUPS; i++) {
            prepare("RandomBlock-" + i, seed, ITEMSPERGROUP, acc);
            seed += ITEMSPERGROUP + 1;
        }

        /**
         * Print all blocks in random order (sorted by Message object reference)
         *
         * Iterator<Message> i = acc.iterator(); while(i.hasNext()) {
         * logger.debug("Message "+i.next()); }
         *
         */
        /**
         * Store random sorted blocks to database.
         */
        int blocks = acc.size() / blockSize;
        for (int i = 0; i < blocks; i++) {
            List block = Arrays
                    .asList(acc.toArray()).subList(0, blockSize);

            store(block);

            acc.removeAll(block);
        }
        assertTrue(acc.isEmpty());

        long elapsed = (System.currentTimeMillis() - timeStart) / 1000;
        logger.info(MessageFormat.format(
                "Stored {0} items in {1} seconds, TPS {2}.",
                GROUPS * ITEMSPERGROUP,
                elapsed, (GROUPS * ITEMSPERGROUP) / elapsed));

        /**
         * Wait while MDB process all payload message.
         */
        waitForEmptyTable(Message.class);
        /**
         * Send audit record, MDB should print statistics for this test.
         */
        store("AUDIT", seed, 1);
        waitForEmptyTable(Message.class);
    }

    private void prepare(String group, Long seed, int count, Set acc) {
        for (long l = seed; l < (seed + count); l++) {
            Message m = new Message();
            m.setGroupId(group);
            m.setItemId(l);
            m.setMessage(MessageFormat.format("Group {0}, item {1}",
                    group, l));
            acc.add(m);
        }
    }
}