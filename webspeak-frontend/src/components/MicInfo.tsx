import { useEffect, useState } from "react";
import audioStores from "../stores/audioStores";
import webSpeakAudio from "../lib/webSpeakAudio";
import { Col, Form, Row } from "react-bootstrap";

export default function MicInfo(_props: any) {
    const mic = audioStores.useUserMic();
    const [muted, setMuted] = useState(() => false);

    useEffect(() => {
        if (mic) {
            webSpeakAudio.setAudioMuted(mic, muted);
        }
    })

    const micText = mic ? <span className="text-success">Mic connected.</span> 
        : <span className="text-danger">Mic not connected!</span>

    return (
        <>
            <Row>
                <Col>{micText}</Col>

            </Row>
            <Row>
                <Col>
                    <Form.Check
                        type="switch"
                        label="Mute"
                        id="muted"
                        checked={muted}
                        onChange={event => setMuted(event.target.checked)} />
                </Col>
            </Row>
        </>
    )
}