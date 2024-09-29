import NetManager from "./NetManager";
import WebSpeakPlayer, { WebSpeakLocalPlayer, WebSpeakRemotePlayer } from "./WebSpeakPlayer";
import webspeakPackets from "./webspeakPackets";

export interface LocalPlayerInfo {
    playerID: string
}

/**
 * An instance of the app, keeping all the relevent data for a connection to a WebSpeak server.
 */
export default class AppInstance {
    readonly serverAddress: string;
    readonly sessionID: string;

    readonly netManager: NetManager;

    /**
     * All the players that this client knows about.
     */
    readonly players: Map<String, WebSpeakRemotePlayer> = new Map();
    readonly localPlayer = new WebSpeakLocalPlayer("");

    get localPlayerID() {
        return this.localPlayer.playerID;
    }

    /**
     * Set a player ID as the local player ID. 
     * If a player already exists with that ID, copy its transform and remove it from the general pool.
     * @param newID new ID to use.
     */
    setLocalPlayerID(newID: string) {
        if (newID === this.localPlayerID) return;

        const existing = this.players.get(newID);
        if (existing != undefined) {
            this.localPlayer.copyTransform(existing);
            existing.onRemoved();
            this.players.delete(newID);
        }

        this.localPlayer.playerID = newID;
        console.log("Set local player ID as " + newID);
    }

    get connectionStatus() {
        return this.netManager.connectionStatus;
    }

    /**
     * Attempt to find a player by its ID.
     * @param playerID Player ID to search for.
     * @returns The player, or `undefined` if the client's not tracking a player with that ID.
     */
    getPlayer(playerID: string): WebSpeakPlayer | undefined {
        if (playerID === this.localPlayerID) {
            return this.localPlayer;
        } else {
            return this.players.get(playerID);
        }
    }

    requestRTCOffer(playerID: string) {
        if (this.players.has(playerID)) {
            console.warn("Already connected to player " + playerID);
        }

        let player = new WebSpeakRemotePlayer(playerID, this);
        this.players.set(playerID, player);
        return player.createOffer();
    }

    handleRTCOffer(playerID: string, offer: RTCSessionDescriptionInit) {
        if (this.players.has(playerID)) {
            console.warn("Already connected to player " + playerID);
        }

        let player = new WebSpeakRemotePlayer(playerID, this);
        this.players.set(playerID, player);
        return player.createAnswer(offer);
    }

    handleRTCAnswer(playerID: string, answer: RTCSessionDescriptionInit) {
        let player = this.players.get(playerID);
        if (!player) {
            throw new Error("Unknown player: " + playerID);
        }

        player.acceptRTCAnswer(answer);
    }

    disconnectPlayerRTC(playerID: string) {
        let player = this.players.get(playerID);
        if (player == undefined) {
            console.warn("Recieved disconnect RTC packet for unknown player: " + playerID);
            return;
        }
        console.log("Disconnecting player from RTC: " + playerID)
        player.disconnect();
        this.players.delete(playerID);
    }

    constructor(serverAddress: string, sessionID: string) {
        this.serverAddress = serverAddress;
        this.sessionID = sessionID;

        this.netManager = new NetManager(new URL(`${serverAddress}/connect?id=${sessionID}`))
        webspeakPackets.setupPacketListeners(this);
    }

    public connect() {
        this.netManager.connect();
    }
    
}