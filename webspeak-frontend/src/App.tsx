import { Container, Navbar } from "react-bootstrap";
import './App.css';
import MainUI from "./pages/MainUI";

function App() {
    return (
        <>
            <Navbar className="navbar-expand-lg navbar-dark bg-dark mb-4">
                <Container>
                    <Navbar.Brand>WebSpeak</Navbar.Brand>
                    <Navbar.Text>hello world</Navbar.Text>
                </Container>
            </Navbar>

            <MainUI />
        </>
    )
}

export default App;