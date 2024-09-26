import React, { createContext, useState } from "react";
import { Button, Container, Navbar } from "react-bootstrap";
import './App.css';
import AppInstance from "./lib/AppInstance";
import MainUI from "./pages/MainUI";
import webSpeakClient from "./lib/WebSpeakClient";
import ConnectionPrompt from "./pages/ConnectionPrompt";

interface AppState {
    hasMic: boolean,
    appInstance: AppInstance | undefined
}

export default class App extends React.Component<any, AppState> {

    constructor(props: any) {
        super(props);
        this.state = { hasMic: false, appInstance: undefined };
    }

    componentDidMount(): void {
        // Try to get address and session ID from URL params.
        const urlParams = new URLSearchParams(window.location.search);
        let urlServerAddress = urlParams.get("server");
        let urlSessionID = urlParams.get("id");
        
        if (urlServerAddress != null && urlSessionID != null) {
            this.setState({appInstance: new AppInstance(urlServerAddress, urlSessionID)});
        }

    }

    drawContent() {
        // TODO: make this cleaner
        if (!this.state.hasMic) {
            return (
                <Container>
                    <Button type="button" onClick={e => {
                        e.preventDefault();
                        this.requestMicAccess();
                    }}>Request mic access</Button>
                </Container>
            )

        }

        if (this.state.appInstance == undefined) {
            console.log("drawing connection prompt")
            return (
                <Container>
                    <ConnectionPrompt onConnect={info => {
                        this.setState({ appInstance: new AppInstance(info.serverAddress, info.sessionID) });
                    }} />
                </Container>

            )
        } else {
            return <MainUI appInstance={this.state.appInstance} />
        }
        
    }

    async requestMicAccess() {
        try {
            await webSpeakClient.requestMicAccess();
            this.setState({ hasMic: true });
        } catch (e) {
            console.log(e);
        }
    }

    render(): React.ReactNode {
        
        return (
            <>
                <Navbar className="navbar-expand-lg navbar-dark bg-dark mb-4">
                    <Container>
                        <Navbar.Brand>WebSpeak</Navbar.Brand>
                        <Navbar.Text>hello world</Navbar.Text>
                    </Container>
                </Navbar>
                {this.drawContent()}
            </>
        )
    }
}

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