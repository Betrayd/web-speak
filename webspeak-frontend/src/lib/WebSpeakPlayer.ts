import AppInstance, { PlayerTransform, WebSpeakVector } from "./AppInstance";
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
    
    x = 0;
    y = 0;
    z = 0;

    /**
     * Get the player's position as an array.
     * @returns A 3-element array with the player's X, Y, and Z values.
     */
    getPos(): WebSpeakVector {
        return [this.x, this.y, this.z];
    }

    /**
     * Set the player's position using an array. Make sure to call `onUpdateTransform()` afterwards.
     * @param pos A 3-element array with the player's X, Y, and Z values.
     */
    setPos(pos: WebSpeakVector) {
        this.x = pos[0];
        this.y = pos[1];
        this.z = pos[2];
    }

    setForward(vec: WebSpeakVector) {
        this.forX = vec[0];
        this.forY = vec[1];
        this.forZ = vec[2];
    }

    forX = 0;
    forY = 1;
    forZ = 0;

    setUp(vec: WebSpeakVector) {
        this.upX = vec[0];
        this.upY = vec[1];
        this.upZ = vec[2];
    }

    upX = 0;
    upY = 0;
    upZ = 1;

    /**
     * Copy the transform of another player in this. Automatically calls `updateTransform()`.
     * @param other Player to copy the transform of.
     */
    copyTransform(other: WebSpeakPlayer) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;

        this.forX = other.forX;
        this.forY = other.forY;
        this.forZ = other.forZ;

        this.upX = other.upX;
        this.upY = other.upY;
        this.upZ = other.upZ;

        this.updateTransform();
    }

    /**
     * Apply a player transform to this player. Automatically calls `updateTransform()`.
     * @param transform Transform to apply.
     */
    setTransform(transform: Partial<PlayerTransform>) {
        if (transform.pos) {
            this.x = transform.pos[0];
            this.y = transform.pos[1];
            this.z = transform.pos[2];
        }

        if (transform.forward) {
            this.forX = transform.forward[0];
            this.forY = transform.forward[1];
            this.forZ = transform.forward[2];
        }

        if (transform.up) {
            this.upX = transform.up[0];
            this.upY = transform.up[1];
            this.upZ = transform.up[2];
        }

        this.updateTransform();
    }

    /**
     * Called every time the player's transform is updated. 
     * Make sure to call it after you update the transform.
     */
    abstract updateTransform(): void;
    
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
    // readonly panner = new PannerNode(webSpeakAudio.audioCtx as AudioContext, webSpeakAudio.defaultPannerOptions);
    private audioStream?: MediaStreamAudioSourceNode;
    readonly panner: PannerNode;

    mediaStream?: MediaStream;

    private _muted: boolean = false;

    public set muted(muted: boolean) {
        this._muted = muted;
        this.onSetMuted(muted);
    }

    private onSetMuted(muted: boolean) {
        if (!this.mediaStream) return;
        this.mediaStream.getTracks().forEach(track => {
            track.enabled = !muted;
        });
    }

    public get muted() {
        return this._muted;
    }

    private _spatialized = true;

    public set spatialized(spatialized: boolean) {
        this.spatialized = spatialized;
        // TODO: actually make this do something.
    }

    public get spatialized() {
        return this._spatialized;
    }
    
    /**
     * Construct a webspeak player and a panner for it. Panner options will be supplied bu the app.
     * @param playerID  Player ID to use.
     * @param app App to base panner options off of and to use for WS communication.
     */
    constructor(playerID: string, app: AppInstance) {
        super(playerID);
        this.app = app;
        this.panner = new PannerNode(webSpeakAudio.getAudioCtx(), app.pannerOptions);
        
        let userMic = webSpeakAudio.userMic;
        if (userMic != undefined && userMic.active) {
            for (let track of userMic.getTracks()) {
                this.connection.addTrack(track, userMic);
            }
        } else {
            console.warn("User mic input was not set.");
        }

        this.connection.ontrack = event => {
            let audioCtx = webSpeakAudio.audioCtx as AudioContext;
            if (event.track.kind === "audio") {
                let mediaStream = event.streams[0];

                let x = new MediaStream();
                event.streams[0].getTracks().forEach((track) => {
                    x.addTrack(track);
                });
                
                // Make chromium jealous of audio source so it can be used in panner
                let bullshitaudio : HTMLAudioElement | null;
                bullshitaudio = new Audio();
                bullshitaudio.muted = true;
                bullshitaudio.srcObject = mediaStream;
                bullshitaudio.addEventListener('canplaythrough', () => {
                    bullshitaudio = null;
                });
                
                this.audioStream = audioCtx.createMediaStreamSource(mediaStream);
                this.audioStream.connect(this.panner);
                // Update muted status
                this.onSetMuted(this.muted);
  
                this.panner.connect(audioCtx.destination);
            } else {
                console.error("IT WAS THE WRONG TYPE OH NOOOOOOOO");
            }
        }

        this.connection.onicecandidate = event => {
            if (event.candidate) {
                console.debug("Sending ICE candidate: ", event.candidate);
                webspeakPackets.sendReturnIce(app, playerID, event.candidate);
            }
        }
    }

    private _pannerOptions: Partial<PannerOptions> = {...webSpeakAudio.defaultPannerOptions};

    public get pannerOptions(): Readonly<Partial<PannerOptions>> {
        return this._pannerOptions;
    }
    
    private _pannerOptionsOverride?: Partial<PannerOptions>;

    /**
     * If set, these values will override any panner options set.
     */
    public get pannerOptionsOverride(): Readonly<PannerOptions> | undefined {
        return this._pannerOptionsOverride;
    }
    
    public setPannerOptionsOverride(override?: Partial<PannerOptions>) {
        if (override) {
            this._pannerOptionsOverride = {...override};
        } else {
            this._pannerOptionsOverride = undefined;
        }
        this.setPannerOptionsInternal(this._pannerOptions);
        if (override) {
            this.setPannerOptionsInternal(override);
        }
    }

    public setPannerOptions(options: Partial<PannerOptions>) {
        this._pannerOptions = {...options};
        this.setPannerOptionsInternal(options);
        if (this.pannerOptionsOverride) {
            this.setPannerOptionsInternal(this.pannerOptionsOverride);
        }
    }

    private setPannerOptionsInternal(options: Partial<PannerOptions>) {
        if (options.coneInnerAngle != undefined)
            this.panner.coneInnerAngle = options.coneInnerAngle;
        if (options.coneOuterAngle != undefined)
            this.panner.coneOuterAngle = options.coneOuterAngle;
        if (options.coneOuterGain != undefined)
            this.panner.coneOuterGain = options.coneOuterGain;
        if (options.distanceModel != undefined)
            this.panner.distanceModel = options.distanceModel;
        if (options.maxDistance != undefined)
            this.panner.maxDistance = options.maxDistance;
        if (options.panningModel != undefined)
            this.panner.panningModel = options.panningModel;
        if (options.refDistance != undefined)
            this.panner.refDistance = options.refDistance;
        if (options.rolloffFactor != undefined)
            this.panner.rolloffFactor = options.rolloffFactor;
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
        this.connection.setRemoteDescription(offer);
        let answer = await this.connection.createAnswer();
        this.connection.setLocalDescription(answer);
        return answer;
    }

    acceptRTCAnswer(answer: RTCSessionDescriptionInit) {
        this.connection.setRemoteDescription(answer);
    }

    disconnect() {
        this.connection.close();
    }

    public updateTransform(): void {
        if (webSpeakAudio.audioCtx == undefined) {
            return;
        }
        if (this.panner.positionX) {
            let currentTime = webSpeakAudio.audioCtx.currentTime;
            this.panner.positionX.setValueAtTime(this.x, currentTime);
            this.panner.positionY.setValueAtTime(this.y, currentTime);
            this.panner.positionZ.setValueAtTime(this.z, currentTime);
        }
        else {
            this.panner.setPosition(this.x, this.y, this.z);
        }
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
        if (webSpeakAudio.audioCtx == undefined) {
            return;
        }
        const listener = webSpeakAudio.audioCtx.listener;

        if (listener.positionX) {
            listener.positionX.value = this.x;
            listener.positionY.value = this.y;
            listener.positionZ.value = this.z;
        } else {
            listener.setPosition(this.x, this.y, this.z);
        }

        if (listener.forwardX) {
            listener.forwardX.value = this.forX;
            listener.forwardY.value = this.forY;
            listener.forwardZ.value = this.forZ;
            listener.upX.value = this.upX;
            listener.upY.value = this.upY;
            listener.upZ.value = this.upZ;
        } else {
            listener.setOrientation(this.forX, this.forY, this.forZ,
                this.upX, this.upY, this.upZ);
        }
    }

    get type(): "local" | "remote" {
        return "local";
    }

}
