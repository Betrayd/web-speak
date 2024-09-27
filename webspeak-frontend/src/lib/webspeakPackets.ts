import AppInstance from "./AppInstance";

module webspeakPackets {
    export function setupPacketListeners(app: AppInstance) {
        
        function registerHandler(name: string, handler: (app: AppInstance, payload: string) => void) {
            app.netManager.packetHandlers.set(name, payload => handler(app, payload));
        }

        registerHandler('localPlayerInfo', onLocalPlayerInfo);
        registerHandler('updateTransform', onUpdateTransform);
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
}

export default webspeakPackets;