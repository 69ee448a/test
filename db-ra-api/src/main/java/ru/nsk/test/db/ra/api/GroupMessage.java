package ru.nsk.test.db.ra.api;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

/**
 *
 *
 */
public class GroupMessage implements Serializable {

    private String groupName;
    private Set<GroupMessageItem> items;

    public GroupMessage(String name) {
        groupName = name;
        items = new LinkedHashSet<GroupMessageItem>();
    }

    /**
     * @return the groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @return the items
     */
    public Set<GroupMessageItem> getItems() {
        return items;
    }

    /**
     * @param items the items to set
     */
    public void addItem(Long id, String message) {
        this.items.add(new GroupMessageItem(id, message));
    }
}
