import NetManager from "./NetManager";
import WebSpeakPlayer, { WebSpeakDummyPlayer, WebSpeakLocalPlayer, WebSpeakRemotePlayer } from "./WebSpeakPlayer";
import webSpeakAudio from "./webSpeakAudio";
import webspeakPackets from "./webspeakPackets";

export interface LocalPlayerInfo {
    playerID: string
}

export type WebSpeakVector = [number, number, number]

export interface PlayerTransform {
    pos: WebSpeakVector,
    forward: WebSpeakVector,
    up: WebSpeakVector
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

    /**
     * Sometimes, due to network shit, the client may recieve player updates before the RTC init packet.
     * In this case, store it so it can be applied to the player later.
     */
    public readonly dummyPlayers: Map<String, WebSpeakDummyPlayer> = new Map();

    private readonly _pannerOptions: PannerOptions = webSpeakAudio.defaultPannerOptions;

    get pannerOptions(): Readonly<PannerOptions> {
        return this._pannerOptions;
    }

    updatePlayerTransform(playerID: string, transform: Partial<PlayerTransform>) {
        this.getPlayer(playerID)?.setTransform(transform);
    }

    /**
     * Set the default panner options to use on all panner nodes.
     * @param pannerOptions New panner options.
     */
    setPannerOptions(pannerOptions?: Partial<PannerOptions>) {
        Object.assign(this._pannerOptions, pannerOptions);
        this.onUpdatePannerOptions();
        console.log("Updated panner options: ", pannerOptions)
    }

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
        const dummy = this.dummyPlayers.get(newID);
        if (existing != undefined) {
            this.localPlayer.copyFrom(existing);
            existing.onRemoved();
            this.players.delete(newID);
        } else if (dummy != undefined) {
            this.localPlayer.copyFrom(dummy);
            this.dummyPlayers.delete(newID);
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
     * @param useDummy Make a dummy player if the real player doesn't exist.
     * @returns Either the real player, a dummy, or `undefined`
     */
    getPlayer(playerID?: string, useDummy: boolean = true): WebSpeakPlayer | undefined {
        if (!playerID) {
            return undefined;
        }

        if (playerID === this.localPlayerID) {
            return this.localPlayer;
        } else {
            const player = this.players.get(playerID);
            if (player == undefined && useDummy) {
                return this.getOrMakeDummy(playerID);
            } else {
                return player;
            }
        }
    }
    
    private getOrMakeDummy(playerID: string) {
        let dummy = this.dummyPlayers.get(playerID);
        if (dummy == undefined) {
            console.debug("Making dummy player for " + playerID);
            dummy = new WebSpeakDummyPlayer(playerID);
            this.dummyPlayers.set(playerID, dummy);
        }
        return dummy;
    }

    private makePlayer(playerID: string) {
        let player = new WebSpeakRemotePlayer(playerID, this);
        let dummy = this.dummyPlayers.get(playerID);
        if (dummy) {
            player.copyFrom(dummy);
            this.dummyPlayers.delete(playerID);
        }
        return player;
    }

    requestRTCOffer(playerID: string) { 
        if (this.players.has(playerID)) {
            console.warn("Already connected to player " + playerID);
        }

        let player = this.makePlayer(playerID);
        this.players.set(playerID, player);
        return player.createOffer();
    }

    handleRTCOffer(playerID: string, offer: RTCSessionDescriptionInit) {
        console.debug(`Recieved RTC offer from ${playerID}: `, offer);
        if (this.players.has(playerID)) {
            console.warn("Already connected to player " + playerID);
        }
        let player = this.makePlayer(playerID);
        this.players.set(playerID, player);
        console.log("Connecting to player RTC: " + playerID);
        return player.createAnswer(offer);
    }

    handleRTCAnswer(playerID: string, answer: RTCSessionDescriptionInit) {
        console.debug(`Recieved RTC answer from ${playerID}: `, answer);
        let player = this.players.get(playerID);
        if (!player) {
            throw new Error("Unknown player: " + playerID);
        }
        console.log("Connected to player RTC: " + playerID);
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

    protected onUpdatePannerOptions() {
        for (let player of this.players.values()) {
            player.setPannerOptions(this._pannerOptions);
        }
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
    
    public shutdown() {
        this.netManager.disconnect();
    }
}