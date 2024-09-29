import React from "react";
import './App.css';
import AppInstance from "./lib/AppInstance";
import MainUI from "./pages/MainUI";
import ConnectionPrompt from "./pages/ConnectionPrompt";
import webSpeakAudio from "./lib/webSpeakAudio";
import { Box, Button, ChakraProvider, Container, Heading, SystemStyleObject, Text } from "@chakra-ui/react";

interface AppState {
    hasMic: boolean,
    appInstance: AppInstance | undefined
}

export default class App extends React.Component<any, AppState> {

    constructor(props: any) {
        super(props);
        this.state = { hasMic: false, appInstance: undefined };
    }

    private appInstance?: AppInstance

    componentDidMount(): void {
        // Try to get address and session ID from URL params.
        const urlParams = new URLSearchParams(window.location.search);
        let urlServerAddress = urlParams.get("server");
        let urlSessionID = urlParams.get("id");

        if (urlServerAddress != null && urlSessionID != null) {
            this.setState({ appInstance: this.launchApp(urlServerAddress, urlSessionID) });
        }
    }

    drawContent() {
        // TODO: make this cleaner
        if (!this.state.hasMic) {
            return (
                <Container paddingY="32px">
                    <Button onClick={e => {
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
                        this.setState({ appInstance: this.launchApp(info.serverAddress, info.sessionID) });
                    }} />
                </Container>

            )
        } else {
            return <MainUI appInstance={this.state.appInstance} />
        }

    }

    async requestMicAccess() {
        try {
            await webSpeakAudio.requestMicAccess();
            this.setState({ hasMic: true });
        } catch (e) {
            console.log(e);
        }
    }

    launchApp(serverAddress: string, sessionID: string): AppInstance {
        if (this.appInstance == undefined) {
            this.appInstance = new AppInstance(serverAddress, sessionID);
        }
        return this.appInstance;
    }

    render(): React.ReactNode {

        const boxStyles: SystemStyleObject = {
            padding: "10px",
            bg: "purple.400",
            color: "white",
            m: "10px",
            textAlign: "center",
            filter: "blur(2px)",
            ':hover': {
                color: 'black',
                bg: 'blue.200'
            }
        }

        return (
            <ChakraProvider>
                {this.drawContent()}
            </ChakraProvider>
            // <>
            //     <Navbar className="navbar-expand-lg navbar-dark bg-dark mb-4">
            //         <Container>
            //             <Navbar.Brand>WebSpeak</Navbar.Brand>
            //             <Navbar.Text>hello world</Navbar.Text>
            //         </Container>
            //     </Navbar>
            //     {this.drawContent()}
            // </>
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