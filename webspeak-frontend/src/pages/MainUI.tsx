import { createContext, useState } from "react";
import { Container } from "react-bootstrap";
import AppInstance from "../lib/AppInstance";
import ConnectionPrompt from "./ConnectionPrompt";

// Somewhat bullshit, but we know this won't be used until it's been set
export const AppInstanceContext = createContext<AppInstance>(undefined as any);

export default function MainUI(props: { appInstance: AppInstance }) {

    return (
        <AppInstanceContext.Provider value={props.appInstance}>
            <Container>
                <p className="text-danger">Not Connected</p>
                <ConnectionPrompt onConnect={() => { }} />
            </Container>
        </AppInstanceContext.Provider>

    )
}
