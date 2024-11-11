import { MouseEventHandler, ReactNode } from "react";
import { Button, Modal } from "react-bootstrap";
import { ModalType } from "../util/ModalContents";

export default function SimpleModal(props: {
    title: string, show?: boolean, type: ModalType,
    onClose?: MouseEventHandler<HTMLButtonElement>, children?: ReactNode
}) {

    let titleClass: string | undefined;

    switch (props.type) {
        case ModalType.WARNING:
            titleClass = "text-warning";
            break;
        case ModalType.ERROR:
            titleClass = "text-danger";
            break;
    }

    return (
        <Modal
            size="lg"
            show={props.show}
            aria-labelledby="contained-modal-title-vcenter"
            centered
        >
            <Modal.Header>
                <Modal.Title id="contained-modal-title-vcenter">
                    <span className={titleClass}>{props.title}</span>
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {props.children}
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={props.onClose}>Close</Button>
            </Modal.Footer>
        </Modal>
    )
}