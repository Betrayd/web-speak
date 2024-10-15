import SimpleEvent from "./util/SimpleEvent";

/**
 * Contains global methods related to audio processing.
 * This is not connected to the app instance so it can be easily accessed globally.
 */
module webSpeakAudio {

    /**
     * Keeps track of audio events that only get activated once the user accepts 
     * (or denies) mic permissions.
     */
    export class WSAudioManager {
        /**
         * The users mic. `null` if they denied mic permissions.
         */
        public readonly userMic: MediaStream | null;

        /**
         * The primary audio context.
         */
        public readonly audioCtx: AudioContext

        private readonly _outputNode: GainNode;

        /**
         * The output node that all audio nodes should point to.
         * Used internally to control volume.
         */
        public get outputNode(): AudioNode {
            return this._outputNode;
        }
        
        constructor(userMic: MediaStream | null) {
            this.userMic = userMic;
            this.audioCtx = new AudioContext();
            this._outputNode = this.audioCtx.createGain();
            this._outputNode.connect(this.audioCtx.destination);
        }
        
        /**
         * Get the master volume of WebSpeak.
         * @returns WebSpeak master volume, where `1` is full volume
         */
        public getVolume() {
            return this._outputNode.gain.value;
        }
        
        /**
         * Set the master volume of WebSpeak.
         * @param volume A new master volume, where `1` is full volume.
         */
        public setVolume(volume: number) {
            this._outputNode.gain.value = volume;
        }
        
        /**
         * Mute or unmute the local user's mic.
         * @param muted Whether to mute the mic.
         */
        public setMicMuted(muted: boolean) {
            if (this.userMic)
                setAudioMuted(this.userMic, muted);
        }
    }

    let audioManager: WSAudioManager | undefined;

    /**
     * Called when the audio manager has been setup for the first time.
     */
    export const onSetupAudioManager = new SimpleEvent<WSAudioManager>();

    export function getAudioManager() {
        return audioManager;
    }
    
    export function getAudioManagerOrThrow() {
        if (!audioManager) {
            throw new Error("Audio manager has not been setup!");
        }
        return audioManager;
    }
    
    export let userMic: MediaStream | undefined = undefined;

    export let audioCtx: AudioContext | undefined
    export let primaryGainNode: GainNode | undefined

    /**
     * A null-safe getter for `audioCtx`
     * @returns Audio context.
     */
    export function getAudioCtx() {
        if (!audioCtx) {
            throw new Error("Audio Context is undefined");
        }
        return audioCtx;
    }

    /**
     * Request microphone permissions from the user and set it up with the rest of the codebase.
     * @returns The `MediaStream` of the user's mic, or `null` if they denied access. 
     */
    // export async function requestMicAccess(): Promise<MediaStream | null> {
    //     // This is a convenient place to setup the audio context.
    //     if (audioCtx == undefined) {
    //         audioCtx = new AudioContext();
    //     }
    //     try {
    //         const mic = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    //         if (mic) {
    //             userMic = mic;
    //             onMicSet.dispatch(userMic);
    //             return userMic;
    //         }
    //     } catch (e) {
    //         console.error(e);
    //     }
    //     return null;
    // }

    /**
     * Request microphone permissions from the user and setup the webspeak audio manager.
     * @returns The new `AudioManager`, or the existing one if we're already setup.
     */
    export async function setupAudio() {
        if (audioManager) {
            console.warn("Tried to setup WebSpeak audio twice!");
            return audioManager;
        }
        let mic: MediaStream | null = null;
        try {
            mic = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
        } catch (e) {
            // User likely denied mic permissions. Continue with setup anyway.
            console.error(e);
        }
        audioManager = new WSAudioManager(mic);
        onSetupAudioManager.dispatch(audioManager);
        return audioManager;
    }
    
    /**
     * Mute or unmute a media stream.
     * @param audio Target media stream.
     * @param muted Whether to mute teh stream.
     */
    export function setAudioMuted(audio: MediaStream, muted: boolean) {
        audio.getTracks().forEach(track => {
            track.enabled = !muted;
        });
    }

    // /**
    //  * Called when the user accepts mic permissions.
    //  */
    // export const onMicSet = new SimpleEvent<MediaStream | undefined>();

    export const defaultPannerOptions: PannerOptions = {
        panningModel: "HRTF",
        distanceModel: "inverse",
        positionX: 0,
        positionY: 0,
        positionZ: 0,
        orientationX: 0,
        orientationY: 0,
        orientationZ: 0,
        refDistance: 1,
        maxDistance: 26,
        rolloffFactor: 1,
        coneInnerAngle: 360,
        coneOuterAngle: 0,
        coneOuterGain: 0,
    }

}

export default webSpeakAudio;