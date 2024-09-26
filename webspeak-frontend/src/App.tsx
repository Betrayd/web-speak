import React, { createContext, useState } from "react";
import { Container, Navbar } from "react-bootstrap";
import './App.css';
import AppInstance from "./lib/AppInstance";
import MainUI from "./pages/MainUI";

export default class App extends React.Component {

    render(): React.ReactNode {
        const [appInstance, setAppInstance] = useState<AppInstance | undefined>();

        
        return (
            <>
                <Navbar className="navbar-expand-lg navbar-dark bg-dark mb-4">
                    <Container>
                        <Navbar.Brand>WebSpeak</Navbar.Brand>
                        <Navbar.Text>hello world</Navbar.Text>
                    </Container>
                </Navbar>
                {appInstance != undefined ? <MainUI appInstance={appInstance} /> : null}
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