/**
 * Contains global methods related to audio processing.
 * This is not connected to the app instance so it can be easily accessed globally.
 */
module webSpeakAudio {
    export let userMic: MediaStream | undefined = undefined;

    export let audioCtx: AudioContext | undefined

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

    export async function requestMicAccess(): Promise<MediaStream> {
        if (audioCtx == undefined) {
            audioCtx = new AudioContext();
        }
        let mic = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
        if (mic == undefined) {
            throw new Error("getUserMedia returned undefined.");
        }
        
        userMic = mic;
        return mic;
    }

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