import PlayerListEntry from "./util/PlayerListEntry";
import SimpleEvent from "./util/SimpleEvent";

export type PlayerListMap = ReadonlyMap<string, Readonly<PlayerListEntry>>;

export default class WSPlayerList {
    public readonly onUpdatePlayerList = new SimpleEvent<PlayerListMap>();

    private readonly _playerList = new Map<string, Readonly<PlayerListEntry>>();

    private _playerListObject: Record<string, Readonly<PlayerListEntry>> = {}

    /**
     * An immutable object containing the entire player list at this time.
     */
    public get playerListObject() {
        return this._playerListObject;
    }

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
            this.updatePlayerList();
        }
        return success;
    }

    public removeListEntries(playerIDs: Iterable<string>) {
        let success = false;
        for (let id of playerIDs) {
            if (this._playerList.delete(id)) {
                success = true;
            }
                
        }
        if (success) {
            this.updatePlayerList();
        }
        return success;
    }

    private updatePlayerList() {
        this._playerListObject = Object.fromEntries(this._playerList);
        Object.freeze(this._playerListObject);
        this.onUpdatePlayerList.dispatch(this._playerList);
    }
}