import { createContext, useEffect } from "react";
import { Container } from "react-bootstrap";
import AppInstance from "../lib/AppInstance";

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
            <Container>
                <p className="text-danger">Not Connected</p>
            </Container>
        </AppInstanceContext.Provider>

    )
}
