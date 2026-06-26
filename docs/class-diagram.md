# Class Diagram

Software structure and relationships. The **model** packages contain all logic and
socket handling; the **view** packages contain only JavaFX code and talk to the model
through the listener interfaces — enforcing the Separation of Concerns requirement.

```mermaid
classDiagram
    direction LR

    %% ===== Server side =====
    class ServerLauncher {
        +main(String[] args)
    }
    class ServerApp {
        -TextArea logArea
        -ObservableList~String~ users
        -Map~String,String~ userColors
        -ChatServer server
        +start(Stage)
        +onLog(String)
        +onUserConnected(String)
        +onUserDisconnected(String)
    }
    class ServerEventListener {
        <<interface>>
        +onLog(String)
        +onUserConnected(String)
        +onUserDisconnected(String)
    }
    class ChatServer {
        -int port
        -List~ClientHandler~ clients
        -ServerSocket serverSocket
        +start()
        +stop()
        +broadcast(String, String)
        +activeUsernames() List~String~
    }
    class ClientHandler {
        -Socket socket
        -String username
        -boolean readOnly
        +run()
        +sendChat(String)
        +close()
    }
    class ServerConfig {
        -String host
        -int port
        +load()$ ServerConfig
    }
    class ServerProtocol["Protocol"] {
        <<utility>>
        +LOGIN, MSG, USERS, BYE
        +WELCOME, CHAT, INFO
    }

    ServerLauncher ..> ServerApp : launches
    ServerApp ..|> ServerEventListener
    ServerApp --> ChatServer : starts & observes
    ChatServer o-- "0..*" ClientHandler : manages
    ChatServer ..> ServerEventListener : notifies
    ChatServer ..> ServerConfig : configured by
    ClientHandler ..> ServerProtocol
    ChatServer ..> ServerProtocol

    %% ===== Client side =====
    class ClientLauncher {
        +main(String[] args)
    }
    class ClientApp {
        -TextArea chatArea
        -TextField input
        -Button sendButton
        -Circle statusDot
        -ChatClient client
        +start(Stage)
        +onMessage(String)
        +onConnected(String, boolean)
        +onDisconnected()
    }
    class ClientEventListener {
        <<interface>>
        +onConnected(String, boolean)
        +onMessage(String)
        +onUsersList(String)
        +onInfo(String)
        +onDisconnected()
    }
    class ChatClient {
        -String host
        -int port
        -Socket socket
        -boolean readOnly
        +connect(String)
        +handleInput(String)
        +disconnect()
    }
    class ClientConfig {
        -String host
        -int port
        +load(String[])$ ClientConfig
    }
    class ClientProtocol["Protocol"] {
        <<utility>>
        +LOGIN, MSG, USERS, BYE
        +WELCOME, CHAT, INFO
    }

    ClientLauncher ..> ClientApp : launches
    ClientApp ..|> ClientEventListener
    ClientApp --> ChatClient : drives & observes
    ChatClient ..> ClientEventListener : notifies
    ChatClient ..> ClientConfig : configured by
    ChatClient ..> ClientProtocol

    %% ===== Network link =====
    ChatClient ..> ClientHandler : TCP socket
```
