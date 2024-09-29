/**
 * Global values used across the webspeak client
 */
module webSpeakClient {

    export const rtcConfig: RTCConfiguration = {
        iceServers: [
            {
                urls: ['stun:stun1.l.google.com:19302', 'stun:stun2.l.google.com:19302'],
            }
        ],
        iceCandidatePoolSize: 10
    }
}

export default webSpeakClient;