package com.groupchat.client.model;

/**
 * Observer through which {@link ChatClient} reports network events to the view.
 * <p>
 * This is the sole coupling point between the client's business logic and its
 * JavaFX interface, keeping the model free of any UI dependency (Separation of
 * Concerns, requirement 3.3).
 */
public interface ClientEventListener {

    /** Login was acknowledged. {@code readOnly} reflects the granted access level. */
    void onConnected(String displayName, boolean readOnly);

    /** A chat line (already formatted by the server) arrived. */
    void onMessage(String text);

    /** Response to an {@code allUsers} request: comma-separated usernames. */
    void onUsersList(String csv);

    /** A system/notice line arrived (e.g. read-only rejection). */
    void onInfo(String text);

    /** The connection to the server was closed. */
    void onDisconnected();
}
