import { createContext, useEffect } from "react";
import { Card, Col, Container, Row } from "react-bootstrap";
import AppInstance from "../lib/AppInstance";
import ConnectionInfo from "../components/ConnectionInfo";
import MicInfo from "../components/MicInfo";

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
                <Row>
                    <Col>
                        <Card>
                            <Card.Header>Server Connection Status</Card.Header>
                            <Card.Body>
                                <ConnectionInfo app={app} />
                            </Card.Body>
                        </Card>
                    </Col>
                    <Col>
                        <Card>
                            <Card.Header>Local Microphone</Card.Header>
                            <Card.Body>
                                <MicInfo />
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            </Container>
        </AppInstanceContext.Provider>

    )
}
