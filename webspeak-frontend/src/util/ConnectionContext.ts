
/**
 * Contains info about the client's current connection to the server.
 */
export default interface ConnectionConfig {
    websocket: WebSocket,
    sessionID: string,

    /**
     * Undefined if the server hasn't yet informed the client about its local player ID.
     */
    localPlayerID?: string
}