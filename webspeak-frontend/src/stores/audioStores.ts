import { useSyncExternalStore } from "react";
import webSpeakAudio from "../lib/webSpeakAudio";

module audioStores {
    function subscribeAudioManager(listener: () => void) {
        webSpeakAudio.onSetupAudioManager.addListener(listener);
        return () => webSpeakAudio.onSetupAudioManager.removeListener(listener);
    }

    /**
     * A react hook that gets the current audio manager.
     */
    export function useAudioManager() {
        return useSyncExternalStore(subscribeAudioManager, webSpeakAudio.getAudioManager);
    }

    // export function subscribeUserMic(listener: () => void) {
    //     webSpeakAudio.onMicSet.addListener(listener);
    //     return () => webSpeakAudio.onMicSet.removeListener(listener);
    // }

    // export function snapshotUserMic() {
    //     return webSpeakAudio.userMic;
    // }

    // export function useUserMic() {
    //     return useSyncExternalStore(subscribeUserMic, snapshotUserMic);
    // }
}

export default audioStores;