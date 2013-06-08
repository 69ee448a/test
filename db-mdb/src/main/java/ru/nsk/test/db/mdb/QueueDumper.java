/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.nsk.test.db.mdb;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import ru.nsk.test.db.ra.api.DbRaMessageListener;
import ru.nsk.test.db.ra.api.GroupMessage;
import ru.nsk.test.db.ra.api.GroupMessageItem;

/**
 *
 */
@MessageDriven(mappedName = "db-ra/DbRaMdb")
public class QueueDumper implements DbRaMessageListener {

    private final static Logger logger = Logger.getLogger(
            QueueDumper.class.getName());
    private static volatile Map<String, Audit> audit = new HashMap(1024);
    private static volatile long timeStart;
    private static volatile long timeEnd;
    @Resource(name = "command.audit")
    protected String commandAudit;

    public QueueDumper() {
    }

    @Override
    public void onMessage(GroupMessage message) {
        String group = null;
        int length = 0;
        if (message != null) {
            group = message.getGroupName();
            if (message.getItems() != null) {
                length = message.getItems().size();
            }
        }

        logger.finest(MessageFormat.format("MDB got message {0}, group {1}, items {2}.",
                message, group, length));
        if (length > 0) {
            for (GroupMessageItem record : message.getItems()) {
                logger.finest(MessageFormat.format("Group {0}, item ID {1}, text {2}.",
                        group, record.getItemId(), record.getMessage()));
            }
        }
        addToAudit(message);
        checkForAudit(message);
    }

    private void addToAudit(GroupMessage message) {
        if (message == null || message.getItems() == null) {
            logger.warning("Null value message (or message payload item) ignored.");
            return;
        }
        if (commandAudit == null || commandAudit.equalsIgnoreCase(message.getGroupName())) {
            return; // do not add audit command to global counters
        }
        synchronized (audit) {
            if(audit.isEmpty()) {
                timeStart = System.currentTimeMillis();
            }
            Audit groupAudit = audit.get(message.getGroupName());
            if (groupAudit == null) {
                groupAudit = new Audit(message.getGroupName());
            }
            groupAudit.checkItemOrder(message.getItems());
            groupAudit.incrementCounter(message.getItems().size());
            audit.put(message.getGroupName(), groupAudit);
            timeEnd = System.currentTimeMillis();
        }
    }

    private void checkForAudit(GroupMessage message) {
        if (message == null || message.getGroupName() == null) {
            logger.warning("Null value message (or message group name) ignored.");
            return;
        }
        if (commandAudit == null || !commandAudit.equalsIgnoreCase(message.getGroupName())) {
            return; // its not audit record, ignore this
        }
        Long total = 0L;
        synchronized (audit) {
            Iterator<String> i = audit.keySet().iterator();
            while (i.hasNext()) {
                String group = i.next();
                Audit groupAudit = audit.get(group);
                total += groupAudit.getCounter();
                logger.info(MessageFormat.format(
                        "AUDIT -> Processed group {0} in {1} parts,"
                        + " total {2} items, group elapse {3} msec.",
                        group,
                        groupAudit.getParts(),
                        groupAudit.getCounter(),
                        groupAudit.getGroupTimeElapseInMsec()));
                i.remove();
            }
        }
        float elapsed = (timeEnd - timeStart) / 1000;
        elapsed = elapsed == 0 ? Float.MIN_VALUE : elapsed;
        logger.info(MessageFormat.format(
                "AUDIT -> total in all groups: {0} items, processed in {1} sec, TPS {2}.",
                total, elapsed, total / elapsed));
    }
}

class Audit {

    private String groupName;
    private Integer itemsCounter;
    private Integer parts;
//    private Long latestItemId;
    private Long timestampFirst;
    private Long timestampLast;
    private static final Logger logger = Logger.getLogger(Audit.class.getName());

    public Audit(String groupName) {
        this.itemsCounter = 0;
        this.parts = 0;
        this.groupName = groupName;
//        this.latestItemId = null;
        this.timestampFirst = this.timestampLast = System.currentTimeMillis();
    }

    /**
     * @return the groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @return the latestItemId
     */
//    public Long getLatestItemId() {
//        return latestItemId;
//    }

    /**
     * Method check items ordering in message. Set of items should be not null,
     * not empty and contains items in ascending orders. If this requirements
     * breaks, then method write error message to system log.
     * 
     * @param items Set or message items. 
     */
    public void checkItemOrder(Set<GroupMessageItem> items) {
        if(items==null || items.isEmpty()) {
            logger.severe(MessageFormat.format(
                    "Items block in group {0} is nullpointer or empty set.",
                    groupName));
            return;
        }
        Long latestItemId = null;
        for(GroupMessageItem i:items) {
            Long id = i.getItemId();
            if(latestItemId==null) {
                latestItemId = id;
                continue;
            }
            if(latestItemId>id) {
                logger.severe(MessageFormat.format(
                    "Items in group {0} had invalid ordering. "
                        + "Previous item ID is {1}, current item ID is {2}.",
                    groupName, latestItemId, id));
                continue;
            }
            latestItemId = id;
        }
        parts++;
        timestampLast = System.currentTimeMillis();
    }

    /**
     * @return the timestampFirst
     */
    public Long getTimestampFirst() {
        return timestampFirst;
    }

    /**
     * @return the timestampLast
     */
    public Long getTimestampLast() {
        return timestampLast;
    }

    /**
     * @return void
     */
    public void incrementCounter(Integer value) {
        synchronized (itemsCounter){
            itemsCounter += value;
        }
    }

    /**
     * @return Current counter value
     */
    public Integer getCounter() {
        return this.itemsCounter;
    }

    /**
     * @return the parts
     */
    public Integer getParts() {
        return parts;
    }
    
    public long getGroupTimeElapseInMsec() {
        return timestampLast - timestampFirst;
    }
}
