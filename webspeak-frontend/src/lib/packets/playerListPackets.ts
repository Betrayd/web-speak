import AppInstance from "../AppInstance";
import PlayerListEntry from "../util/PlayerListEntry";

module playerListPackets {
    export function registerHandlers(app: AppInstance) {
        app.netManager.registerJsonHandler('setPlayerEntries', onUpdatePlayerList);
        app.netManager.registerJsonHandler('removePlayerEntries', onRemovePlayerEntries);
    }

    function onUpdatePlayerList(payload: Record<string, PlayerListEntry>, app: AppInstance) {
        app.playerList.setListEntries(Object.entries(payload));
        // Object.entries
        // for (const [key, val] of Object.entries(payload)) {
        //     app.playerList.set(key, val);
        // }
        // app.updatePlayerList();
    }

    function onRemovePlayerEntries(payload: string[], app: AppInstance) {
        app.playerList.removeListEntries(payload);
    }
}

export default playerListPackets;