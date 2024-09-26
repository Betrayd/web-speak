import { createContext } from "react";
import { Container } from "react-bootstrap";
import AppInstance from "../lib/AppInstance";

// Somewhat bullshit, but we know this won't be used until it's been set
export const AppInstanceContext = createContext<AppInstance>(undefined as any);

export default function MainUI(props: { appInstance: AppInstance }) {

    return (
        <AppInstanceContext.Provider value={props.appInstance}>
            <Container>
                <p className="text-danger">Not Connected</p>
            </Container>
        </AppInstanceContext.Provider>

    )
}
