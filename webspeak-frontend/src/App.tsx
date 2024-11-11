import { createContext, useContext, useState } from "react";
import './App.css';
import AppInstance from "./lib/AppInstance";
import ConnectionPrompt from "./pages/ConnectionPrompt";
import MainUI from "./pages/MainUI";
import webSpeakAudio from "./lib/webSpeakAudio";
import { Button, Container } from "react-bootstrap";
import ModalContents, { ModalType } from "./util/ModalContents";
import SimpleModal from "./components/SimpleModal";

type ModalContentsHandle = [Readonly<ModalContents> | null, (val: ModalContents | null) => any];

/**
 * Allows various parts of the code to add modals to the screen.
 */
export const ModalProvider = createContext<ModalContentsHandle>([null, () => {}]);

export default function App() {
    const [modalContents, setModalContents] = useState<ModalContents | null>(null);

    // No reason to draw whole components here when therse are just
    // helper functions that return one component each.
    return (
        <ModalProvider.Provider value={[modalContents, setModalContents]}>
            <AppUI />
            {modalContents ? SimpleModalWithContents(
                {
                    contents: modalContents,
                    onClose: () => setModalContents(null)
                }) : null}
        </ModalProvider.Provider>
    )
}

function SimpleModalWithContents(props: { contents: ModalContents, onClose: () => any }) {
    return <SimpleModal
        title={props.contents.title}
        type={props.contents.type}
        onClose={props.onClose}
        show
    >
        <p>{props.contents.detail}</p>
    </SimpleModal>
}

/**
 * Main app UI could be a number of different things.
 * This simply returns the correct page for the given context.
 * @returns React element.
 */
function AppUI() {
    // Note: mic permissions may not actually be granted.
    // This just signifies that this step has been completed.
    const [hasMic, setHasMic] = useState(() => false);

    const [app, setApp] = useState<AppInstance | null>(() => null);
    const [_modal, setModal] = useContext(ModalProvider);

    function closeApp() {
        if (app) {
            app.shutdown();
            setApp(null);
        }
    }

    function connect(serverAddress: string, sessionID: string) {
        // Ensure valid
        if (!serverAddress || !sessionID) {
            let detail = serverAddress ? "Please enter a session ID." : "Please enter a server address.";
            setModal({title: "Unable to connect", type: ModalType.ERROR, detail});
            return;
        }

        // Ensure URL is valid
        try {
            new URL(serverAddress);
        } catch (e) {
            setModal({ title: "Unable to connect", type: ModalType.ERROR, detail: "The supplied URL is invalid." });
            return;
        }

        const app = new AppInstance(serverAddress, sessionID);
        setApp(app);
        app.connect();
    }

    async function requestMicAccess() {
        await webSpeakAudio.setupAudio();
        setHasMic(true);

        const urlParams = new URLSearchParams(window.location.search);
        let urlServerAddress = urlParams.get("server");
        let urlSessionID = urlParams.get("id");

        if (urlServerAddress && urlSessionID) {
            connect(urlServerAddress, urlSessionID);
        }
    }

    if (hasMic) {
        if (app) {
            return <MainUI appInstance={app} onShutdown={closeApp} />
        } else {
            return <ConnectionPrompt onConnect={connect} />
        }
    } else {
        // TODO: Make better UI for this
        return (
            <Container>
                <Button onClick={(e) => {
                    e.preventDefault();
                    requestMicAccess();
                }}>Join Voice</Button>
            </Container>
        )
    }
}