import { useEffect, useState } from "react";
import audioStores from "../stores/audioStores";
import { Col, Form, Row } from "react-bootstrap";

export default function MicInfo(_props: any) {
    // const mic = audioStores.useUserMic();
    const audioManager = audioStores.useAudioManager();
    const mic = audioManager?.userMic;
    const [muted, setMuted] = useState(() => false);

    useEffect(() => {
        if (audioManager) {
            audioManager.setMicMuted(muted);
            // webSpeakAudio.setAudioMuted(mic, muted);
        }
    }, [muted])

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