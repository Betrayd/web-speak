import PlayerListEntry from "./util/PlayerListEntry";
import SimpleEvent from "./util/SimpleEvent";

type PlayerListMap = ReadonlyMap<string, Readonly<PlayerListEntry>>;

export default class WSPlayerList {
    public readonly onUpdatePlayerList = new SimpleEvent<PlayerListMap>();

    private readonly _playerList = new Map<string, Readonly<PlayerListEntry>>();

    public get playerList(): PlayerListMap {
        return this._playerList;
    }

    public getListEntry(playerID: string) {
        return this._playerList.get(playerID);
    }

    public setListEntry(playerID: string, entry: PlayerListEntry) {
        this._playerList.set(playerID, Object.freeze(entry));
        this.updatePlayerList();
    }

    public setListEntries(entries: [string, PlayerListEntry][]) {
        for (const [id, entry] of entries) {
            this._playerList.set(id, Object.freeze(entry));
        }
        this.updatePlayerList();
    }

    public removeListEntry(playerID: string) {
        let success = this._playerList.delete(playerID);
        if (success) {
            this._volumes.delete(playerID);
            this.updatePlayerList();
        }
        return success;
    }

    public removeListEntries(playerIDs: Iterable<string>) {
        let success = false;
        for (let id of playerIDs) {
            if (this._playerList.delete(id)) {
                success = true;
                this._volumes.delete(id);
            }
                
        }
        if (success) {
            this.updatePlayerList();
        }
        return success;
    }

    private updatePlayerList() {
        this.onUpdatePlayerList.dispatch(this._playerList);
    }

    public readonly onUpdatePlayerVolume = new SimpleEvent<[string, number]>();

    private readonly _volumes = new Map<string, number>();

    public get volumes(): ReadonlyMap<string, number> {
        return this._volumes;
    }

    public setVolume(playerID: string, volume: number) {
        this._volumes.set(playerID, volume);
        this.onUpdatePlayerVolume.dispatch([playerID, volume]);
    }

    public getVolume(playerID: string) {
        let value = this._volumes.get(playerID);
        if (value === undefined) {
            value = 1;
        }
        return value;
    }
}