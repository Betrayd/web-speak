/**
 * A collection of modifiers that may be applied to the audio of another player.
 */
export default interface AudioModifier {

    /**
     * Whether spatialize the audio of the target player.
     */
    spatialized: boolean,

    /**
     * Mute the audio of the target player, while remaining in scope.
     */
    muted: boolean
}