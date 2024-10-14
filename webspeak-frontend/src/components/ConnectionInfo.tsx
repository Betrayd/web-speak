import { Col, Row } from "react-bootstrap";
import AppInstance from "../lib/AppInstance";
import appStatusStores from "../stores/appStatusStores";

export default function ConnectionInfo(props: { app: AppInstance }) {
    let readyState = appStatusStores.useConnectionStatus(props.app);
    let localPlayerID = appStatusStores.useLocalPlayerID(props.app);
    
    const connectionStatus = {
        [WebSocket.CONNECTING]: <span className="text-info">Connecting</span>,
        [WebSocket.OPEN]: <span className="text-success">Open</span>,
        [WebSocket.CLOSING]: <span className="text-warning">Closing</span>,
        [WebSocket.CLOSED]: <span className="text-danger">Closed</span>
    }[readyState];
    return (
        <>
            <Row className="justify-content-md-center">
                <Col xs lg="2">Server Address:</Col>
                <Col>{props.app.serverAddress}</Col>
            </Row>
            <Row>
                <Col xs lg="2">Connection Status:</Col>
                <Col>{connectionStatus}</Col>
            </Row>
            <Row>
                <Col xs lg="2">Session ID:</Col>
                <Col>{props.app.sessionID}</Col>
            </Row>
            <Row>
                <Col xs lg="2">Local player ID:</Col>
                <Col>{localPlayerID}</Col>
            </Row>
        </>
    )
}