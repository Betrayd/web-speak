import { useState } from "react";
import './App.css';
import AppInstance from "./lib/AppInstance";
import ConnectionPrompt from "./pages/ConnectionPrompt";
import MainUI from "./pages/MainUI";
import webSpeakAudio from "./lib/webSpeakAudio";
import { Button, Container } from "react-bootstrap";

export default function App(_props: any) {
    // Note: mic permissions may not actually be granted. 
    // This just signifies that this step has been completed.
    const [hasMic, setHasMic] = useState(() => false);

    const [app, setApp] = useState<AppInstance | null>(() => null);

    function closeApp() {
        if (app) {
            app.shutdown();
            setApp(null);
        }
    }

    function connect(serverAddress: string, sessionID: string) {
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

// interface AppState {
//     hasMic: boolean,
//     appInstance: AppInstance | undefined
// }

// export default class App extends React.Component<any, AppState> {

//     constructor(props: any) {
//         super(props);
//         this.state = { hasMic: false, appInstance: undefined };
//     }

//     private appInstance?: AppInstance

//     componentDidMount(): void {
//         // Try to get address and session ID from URL params.
//         const urlParams = new URLSearchParams(window.location.search);
//         let urlServerAddress = urlParams.get("server");
//         let urlSessionID = urlParams.get("id");

//         if (urlServerAddress != null && urlSessionID != null) {
//             this.setState({ appInstance: this.launchApp(urlServerAddress, urlSessionID) });
//         }
//     }

//     closeApp() {
//         if (this.appInstance)
//             this.appInstance.shutdown();
//         this.appInstance = undefined;
//         this.setState({ appInstance: undefined });
//     }

//     drawContent() {
//         // TODO: make this cleaner
//         if (!this.state.hasMic) {
//             return (
//                 <Container>
//                     <Button type="button" onClick={e => {
//                         e.preventDefault();
//                         this.requestMicAccess();
//                     }}>Request mic access</Button>
//                 </Container>
//             )

//         }

//         if (this.state.appInstance == undefined) {
//             console.log("drawing connection prompt")
//             return (
//                 <Container>
//                     <ConnectionPrompt onConnect={info => {
//                         this.setState({ appInstance: this.launchApp(info.serverAddress, info.sessionID) });
//                     }} />
//                 </Container>

//             )
//         } else {
//             return <MainUI appInstance={this.state.appInstance} closeApp={this.closeApp.bind(this)} />
//         }

//     }

//     async requestMicAccess() {
//         try {
//             await webSpeakAudio.requestMicAccess();
//             this.setState({ hasMic: true });
//         } catch (e) {
//             console.log(e);
//         }
//     }

//     launchApp(serverAddress: string, sessionID: string): AppInstance {
//         if (this.appInstance == undefined) {
//             this.appInstance = new AppInstance(serverAddress, sessionID);
//         }
//         return this.appInstance;
//     }

//     render(): React.ReactNode {

//         return (
//             <>
//                 <Navbar className="navbar-expand-lg navbar-dark bg-dark mb-4">
//                     <Container>
//                         <Navbar.Brand>WebSpeak</Navbar.Brand>
//                         <Navbar.Text>hello world</Navbar.Text>
//                     </Container>
//                 </Navbar>
//                 {this.drawContent()}
//             </>
//         )
//     }
// }

// function App() {
//     const [appInstance, setAppInstance] = useState<AppInstance | undefined>();



//     return (
//         <>
//             <Navbar className="navbar-expand-lg navbar-dark bg-dark mb-4">
//                 <Container>
//                     <Navbar.Brand>WebSpeak</Navbar.Brand>
//                     <Navbar.Text>hello world</Navbar.Text>
//                 </Container>
//             </Navbar>
//             {appInstance != undefined ? <MainUI appInstance={appInstance} /> : null}
//         </>
//     )
// }

// export default App;