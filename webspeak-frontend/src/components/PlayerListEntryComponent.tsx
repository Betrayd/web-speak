import { Col, Form, Row } from "react-bootstrap"
import VolumeSlider from "./VolumeSlider"

interface PlayerListEntryProps {
    playerName: string,
    avatar?: string,
    volume?: number,
    onSetVolume?: (volume: number) => void
}

export default function PlayerListEntryComponent(props: PlayerListEntryProps) {
    return <>
        <Row>
            <Col><b>{props.playerName}</b></Col>
        </Row>
        {props.volume !== undefined ?
            <Row>
                <Form.Label>Player Volume</Form.Label>
                <VolumeSlider value={props.volume} min={0} max={2} onChange={props.onSetVolume} />
            </Row>
            : null}
    </>
}