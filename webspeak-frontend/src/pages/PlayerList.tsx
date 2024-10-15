import { useContext, useState } from "react";
import { AppInstanceContext } from "./MainUI";
import { Form, Row } from "react-bootstrap";
import VolumeSlider from "../components/VolumeSlider";
import appStatusStores from "../stores/appStatusStores";
import PlayerListEntry from "../lib/util/PlayerListEntry";
import PlayerListEntryComponent from "../components/PlayerListEntryComponent";

export default function PlayerList(_props: any) {
    const app = useContext(AppInstanceContext);
    const playerList = appStatusStores.usePlayerList(app);

    const [masterVolume, setMasterVolume] = useState(() => 1);
    function drawPlayers() {
        const data = Array.from((playerList.entries()))
        return data.map(entry => <>
            <RemotePlayer key={entry[0]} playerID={entry[0]} meta={entry[1]} />
            <hr/>
        </>)
    }

    return (
        <>
            <Row>
                <Form.Label>Master Volume</Form.Label>
                <VolumeSlider value={masterVolume} min={0} max={1.5} onChange={setMasterVolume} />
                {drawPlayers()}
            </Row>
            
        </>
    )
}

export function RemotePlayer(props: { playerID: string, meta: PlayerListEntry }) {
    const app = useContext(AppInstanceContext);
    const volume = appStatusStores.usePlayerVolume(app.playerList, props.playerID);

    function onSetVolume(value: number) {
        app.playerList.setVolume(props.playerID, value);
    }

    return <PlayerListEntryComponent playerName={props.meta.name} avatar={props.meta.avatar} volume={volume} onSetVolume={onSetVolume} />
}
