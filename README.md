# Group Chat Application (TCP + JavaFX)

A real-time group chat built on **Java Sockets (TCP)** and **JavaFX**. A central
server relays messages between many connected clients in a shared chat room.

This repository contains two independent Maven projects, aggregated by a parent POM:

| Module      | Description                                                        |
|-------------|--------------------------------------------------------------------|
| `TCPServer` | JavaFX server: accepts connections, distributes messages, logs activity, shows connected users. |
| `TCPClient` | JavaFX client: logs in with a username, sends/receives messages, runs commands. |

---

## Features

### Client
- **Authentication & identity** — enter a username before chatting.
- **Read-only mode** — connect without a username and you can read messages but not send.
- **Real-time messaging** — type and hit **Enter** or **SEND**.
- **Active user inquiry** — type `allUsers` to get the list of currently connected users.
- **Disconnect** — type `end` or `bye` to leave; the socket is closed and the server is notified.
- **Status indicators** — an *Online / Offline* label and a coloured status dot.

### Server
- **Multiple simultaneous connections** (thread-per-connection).
- **Message distribution** — each message is stamped with the sender's username and time, then broadcast to everyone.
- **Live user list** — a `ListView` of connected usernames, each with a **random background colour** for readability.
- **Activity log** — `Server Started`, `Waiting for Client ...`, `Welcome <user>`, broadcasts, disconnects.

---

## Architecture

The application follows a **Server–Client** model and strictly separates logic from UI
(Separation of Concerns):

```
com.groupchat.server
├── Launcher                 ← plain main(), launches the JavaFX app
├── model/                   ← NO JavaFX imports
│   ├── ChatServer           ← accept loop, broadcast, user registry
│   ├── ClientHandler        ← one per connection (its own thread)
│   ├── ServerConfig         ← loads host/port from server.properties
│   ├── ServerEventListener  ← observer: model → view
│   └── Protocol             ← textual wire protocol
└── view/
    └── ServerApp            ← JavaFX UI; implements ServerEventListener

com.groupchat.client
├── Launcher
├── model/                   ← NO JavaFX imports
│   ├── ChatClient           ← connect, send, background reader thread
│   ├── ClientConfig         ← loads host/port (args > properties)
│   ├── ClientEventListener  ← observer: model → view
│   └── Protocol
└── view/
    └── ClientApp            ← JavaFX UI; implements ClientEventListener
```

The **model** layer knows nothing about JavaFX; it communicates with the **view** only
through the `*EventListener` interfaces. The UI could be swapped (CLI, web, …) without
touching the networking code.

### Concurrency model
The server uses **thread-per-connection**: a dedicated acceptor thread waits on
`ServerSocket.accept()` and gives each new socket its own `ClientHandler` thread. This is
simple and clear for classroom-scale loads. For very large client counts the
non-blocking alternative — a single thread with an NIO `Selector` multiplexing many
`SocketChannel`s — scales better; it is intentionally left as a possible evolution.

### Wire protocol
Newline-delimited UTF-8 text frames of the form `TYPE|payload`:

| Direction        | Frame                       | Meaning                                  |
|------------------|-----------------------------|------------------------------------------|
| client → server  | `LOGIN\|<username>`         | First frame; empty username = read-only  |
| client → server  | `MSG\|<text>`               | Send a chat message                      |
| client → server  | `USERS`                     | Request the active-user list             |
| client → server  | `BYE`                       | Graceful disconnect                      |
| server → client  | `WELCOME\|<username>` / `WELCOME\|READONLY` | Login acknowledged       |
| server → client  | `CHAT\|[time] user: text`   | A message to display                     |
| server → client  | `USERS\|a, b, c`            | Response to `allUsers`                   |
| server → client  | `INFO\|<text>`              | System notice                            |

---

## Configuration

Network settings are loaded **at runtime** (no recompilation needed). Resolution order:

1. command-line arguments (client only): `<serverIp> <port>`
2. an external `server.properties` / `client.properties` placed next to the JAR
3. the bundled properties file
4. built-in defaults (`localhost:3000`)

`TCPServer/src/main/resources/server.properties`
```properties
server.host=0.0.0.0
server.port=3000
```

`TCPClient/src/main/resources/client.properties`
```properties
server.host=localhost
server.port=3000
```

---

## Build

Requires **JDK 21+** and **Maven 3.9+**.

```bash
# Build both modules and produce executable JARs
mvn clean package
```

Artifacts:
- `TCPServer/target/TCPServer.jar`
- `TCPClient/target/TCPClient.jar`

> The JARs bundle the JavaFX libraries for the platform they are built on. Build on the
> OS you intend to run on, or just use `mvn javafx:run` (below), which resolves the right
> JavaFX binaries automatically.

## Run

### Option A — Maven (recommended during development)
```bash
# Terminal 1 — server
mvn -pl TCPServer javafx:run

# Terminal 2 — a client (add more terminals for more clients)
mvn -pl TCPClient javafx:run
```

### Option B — executable JARs
```bash
# Server
java -jar TCPServer/target/TCPServer.jar

# Client (host/port optional; falls back to client.properties)
java -jar TCPClient/target/TCPClient.jar localhost 3000
```

### Trying it out
1. Start the server — the log shows `Server Started` and `Waiting for Client ...`.
2. Start a client, enter a username → you appear in the server's user list with a colour.
3. Start another client, send messages → both clients receive them in real time.
4. Type `allUsers` to see who is online.
5. Start a client and leave the username blank → read-only mode (input disabled).
6. Type `end` or `bye`, or close the window, to disconnect.

---

## Technical architecture (UML)

Diagrams live in [`docs/`](docs/) (Mermaid renders directly on GitHub):

- [Class diagram](docs/class-diagram.md)
- [Deployment diagram](docs/deployment-diagram.md) (Mermaid + PlantUML)
- [Sequence diagram](docs/sequence-diagram.md)
- [Use case diagram](docs/usecase-diagram.md)

---

## Technology stack
- **Language:** Java 21
- **Networking:** Java Sockets (TCP)
- **GUI:** JavaFX (GridPane layout + CSS styling)
- **Build:** Maven (multi-module, `maven-shade-plugin` for executable JARs)
- **IDE:** IntelliJ IDEA
