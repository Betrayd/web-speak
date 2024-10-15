import SimpleEvent from "./util/SimpleEvent";

/**
 * Recieves and handles packets from the server. Usually, one will exist per app instance.
 */
export default class NetManager {

    /**
     * A callback for when the websocket connection status changes.
     */
    readonly onConnectionStatusChanged = new SimpleEvent<number>();

    readonly onDisconnect = new SimpleEvent<{message: string, errored: boolean}>();

    /**
     * The combined address to use when establishing websocket connection.
     * Example: `http://[serverDomain]/connect?id=[sessionID]`
     */
    readonly connectionAddress: URL;

    /**
     * A map of all packet IDs and functions to handle them.
     */
    readonly packetHandlers: Map<string, (payload: string) => void> = new Map();

    /**
     * Called when there's something wrong with a packet recieved from the server.
     */
    onPacketError: (...err: any) => void = console.error;
    
    /**
     * An optional callback to be notified when a packet is recieved before it is handled.
     * @returns Whether to continue packet execution
     */
    onPacket: (packetID: string, payload: string) => boolean = () => true;
    onWsOpen: (e: Event) => void = e => console.log("Websocket has opened!", e);
    onWsError: (e: Event) => void = console.error;
    onWsClose: (e: CloseEvent) => void = e => console.log("Websocket has closed. ", e.reason);

    private _wsConnection: WebSocket | null = null;
    
    /**
     * The current websocket connection. `undefined` if we haven't connected yet or we've lost connection.
     */
    get wsConnection() {
        return this._wsConnection;
    }

    /**
     * Get the ready state of the underlying connection. 
     * If `wsConnection` is undefined, return `WebSocket.CLOSED`.
     */
    get connectionStatus(): number {
        let ws = this.wsConnection;
        if (ws != null) {
            return ws.readyState;
        } else {
            return WebSocket.CLOSED;
        }
    }
    
    /**
     * Create a net manager instance
     * @param connectionAddress Server connection address
     */
    constructor(connectionAddress: URL) {
        this.connectionAddress = connectionAddress;
        console.log("initializing net manager")
    }
    
    private keepAliveID = -1;

    /**
     * Attempt to connect to the server, using the established connection address.
     */
    public connect() {
        console.log("Attempting connection to " + this.connectionAddress)
        if (this.wsConnection) {
            throw new Error("Already connected to server.");
        }
        let ws = new WebSocket(this.connectionAddress);
        this._wsConnection = ws;

        this.keepAliveID = setInterval(() => {
            this.sendPacket('keepAlive', { timestamp: Date.now() });
        }, 15000)

        ws.onopen = e => {
            this.onWsOpen(e);
            this.onConnectionStatusChanged.dispatch(ws.readyState);
        }
        
        ws.onclose = e => {
            this.onWsClose(e);
            this._wsConnection = null;
            clearInterval(this.keepAliveID);
            this.onConnectionStatusChanged.dispatch(ws.readyState);
            this.onDisconnect.dispatch({ message: e.reason, errored: false });
        }

        ws.onerror = e => {
            this.onWsError(e);
            this._wsConnection = null;
            this.onConnectionStatusChanged.dispatch(ws.readyState);
            this.onDisconnect.dispatch({ message: "An unknown error has occured.", errored: true });
        }

        ws.onmessage = msg => this.onWsMessage(msg);
    }

    protected onWsMessage(msg: MessageEvent) {
        let strData = msg.data as string;
        let index = strData.indexOf(';');
        if (index < 0) {
            this.onPacketError("No semicolon was found to indicate packet type.", strData);
            return;
        }

        let name = strData.substring(0, index);
        let payload = strData.substring(index + 1);

        // If there was an onPacket handler and it returned false.
        if (!this.onPacket(name, payload)) {
            return;
        }

        let handler = this.packetHandlers.get(name);
        if (handler == undefined) {
            this.onPacketError("Unknown packet type: " + name);
            return;
        }

        try {
            handler(payload);
        } catch (err) {
            this.onPacketError(`An error occured while handling packet ${strData}: `, err);
        }
    }

    /**
     * Attempt to send a packet to the server.
     * @param packetID Packet ID string
     * @param payload Packet payload. If not already a string, will be converted to JSON using `JSON.stringify()`.
     * @throws If the server is not connected.
     */
    public sendPacket(packetID: string, payload: string | any) {
        let ws = this.wsConnection;
        if (ws == null || this.connectionStatus !== WebSocket.OPEN) {
            throw new Error("Websocket connection must be open to send packet.");
        }

        if (packetID.indexOf(';') >= 0) {
            throw new Error("Packet ID may not have a semicolon (;).");
        }

        if (typeof(payload) !== 'string') {
            payload = JSON.stringify(payload);
        }
        
        ws.send(packetID + ';' + payload);
    }

    public disconnect() {
        if (this.wsConnection) {
            this.wsConnection.close();
            this._wsConnection = null;
        }
    }
}