import { useContext, useEffect, useState } from "react";
import { AppInstanceContext } from "./MainUI";
import { Form, Row } from "react-bootstrap";
import VolumeSlider from "../components/VolumeSlider";
import appStatusStores from "../stores/appStatusStores";
import PlayerListEntry from "../lib/util/PlayerListEntry";
import PlayerListEntryComponent from "../components/PlayerListEntryComponent";
import webSpeakAudio from "../lib/webSpeakAudio";

export default function PlayerList(_props: any) {
    const app = useContext(AppInstanceContext);
    const playerList = appStatusStores.usePlayerList(app);

    const [masterVolume, setMasterVolume] = useState(() => 1);

    useEffect(() => {
        webSpeakAudio.getAudioManagerOrThrow().setVolume(masterVolume);
    }, [masterVolume])

    const playerComponents = Object.entries(playerList).map(([key, value]) => (
        <>
            <Row key={key}>
                <RemotePlayer key={key} playerID={key} meta={value} />
                <hr key={key + ".hr"} />
            </Row>
        </>
    ));

    return (
        <>
            <Row key="header">
                <Form.Label key="header.label">Master Volume</Form.Label>
                <VolumeSlider key="header.volume" value={masterVolume} min={0} max={2} onChange={setMasterVolume} />
                <hr />
            </Row>
            {playerComponents}
        </>
    )
}

export function RemotePlayer(props: { playerID: string, meta: PlayerListEntry }) {
    const app = useContext(AppInstanceContext);
    const [volume, setVolume] = useState(() => 1);

    // Init volume
    useEffect(() => {
        let newVolume = app.getPlayerVolume(props.playerID);
        if (newVolume !== 1) {
            setVolume(newVolume);
        }
    }, [])
    
    useEffect(() => {
        app.setPlayerVolume(props.playerID, volume);
    }, [volume])

    return <PlayerListEntryComponent playerName={props.meta.name} avatar={props.meta.avatar} volume={volume} onSetVolume={setVolume} />
}
