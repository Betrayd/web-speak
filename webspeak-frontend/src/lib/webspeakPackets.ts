import AppInstance from "./AppInstance";

module webspeakPackets {
    export function setupPacketListeners(app: AppInstance) {
        
        function registerHandler(name: string, handler: (app: AppInstance, payload: string) => void) {
            app.netManager.packetHandlers.set(name, payload => handler(app, payload));
        }

        function registerRTCPacketHandler(name: string, handler: (app: AppInstance, playerID: string, payload: any) => void) {
            app.netManager.packetHandlers.set(name, payload => {
                let data: { playerID?: string, payload: any } = JSON.parse(payload);
                if (data.playerID == undefined) {
                    throw new Error("Player ID was not sent.");
                }
                handler(app, data.playerID, data.payload);
            })
        }

        registerHandler('localPlayerInfo', onLocalPlayerInfo);
        registerHandler('updateTransform', onUpdateTransform);
        
        registerRTCPacketHandler('handIce', onHandIce);
        registerHandler('requestOffer', onRequestOffer);
        registerRTCPacketHandler('handOffer', onHandOffer);
        registerRTCPacketHandler('handAnswer', onHandAnswer);
        registerHandler('disconnectRTC', onDisconnectRTC);
    }

    function onLocalPlayerInfo(app: AppInstance, payload: string) {
        const info: { playerID?: string } = JSON.parse(payload);
        if (info.playerID) {
            app.setLocalPlayerID(info.playerID);
        }
    }

    function onUpdateTransform(app: AppInstance, payload: string) {
        interface PositionData {
            playerID: string,
            pos: number[],
            rot: number[]
        }

        const data: Partial<PositionData> = JSON.parse(payload);

        if (data.playerID == undefined) {
            throw new Error("Player ID was not sent.");
        }

        let player = app.getPlayer(data.playerID);
        if (player) {
            if (data.pos != null) {
                player.setPos(data.pos);
            }
            if (data.rot != null) {
                player.setRot(data.rot);
            }
            player.updateTransform();
        }
    }

    async function onRequestOffer(app: AppInstance, payload: string) {
        const data: { playerID?: string } = JSON.parse(payload);
        if (data.playerID == undefined) {
            throw new Error("Player ID was not sent.");
        }

        let offer = await app.requestRTCOffer(data.playerID);
        sendReturnOffer(app, data.playerID, offer);
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

    function onDisconnectRTC(app: AppInstance, payload: string) {
        let data: { playerID?: string } = JSON.parse(payload);
        if (data.playerID == undefined) {
            throw new Error("Player ID was not sent.");
        }

        app.disconnectPlayerRTC(data.playerID);
    }

    export function sendReturnIce(app: AppInstance, playerID: string, candidate: RTCIceCandidate) {
        let packet = {
            playerID,
            payload: candidate
        };
        app.netManager.sendPacket('returnIce', JSON.stringify(packet));
    }

    export function sendReturnOffer(app: AppInstance, playerID: string, offer: RTCSessionDescriptionInit) {
        let packet = {
            playerID,
            payload: offer
        }
        app.netManager.sendPacket('returnOffer', JSON.stringify(packet));
    }

    export function sendReturnAnswer(app: AppInstance, playerID: string, answer: RTCSessionDescriptionInit) {
        let packet = {
            playerID,
            payload: answer
        }
        app.netManager.sendPacket('returnAnswer', JSON.stringify(packet));
    }
}

export default webspeakPackets;