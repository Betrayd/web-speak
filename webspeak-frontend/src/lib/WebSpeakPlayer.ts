import AppInstance, { PlayerTransform, WebSpeakVector } from "./AppInstance";
import webSpeakClient from "./WebSpeakClient";
import rtcPackets from "./packets/rtcPackets";
import webSpeakAudio from "./webSpeakAudio";

export interface AudioModifier {
    silenced: boolean,
    spatialized: boolean
}

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
     * Copy the values from another player into this. Automatically calls `updateTransform()`.
     * @param other Player to copy the values of.
     */
    copyFrom(other: WebSpeakPlayer) {
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
        this.audioModifier = other.audioModifier;
        // this.spatialized = other.spatialized;
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
    
    abstract get type(): "remote" | "local" | "dummy";
    
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

    private _audioModifier: AudioModifier = {
        spatialized: true,
        silenced: false
    };

    get audioModifier() {
        return this._audioModifier as Readonly<AudioModifier>;
    }

    set audioModifier(modifier: AudioModifier) {
        this._audioModifier = {...modifier};
        this.onSetAudioModifier(this._audioModifier);
    }

    /**
     * Apply an audio modifier on top of the current audio modifier.
     * @param modifier Partial audio modifier to apply.
     */
    public applyAudioModifier(modifier: Partial<AudioModifier>) {
        if (modifier.spatialized !== undefined) {
            this._audioModifier.spatialized = modifier.spatialized;
        }
        if (modifier.silenced !== undefined) {
            this._audioModifier.silenced = modifier.silenced;
        }
        this.onSetAudioModifier(this._audioModifier);
    }

    protected onSetAudioModifier(_modifier: Readonly<AudioModifier>) {};
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

    private readonly pannerGain: GainNode;
    private readonly directGain: GainNode;
    private readonly masterGain: GainNode;

    mediaStream?: MediaStream;


    public get gain() {
        return this.masterGain.gain.value;
    }

    public set gain(value: number) {
        this.masterGain.gain.value = value;
    }
    
    /**
     * Construct a webspeak player and a panner for it. Panner options will be supplied bu the app.
     * @param playerID  Player ID to use.
     * @param app App to base panner options off of and to use for WS communication.
     */
    constructor(playerID: string, app: AppInstance) {
        super(playerID);
        this.app = app;
        const audioManager = webSpeakAudio.getAudioManagerOrThrow();
        const audioCtx = audioManager.audioCtx;

        // SETUP AUDIO NODES
        this.panner = new PannerNode(audioCtx, app.pannerOptions);

        this.pannerGain = audioCtx.createGain();
        this.directGain = audioCtx.createGain();
        this.directGain.gain.value = 0;

        this.masterGain = audioCtx.createGain();

        this.pannerGain.connect(this.panner);
        this.panner.connect(this.masterGain);
        this.directGain.connect(this.masterGain);
        this.masterGain.connect(audioManager.outputNode);

        this.directGain.gain.value = 0;
        
        let userMic = webSpeakAudio.getAudioManagerOrThrow().userMic;
        if (userMic?.active) {
            for (let track of userMic.getTracks()) {
                this.connection.addTrack(track, userMic);
            }
        } else {
            console.warn("User mic input was not set.");
        }

        this.connection.ontrack = event => {
            // let audioCtx = webSpeakAudio.audioCtx as AudioContext;
            const audioManager = webSpeakAudio.getAudioManagerOrThrow();
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
                
                // this.audioStream = audioCtx.createMediaStreamSource(mediaStream);
                this.audioStream = audioManager.audioCtx.createMediaStreamSource(mediaStream);

                // Swap between panner node and direct to toggle spatialization
                this.audioStream.connect(this.pannerGain);
                this.audioStream.connect(this.directGain);
                this.mediaStream = mediaStream;

                // Update muted status
                this.setMuted(this.shouldMute());
                this.setSpatialized(this.shouldSpatialize());
            } else {
                console.error("IT WAS THE WRONG TYPE OH NOOOOOOOO");
            }
        }

        this.connection.onicecandidate = event => {
            if (event.candidate) {
                console.debug("Sending ICE candidate: ", event.candidate);
                rtcPackets.sendReturnIce(app, playerID, event.candidate);
            }
        }
    }

    protected onSetAudioModifier(modifier: Readonly<AudioModifier>): void {
        super.onSetAudioModifier(modifier);
        this.setMuted(this.shouldMute());
        this.setSpatialized(this.shouldSpatialize());
        console.log("Updated audio modifier for player " + this.playerID, modifier);
    }

    /**
     * Determine whether the audio source should mute based 
     * on the audio modifier and local player's audio settings.
     * 
     * @returns `true` if the audio source should be muted.
     */
    public shouldMute() {
        return this.audioModifier.silenced;
    }
    
    /**
     * Determine whether the audio source should be spatialized 
     * based on the audio modifier.
     * 
     * @returns `true` if the audio source should be spatialized.
     */
    public shouldSpatialize() {
        return this.audioModifier.spatialized;
    }
    // protected get shouldSpatialize(): boolean {
    //     return this.audioModifier.spatialized;
    // }
    
    private setMuted(muted: boolean) {
        if (!this.mediaStream) return;
        this.mediaStream.getTracks().forEach(track => {
            track.enabled = !muted;
        })
    }

    private setSpatialized(spatialized: boolean) {
        if (spatialized) {
            this.pannerGain.gain.value = 1;
            this.directGain.gain.value = 0;
        } else {
            this.pannerGain.gain.value = 0;
            this.directGain.gain.value = 1;
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
        console.debug("created RTC offer:", offer)
        return offer;
    }

    async createAnswer(offer: RTCSessionDescriptionInit) {
        this.connection.setRemoteDescription(offer);
        let answer = await this.connection.createAnswer();
        this.connection.setLocalDescription(answer);
        console.debug("created RTC answer", answer)
        return answer;
    }

    acceptRTCAnswer(answer: RTCSessionDescriptionInit) {
        this.connection.setRemoteDescription(answer);
    }

    /**
     * Disconnect and remove listeners related to this player.
     */
    disconnect() {
        this.connection.close();
    }

    public updateTransform(): void {
        const audioManager = webSpeakAudio.getAudioManager();
        if (audioManager == undefined) {
            return;
        }
        if (this.panner.positionX) {
            let currentTime = audioManager.audioCtx.currentTime;
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
        const audioManager = webSpeakAudio.getAudioManager();
        if (audioManager == undefined) {
            return;
        }
        const listener = audioManager.audioCtx.listener;

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

/**
 * A dummy webspeak player for when values that need to be stored
 * for players that are not connected.
 */
export class WebSpeakDummyPlayer extends WebSpeakPlayer {
    updateTransform(): void {

    }
    get type(): "local" | "remote" | "dummy" {
        return "dummy";
    }
    
}