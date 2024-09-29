import { createContext, useEffect } from "react";
import AppInstance from "../lib/AppInstance";
import { Heading } from "@chakra-ui/react";

// Somewhat bullshit, but we know this won't be used until it's been set
export const AppInstanceContext = createContext<AppInstance>(undefined as any);

export default function MainUI(props: { appInstance: AppInstance }) {
    const app = props.appInstance;

    useEffect(() => {
        if (app.connectionStatus == WebSocket.CLOSED) {
            app.connect();
        }
    })

    return (
        <AppInstanceContext.Provider value={props.appInstance}>
            <Heading>You are connected! I haven't made the app UI yet.</Heading>
        </AppInstanceContext.Provider>

    )
}
