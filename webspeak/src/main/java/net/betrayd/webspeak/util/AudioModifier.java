package net.betrayd.webspeak.util;

/**
 * <p>
 * A collection of modifiers that may be applied to the audio of another player.
 * All values are optional, and omitted values will revert to default or a
 * lower-priority modifier.
 * </p>
 * <p>
 * Modifiers may be applied in various contexts. The priority will be as
 * follows, with higher-priority overriding values from lower-priority:
 * </p>
 * <ol>
 * <li>Player -> Player</li>
 * <li>Player -> Channel</li>
 * <li>Channel -> Player</li>
 * <li>Channel -> Channel</li>
 * </ol>
 * 
 * @param spatialized   Whether spatialize the audio of the target player.
 *                      Default: <code>true</code>
 * @param muted         Mute the audio of the target player, while remaining in
 *                      scope. Default: <code>false</code>
 * @param pannerOptions An optional panner options override to use. Default:
 *                      <code>null</code>
 */
public record AudioModifier(Boolean spatialized, Boolean muted) {

    /**
     * The default audio modifier.
     */
    public static final AudioModifier DEFAULT = new AudioModifier(true, false);
    
    /**
     * An empty audio modifier. The client will populate default values.
     */
    public static final AudioModifier EMPTY = new AudioModifier(null, null);
    
    /**
     * Append another audio modifier to this one. The passed modifier will override
     * this modifier with any values it has set.
     * 
     * @param other The higher-priority modifier.
     * @return The combined audio modifier.
     */
    public AudioModifier append(AudioModifier other) {
        if (other == null)
            return this;

        return new AudioModifier(other.spatialized != null ? other.spatialized : this.spatialized,
                other.muted != null ? other.muted : this.muted);
    }
}
