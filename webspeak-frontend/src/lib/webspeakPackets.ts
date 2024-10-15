import AppInstance, { PlayerTransform } from "./AppInstance";
import { AudioModifier } from "./WebSpeakPlayer";
import playerListPackets from "./packets/playerListPackets";
import rtcPackets from "./packets/rtcPackets";

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
        registerHandler('setAudioModifier', onSetAudioModifier)

        rtcPackets.registerHandlers(app);
        playerListPackets.registerHandlers(app);
        
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
        interface TransformData extends PlayerTransform {
            playerID: string
        }

        const data: Partial<TransformData> = JSON.parse(payload);

        if (data.playerID == undefined) {
            throw new Error("Player ID was not sent.");
        }

        app.updatePlayerTransform(data.playerID, data);

    }

    function onSetPannerOptions(app: AppInstance, payload: string) {
        let options: PannerOptions = JSON.parse(payload);
        app.setPannerOptions(options);
    }

    function onSetAudioModifier(app: AppInstance, payload: string) {
        interface PacketPayload {
            playerID: string,
            audioModifier: Partial<AudioModifier>
        }

        const data: Partial<PacketPayload> = JSON.parse(payload);
        if (data.playerID == undefined) {
            throw new Error("Player ID was not sent.");
        }
        if (data.audioModifier == undefined) {
            throw new Error("Audio modifier was not sent.");
        }

        const player = app.getPlayer(data.playerID, true);
        if (!player) {
            throw new Error("Invalid player ID: " + data.playerID);
        }
        
        player.applyAudioModifier(data.audioModifier);
    }
}

export default webspeakPackets;