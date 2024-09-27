import AppInstance from "./AppInstance";
import webSpeakClient from "./WebSpeakClient";
import webSpeakAudio from "./webSpeakAudio";
import webspeakPackets from "./webspeakPackets";

/**
 * A base class for webspeak players being synced to the client.
 */
export default abstract class WebSpeakPlayer {
    playerID: string;

    constructor(playerID: string) {
        this.playerID = playerID;
    }
    
    /**
     * The player's X coordinate. Make sure to call `onUpdateTransform()` after updating.
     */
    x = 0;

    /**
     * The player's Y coordinate. Make sure to call `onUpdateTransform()` after updating.
     */
    y = 0;

    /**
     * The player's Z coordinate. Make sure to call `onUpdateTransform()` after updating.
     */
    z = 0;

    /**
     * Get the player's position as an array.
     * @returns A 3-element array with the player's X, Y, and Z values.
     */
    getPos() {
        return [this.x, this.y, this.z];
    }

    /**
     * Set the player's position using an array. Make sure to call `onUpdateTransform()` afterwards.
     * @param pos A 3-element array with the player's X, Y, and Z values.
     */
    setPos(pos: number[]) {
        this.x = pos[0];
        this.y = pos[1];
        this.y = pos[2];
    }
    
    /**
     * The player's pitch. Make sure to call `onUpdateTransform()` after updating.
     */
    pitch = 0;

    /**
     * The player's yaw. Make sure to call `onUpdateTransform()` after updating.
     */
    yaw = 0;

    /**
     * The player's roll. Make sure to call `onUpdateTransform()` after updating.
     */
    roll = 0;

    /**
     * Get the player's rotation as an array.
     * @returns A 3-element array with the player's pitch, yaw, and roll values.
     */
    getRot() {
        return [this.pitch, this.yaw, this.roll];
    }

    /**
     * Set the player's rotation using an array. Make sure to call `onUpdateTransform()` afterwards.
     * @param rot A 3-element array with the player's pitch, yaw, and roll values.
     */
    setRot(rot: number[]) {
        this.pitch = rot[0];
        this.yaw = rot[1];
        this.roll = rot[2];
    }

    /**
     * Copy the transform of another player in this. Automatically calls `updateTransform()`.
     * @param other Player to copy the transform of.
     */
    copyTransform(other: WebSpeakPlayer) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;

        this.pitch = other.pitch;
        this.yaw = other.yaw;
        this.roll = other.roll;  
        this.updateTransform();
    }

    /**
     * Called every time the player's transform is updated. 
     * Make sure to call it after you update the transform.
     */
    abstract updateTransform(): void;
    
    /**
     * The type of player this player is.
     */
    abstract get type(): "remote" | "local";
    
    /**
     * Called after the player has been removed from the client for any reason.
     */
    onRemoved() {

    }
        
    isRemote(): this is WebSpeakRemotePlayer {
        return this.type === "remote";
    }

    isLocal(): this is WebSpeakLocalPlayer {
        return this.type === "local";
    }
}

/**
 * A player that is *not* the client's local player. Contains an RTC connection.
 */
export class WebSpeakRemotePlayer extends WebSpeakPlayer {

    readonly app: AppInstance;
    readonly connection: RTCPeerConnection = new RTCPeerConnection(webSpeakClient.rtcConfig);
    readonly panner = new PannerNode(webSpeakAudio.audioCtx, webSpeakAudio.defaultPannerOptions);

    mediaStream?: MediaStream;

    constructor(playerID: string, app: AppInstance) {
        super(playerID);
        this.app = app;
        
        let userMic = webSpeakAudio.userMic;
        if (userMic != undefined && userMic.active) {
            for (let track of userMic.getTracks()) {
                this.connection.addTrack(track, userMic);
            }
        } else {
            console.warn("User mic input was not set.");
        }

        this.connection.ontrack = event => {
            if (event.track.kind === "audio") {
                let mediaStream = event.streams[0];

                // Make chromium jealous of audio source so it can be used in panner
                let bullshitAudio: HTMLAudioElement | null = new Audio();
                bullshitAudio.muted = true;
                bullshitAudio.srcObject = mediaStream;
                bullshitAudio.addEventListener('canplaythrough', () => {
                    bullshitAudio = null;
                })

                let audioStream = webSpeakAudio.audioCtx.createMediaStreamSource(mediaStream);
                audioStream.connect(this.panner);
                this.panner.connect(webSpeakAudio.audioCtx.destination);
            }
        }

        this.connection.onicecandidate = event => {
            if (event.candidate) {
                webspeakPackets.sendReturnIce(app, playerID, event.candidate);
            }
        }
    }

    public addIceCandidate(candidate: RTCIceCandidate) {
        this.connection.addIceCandidate(candidate);
    }

    async createOffer() {
        let offer = await this.connection.createOffer();
        this.connection.setLocalDescription(offer);
        return offer;
    }

    async createAnswer(offer: RTCSessionDescriptionInit) {
        let answer = await this.connection.createAnswer();
        this.connection.setRemoteDescription(offer);
        return answer;
    }

    acceptRTCAnswer(answer: RTCSessionDescriptionInit) {
        this.connection.setRemoteDescription(answer);
    }

    public updateTransform(): void {
        
    }

    get type(): "local" | "remote" {
        return "remote";
    }
}


/**
 * The player that is the client's local player.
 */
export class WebSpeakLocalPlayer extends WebSpeakPlayer {

    updateTransform(): void {
        console.log(`Local transform is (${this.x}, ${this.y}, ${this.z})`);
    }

    get type(): "local" | "remote" {
        return "local";
    }

}
