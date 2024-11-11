import { createContext, useContext, useEffect } from "react";
import { Card, Col, Container, Row } from "react-bootstrap";
import AppInstance from "../lib/AppInstance";
import ConnectionInfo from "../components/ConnectionInfo";
import MicInfo from "../components/MicInfo";
import PlayerList from "./PlayerList";
import { ModalProvider } from "../App";
import { ModalType } from "../util/ModalContents";

// Somewhat bullshit, but we know this won't be used until it's been set
export const AppInstanceContext = createContext<AppInstance>(undefined as any);

export default function MainUI(props: { appInstance: AppInstance, onShutdown?: () => void }) {
  const app = props.appInstance;
  const [_modal, setModal] = useContext(ModalProvider);

  function onDisconnected(event: { message: string, errored: boolean }) {
    setModal({
      title: "Disconnected from server",
      type: event.errored ? ModalType.ERROR : ModalType.STANDARD,
      detail: event.message
    })
    if (props.onShutdown) {
      props.onShutdown();
    }
  }

  useEffect(() => {
    props.appInstance.netManager.onDisconnect.addListener(onDisconnected);
    return () => {
      props.appInstance.netManager.onDisconnect.removeListener(onDisconnected);
    }
  }, [props.appInstance])

  return (
    <AppInstanceContext.Provider value={props.appInstance}>
      <Container>
        <h2>Behold the worst UI ever created!</h2>
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
    </AppInstanceContext.Provider>
  )
}
