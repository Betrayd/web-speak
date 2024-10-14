import { useSyncExternalStore } from "react";
import webSpeakAudio from "../lib/webSpeakAudio";

module audioStores {
    export function subscribeUserMic(listener: () => void) {
        webSpeakAudio.onMicSet.addListener(listener);
        return () => webSpeakAudio.onMicSet.removeListener(listener);
    }

    export function snapshotUserMic() {
        return webSpeakAudio.userMic;
    }

    export function useUserMic() {
        return useSyncExternalStore(subscribeUserMic, snapshotUserMic);
    }
}

export default audioStores;