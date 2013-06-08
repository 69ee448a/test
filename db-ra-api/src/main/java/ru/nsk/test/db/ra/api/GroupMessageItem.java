package ru.nsk.test.db.ra.api;

import java.io.Serializable;

public class GroupMessageItem implements Serializable {

    private Long itemId;
    private String message;

    public GroupMessageItem(Long id, String msg) {
        this.itemId = id;
        this.message = msg;
    }

    /**
     * @return the itemId
     */
    public Long getItemId() {
        return itemId;
    }

    /**
     * @param itemId the itemId to set
     */
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int hashCode() {
        return itemId.intValue() + message==null? 0 : message.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GroupMessageItem other = (GroupMessageItem) obj;
        if (this.itemId != other.itemId && (this.itemId == null || !this.itemId.equals(other.itemId))) {
            return false;
        }
        if ((this.message == null) ? (other.message != null) : !this.message.equals(other.message)) {
            return false;
        }
        return true;
    }
}