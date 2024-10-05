import AppInstance from "../AppInstance";

module setAudioParamsS2CPacket {
    export interface SetAudioParamsS2CPayload {
        playerID: string,
        spatialize: boolean,
        mute: boolean,
        overridePanner: boolean,
        pannerOptions?: Partial<PannerOptions>
    }

    export function handle(app: AppInstance, payload: string) {
        let parsed = JSON.parse(payload) as SetAudioParamsS2CPayload;

        let player = app.getPlayer(parsed.playerID);
        if (!player) {
            throw new Error("Unknown player ID: " + parsed.playerID);
        }

        if (player.isRemote()) {
            player.muted = parsed.mute;
            player.spatialized = parsed.spatialize;
            
            if (parsed.overridePanner && parsed.pannerOptions) {
                player.setPannerOptionsOverride(parsed.pannerOptions);
            } else if (!parsed.overridePanner) {
                player.setPannerOptionsOverride(undefined);
            }
        }
    }
}

export default setAudioParamsS2CPacket;