import { Button, Modal } from "react-bootstrap";

export function DisconnectedModal(props: { message: string, show?: boolean, errored?: boolean, onClose: () => void }) {
    const title = props.errored ? <span className="text-danger">Connection Lost!</span> : <span>Disconnected from server</span>

    return (
        <Modal
            {...props}
            size="lg"
            show={props.show}
            aria-labelledby="contained-modal-title-vcenter"
            centered
        >
            <Modal.Header>
                <Modal.Title id="contained-modal-title-vcenter">
                    {title}
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p>
                    {props.message}
                </p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={props.onClose}>Close</Button>
            </Modal.Footer>
        </Modal>
    )
}