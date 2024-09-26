import NetManager from "./NetManager";

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

    onSetLocalPlayerInfo: (info: LocalPlayerInfo) => void = () => {};

    private _localPlayerInfo: LocalPlayerInfo = {
        playerID: ""
    };

    get localPlayerInfo() {
        return this._localPlayerInfo;
    }

    set localPlayerInfo(info: LocalPlayerInfo) {
        this._localPlayerInfo = info;
        this.onSetLocalPlayerInfo(info);
    }

    constructor(serverAddress: string, sessionID: string) {
        this.serverAddress = serverAddress;
        this.sessionID = sessionID;

        this.netManager = new NetManager(new URL(`${serverAddress}/connect?id=${sessionID}`))
    }

    protected setupPacketListeners() {
        this.netManager.packetHandlers.set('localPlayerInfo', payload => {
            this.localPlayerInfo = JSON.parse(payload);
            console.log(`New player ID is ${this.localPlayerInfo.playerID}`)
        })
    }
}