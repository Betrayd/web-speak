import AppInstance from "../AppInstance";

module rtcPackets {
    export interface RTCPacket { playerID: string, payload?: any }

    export function registerHandlers(app: AppInstance) {
        function registerHandler(name: string, handler: (app: AppInstance, playerID: string, payload?: any) => void) {
            app.netManager.packetHandlers.set(name, payload => {
                let data: Partial<RTCPacket> = JSON.parse(payload);
                if (data.playerID == undefined) {
                    throw new Error("Player ID was not sent.")
                }
                handler(app, data.playerID, data.payload);
            })
        }

        registerHandler('requestOffer', onRequestOffer);
        registerHandler('handOffer', onHandOffer);
        registerHandler('handAnswer', onHandAnswer);
        registerHandler('handIce', onHandIce);
        registerHandler('disconnectRTC', onDisconnectRTC);
    }

    async function onRequestOffer(app: AppInstance, playerID: string) {
        let offer = await app.requestRTCOffer(playerID);
        sendReturnOffer(app, playerID, offer);
    }
    
    async function onHandOffer(app: AppInstance, playerID: string, payload: any) {
        let answer = await app.handleRTCOffer(playerID, payload);
        sendReturnAnswer(app, playerID, answer);
    }

    function onHandAnswer(app: AppInstance, playerID: string, payload: any) {
        app.handleRTCAnswer(playerID, payload);
    }

    function onHandIce(app: AppInstance, playerID: string, payload: any) {
        let player = app.getPlayer(playerID);
        if (!player) {
            throw new Error("Unknown player ID: " + playerID);
        }

        if (player.isRemote()) {
            player.addIceCandidate(payload);
        }
    }

    function onDisconnectRTC(app: AppInstance, playerID: any) {
        app.disconnectPlayerRTC(playerID);
    }

    export function sendReturnOffer(app: AppInstance, playerID: string, offer: RTCSessionDescriptionInit) {
        console.debug(`Sending RTC offer to ${playerID}: `, offer);
        let packet: RTCPacket = {
            playerID,
            payload: offer
        };

        app.netManager.sendPacket('returnOffer', JSON.stringify(packet));
    }
    
    export function sendReturnAnswer(app: AppInstance, playerID: string, answer: RTCSessionDescriptionInit) {
        console.debug(`Sending RTC answer to ${playerID}: `, answer);
        let packet: RTCPacket = {
            playerID,
            payload: answer
        };

        app.netManager.sendPacket('returnAnswer', JSON.stringify(packet));
    }

    export function sendReturnIce(app: AppInstance, playerID: string, candidate: RTCIceCandidate) {
        let packet = {
            playerID,
            payload: candidate
        };
        app.netManager.sendPacket('returnIce', JSON.stringify(packet));
    }
}

export default rtcPackets;