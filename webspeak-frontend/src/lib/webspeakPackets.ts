import AppInstance from "./AppInstance";
import rtcPackets from "./packets/rtcPackets";
import setAudioParamsS2CPacket from "./packets/setAudioParamsS2CPacket";

module webspeakPackets {
    export function setupPacketListeners(app: AppInstance) {
        
        function registerHandler(name: string, handler: (app: AppInstance, payload: string) => void) {
            app.netManager.packetHandlers.set(name, payload => handler(app, payload));
        }

        // function registerRTCPacketHandler(name: string, handler: (app: AppInstance, playerID: string, payload: any) => void) {
        //     app.netManager.packetHandlers.set(name, payload => {
        //         let data: { playerID?: string, payload: any } = JSON.parse(payload);
        //         if (data.playerID == undefined) {
        //             throw new Error("Player ID was not sent.");
        //         }
        //         handler(app, data.playerID, data.payload);
        //     })
        // }

        registerHandler('localPlayerInfo', onLocalPlayerInfo);
        registerHandler('updateTransform', onUpdateTransform);
        registerHandler('setPannerOptions', onSetPannerOptions);
        registerHandler('setAudioParams', setAudioParamsS2CPacket.handle);

        rtcPackets.registerHandlers(app);
        
        // registerRTCPacketHandler('handIce', onHandIce);
        // registerHandler('requestOffer', onRequestOffer);
        // registerRTCPacketHandler('handOffer', onHandOffer);
        // registerRTCPacketHandler('handAnswer', onHandAnswer);
        // registerHandler('disconnectRTC', onDisconnectRTC);
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
            forward: number[]
            up: number[]
        }

        const data: Partial<PositionData> = JSON.parse(payload);

        if (data.playerID == undefined) {
            throw new Error("Player ID was not sent.");
        }
        // console.debug("Recieved update transform from " + data.playerID, data.pos)
        let player = app.getPlayer(data.playerID);
        if (player) {
            if (data.pos) {
                player.setPos(data.pos);
            }
            
            if (data.forward) {
                player.setForward(data.forward);
            }

            if (data.up) {
                player.setUp(data.up)
            }
            // if (data.rot != null) {
            //     player.setRot(data.rot);
            // }
            player.updateTransform();
        } else {
            console.warn("Recieved transform for unknown player: ", data.playerID)
        }
    }

    function onSetPannerOptions(app: AppInstance, payload: string) {
        let options: PannerOptions = JSON.parse(payload);
        app.setPannerOptions(options);
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