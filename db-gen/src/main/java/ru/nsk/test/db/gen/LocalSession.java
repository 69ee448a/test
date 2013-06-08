package ru.nsk.test.db.gen;

import org.hibernate.Session;

class LocalSession {

    private Session session;
    private boolean error;
    private String errorMessage;

    public LocalSession(Session inSession) {
        this.session = inSession;
        this.error = false;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session inSession) {
        this.session = inSession;
    }

    public boolean getError() {
        return error;
    }

    public void setError() {
        error = true;
    }

    public void setError(String message) {
        error = true;
        errorMessage = message;
    }

    public void resetError() {
        error = false;
    }
}