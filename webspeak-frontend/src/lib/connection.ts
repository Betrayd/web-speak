// Keeps track of client connection to server.

export interface ConnectionState {
    websocket: WebSocket,
    sessionID: string
}
