package com.groupchat.server.model;

/**
 * Tiny line-based wire protocol shared by the server and the client.
 * <p>
 * Every frame is a single UTF-8 text line of the form {@code TYPE|payload}
 * (the payload is optional). Keeping the protocol textual makes the traffic
 * easy to inspect with tools such as {@code telnet} or Wireshark, which is
 * handy when demonstrating the TCP exchange.
 */
public final class Protocol {

    private Protocol() {
    }

    /** Field separator between the frame type and its payload. */
    public static final String SEP = "|";

    // ---- client -> server ----
    /** First frame a client sends: {@code LOGIN|username} (empty username = read-only). */
    public static final String LOGIN = "LOGIN";
    /** A chat message: {@code MSG|text}. */
    public static final String MSG = "MSG";
    /** Request the list of active users: {@code USERS}. */
    public static final String USERS = "USERS";
    /** Graceful disconnect request: {@code BYE}. */
    public static final String BYE = "BYE";

    // ---- server -> client ----
    /** Login acknowledgement: {@code WELCOME|username} or {@code WELCOME|READONLY}. */
    public static final String WELCOME = "WELCOME";
    /** A formatted chat line to display: {@code CHAT|[time] user: text}. */
    public static final String CHAT = "CHAT";
    /** A system/notice line: {@code INFO|text}. */
    public static final String INFO = "INFO";

    // ---- commands typed by the user in the client ----
    public static final String CMD_ALL_USERS = "allUsers";
    public static final String CMD_END = "end";
    public static final String CMD_BYE = "bye";

    /** Marker payload identifying a read-only (guest) session. */
    public static final String READ_ONLY = "READONLY";
}
