/**
 * Global values used across the webspeak client
 */
module webSpeakClient {
    export let userMic: MediaStream | undefined = undefined;

    export async function requestMicAccess(): Promise<MediaStream> {
        let mic = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
        if (mic == undefined) {
            throw new Error("getUserMedia returned undefined.");
        }

        userMic = mic;
        return mic;
    }
}

export default webSpeakClient;