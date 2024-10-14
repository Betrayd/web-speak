import { useSyncExternalStore } from "react";
import AppInstance from "../lib/AppInstance";

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
}

export default appStatusStores;