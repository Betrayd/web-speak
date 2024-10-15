import { createContext, useEffect, useState } from "react";
import { Card, Col, Container, Row } from "react-bootstrap";
import AppInstance from "../lib/AppInstance";
import ConnectionInfo from "../components/ConnectionInfo";
import MicInfo from "../components/MicInfo";
import { DisconnectedModal } from "../components/DisconnectedModal";
import PlayerList from "./PlayerList";

// Somewhat bullshit, but we know this won't be used until it's been set
export const AppInstanceContext = createContext<AppInstance>(undefined as any);

interface DisconnectedState {
  message: string,
  errored: boolean
}

export default function MainUI(props: { appInstance: AppInstance, onShutdown?: () => void }) {
  const app = props.appInstance;

  // useEffect(() => {
  //     if (app.connectionStatus == WebSocket.CLOSED) {
  //         app.connect();
  //     }
  // }, [])

  const [disconnected, setDisconnected] = useState<DisconnectedState | undefined>(() => undefined);

  function onDisconnected(event: { message: string, errored: boolean }) {
    setDisconnected(event);
  }

  useEffect(() => {
    props.appInstance.netManager.onDisconnect.addListener(onDisconnected);
    return () => {
      props.appInstance.netManager.onDisconnect.removeListener(onDisconnected);
    }
  }, [props.appInstance])

  function onCloseDisconnectModal() {
    setDisconnected(undefined);
    if (props.onShutdown) {
      props.onShutdown();
    }
  }
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
        <Row>
          <Col>
            <Card>
              <Card.Header>Players</Card.Header>
              <Card.Body>
                <PlayerList />
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
      {disconnected ? <DisconnectedModal show
        message={disconnected.message}
        errored={disconnected.errored}
        onClose={onCloseDisconnectModal} /> : undefined}
    </AppInstanceContext.Provider>
  )
}
