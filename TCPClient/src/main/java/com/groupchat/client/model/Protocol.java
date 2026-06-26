package com.groupchat.client.model;

/**
 * Client-side copy of the wire protocol. The server keeps its own identical copy
 * so the two Maven projects stay fully independent and can be built/shipped on
 * their own (no shared module to depend on).
 *
 * @see com.groupchat.client.model.ChatClient
 */
public final class Protocol {

    private Protocol() {
    }

    /** Field separator between the frame type and its payload. */
    public static final String SEP = "|";

    // ---- client -> server ----
    public static final String LOGIN = "LOGIN";
    public static final String MSG = "MSG";
    public static final String USERS = "USERS";
    public static final String BYE = "BYE";

    // ---- server -> client ----
    public static final String WELCOME = "WELCOME";
    public static final String CHAT = "CHAT";
    public static final String INFO = "INFO";

    // ---- commands typed by the user ----
    public static final String CMD_ALL_USERS = "allUsers";
    public static final String CMD_END = "end";
    public static final String CMD_BYE = "bye";

    /** Marker payload identifying a read-only (guest) session. */
    public static final String READ_ONLY = "READONLY";
}
