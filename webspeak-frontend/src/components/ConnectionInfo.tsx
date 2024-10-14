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
            <dl className="row">
                <dt className="col-sm-4">Server Address:</dt>
                <dd className="col-sm-8">{props.app.serverAddress}</dd>

                <dt className="col-sm-4">Connection Status:</dt>
                <dd className="col-sm-8">{connectionStatus}</dd>

                <dt className="col-sm-4">Session ID:</dt>
                <dd className="col-sm-8">{props.app.sessionID}</dd>

                <dt className="col-sm-4">Local Player ID:</dt>
                <dd className="col-sm-8">{localPlayerID}</dd>
            </dl>
            {/* <Row className="justify-content-md-center">
                <Col className="col-sm-4">Server Address:</Col>
                <Col>{props.app.serverAddress}</Col>
            </Row>
            <Row>
                <Col className="col-sm-4">Connection Status:</Col>
                <Col>{connectionStatus}</Col>
            </Row>
            <Row>
                <Col className="col-sm-4">Session ID:</Col>
                <Col>{props.app.sessionID}</Col>
            </Row>
            <Row>
                <Col className="col-sm-4">Local player ID:</Col>
                <Col>{localPlayerID}</Col>
            </Row> */}
        </>
    )
}