package com.groupchat.server.model;

/**
 * Observer used by the model to notify the view of state changes.
 * <p>
 * This interface is the <em>only</em> coupling point between the business logic
 * ({@link ChatServer}) and the JavaFX presentation layer. The model never imports
 * JavaFX, satisfying the Separation of Concerns requirement (3.3): the UI can be
 * replaced entirely by providing a different implementation of this interface.
 */
public interface ServerEventListener {

    /** A human-readable activity line was produced (for the log view). */
    void onLog(String message);

    /** A named user successfully joined the chat. */
    void onUserConnected(String username);

    /** A named user left the chat. */
    void onUserDisconnected(String username);
}
