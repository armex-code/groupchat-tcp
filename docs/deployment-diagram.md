# Deployment Diagram

Physical network nodes (one **Server** node, many **Client** nodes) and the TCP/IP
communication links between them.

A Mermaid view is shown first for quick reading. Below it is a **PlantUML** version
using true UML deployment notation (`node` / `artifact`) — render it in IntelliJ with
the PlantUML Integration plugin, or at <https://www.plantuml.com/plantuml>.

## Mermaid (quick view)

```mermaid
flowchart TB
    subgraph ServerNode["🖥️ Server Host"]
        direction TB
        SJVM["JVM"]
        SApp["TCPServer.jar<br/>(JavaFX UI + ChatServer)"]
        SCfg["server.properties<br/>host / port"]
        SApp --- SCfg
        SJVM --- SApp
    end

    subgraph ClientA["💻 Client Host A"]
        CAjar["TCPClient.jar"]
    end
    subgraph ClientB["💻 Client Host B"]
        CBjar["TCPClient.jar"]
    end
    subgraph ClientC["💻 Client Host C (read-only)"]
        CCjar["TCPClient.jar"]
    end

    CAjar -- "TCP/IP socket<br/>port 3000" --> SApp
    CBjar -- "TCP/IP socket<br/>port 3000" --> SApp
    CCjar -- "TCP/IP socket<br/>port 3000" --> SApp
```

## PlantUML (formal UML deployment)

```plantuml
@startuml
title Group Chat — Deployment Diagram

node "Server Host" {
  artifact "TCPServer.jar" as Server
  artifact "server.properties" as SCfg
  Server ..> SCfg : reads
}

node "Client Host A" {
  artifact "TCPClient.jar" as ClientA
}
node "Client Host B" {
  artifact "TCPClient.jar" as ClientB
}
node "Client Host C\n(read-only)" {
  artifact "TCPClient.jar" as ClientC
}

ClientA --> Server : TCP/IP : 3000
ClientB --> Server : TCP/IP : 3000
ClientC --> Server : TCP/IP : 3000
@enduml
```
