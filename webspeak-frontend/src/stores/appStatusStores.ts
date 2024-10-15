import { useSyncExternalStore } from "react";
import AppInstance from "../lib/AppInstance";
import WSPlayerList from "../lib/WSPlayerList";

module appStatusStores {

    namespace connectionStatusStore {
        export function subscribe(app: AppInstance, listener: () => void) {
            app.netManager.onConnectionStatusChanged.addListener(listener);
            return () => app.netManager.onConnectionStatusChanged.removeListener(listener);
        }
    
        export function getSnapshot(app: AppInstance) {
            return app.netManager.connectionStatus;
        }
    }

    namespace localPlayerIDStore {
        export function subscribe(app: AppInstance, listener: () => void) {
            app.onSetLocalPlayerID.addListener(listener);
            return () => app.onSetLocalPlayerID.removeListener(listener);
        }
        export function getSnapshot(app: AppInstance) {
            return app.localPlayerID;
        }
    }

    export function useConnectionStatus(app: AppInstance) {
        return useSyncExternalStore(listener => connectionStatusStore.subscribe(app, listener), () => connectionStatusStore.getSnapshot(app));
    }

    export function useLocalPlayerID(app: AppInstance) {
        return useSyncExternalStore(listener => localPlayerIDStore.subscribe(app, listener), () => localPlayerIDStore.getSnapshot(app));
    }

    function subscribePlayerList(app: AppInstance, listener: () => void) {
        app.playerList.onUpdatePlayerList.addListener(listener);
        return () => app.playerList.onUpdatePlayerList.removeListener(listener);
    }

    export function usePlayerList(app: AppInstance) {
        return useSyncExternalStore(listener => subscribePlayerList(app, listener), () => app.playerList.playerList)
    }

    function subscribePlayerVolume(playerList: WSPlayerList, playerID: string, listener: () => void) {
        let actualListener = ([id, _val]: [string, number]) => {
            if (id == playerID) {
                listener();
            }
        };
        playerList.onUpdatePlayerVolume.addListener(actualListener);
        return () => playerList.onUpdatePlayerVolume.removeListener(actualListener);
    }

    export function usePlayerVolume(playerList: WSPlayerList, playerID: string) {
        return useSyncExternalStore(listener => subscribePlayerVolume(playerList, playerID, listener),
            () => playerList.getVolume(playerID));
    }
}

export default appStatusStores;