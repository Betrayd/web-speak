import AudioModifier from "../util/AudioModifier";
import AppInstance, { PlayerTransform } from "./AppInstance";
import rtcPackets from "./packets/rtcPackets";

module webspeakPackets {
    export function setupPacketListeners(app: AppInstance) {
        
        function registerHandler(name: string, handler: (app: AppInstance, payload: string) => void) {
            app.netManager.packetHandlers.set(name, payload => handler(app, payload));
        }

        registerHandler('localPlayerInfo', onLocalPlayerInfo);
        registerHandler('updateTransform', onUpdateTransform);
        registerHandler('setPannerOptions', onSetPannerOptions);
        registerHandler('setAudioModifier', onSetAudioModifier);

        rtcPackets.registerHandlers(app);

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
        interface Packet {
            playerID: string,
            modifier: AudioModifier
        }

        const data = JSON.parse(payload) as Partial<Packet>;

        if (!data.modifier) {
            throw new Error("Modifier was not sent.");
        }
        
        const player = app.getPlayer(data.playerID, true);
        if (!player) {
            throw new Error("Unknown player: " + data.playerID);
        }

        player.audioModifier = data.modifier;
    }
}

export default webspeakPackets;