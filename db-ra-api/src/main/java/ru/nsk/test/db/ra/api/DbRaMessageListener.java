package ru.nsk.test.db.ra.api;


/**
 * DbRaMessageListener interface implemented by DbRaMessageBean.
 * 
 */

public interface DbRaMessageListener
{
    /**
     * Message-driven bean method invoked by the EJB container.
     *
     * @param message  the incoming message
     */

    public void onMessage(GroupMessage message);
}
