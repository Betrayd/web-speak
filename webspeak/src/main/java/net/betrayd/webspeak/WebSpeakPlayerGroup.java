package net.betrayd.webspeak;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


import net.betrayd.webspeak.util.AudioModifier;
import net.betrayd.webspeak.util.WebSpeakEvents;
import net.betrayd.webspeak.util.WebSpeakEvents.WebSpeakEvent;

public class WebSpeakPlayerGroup {
    private final Set<WebSpeakPlayer> players = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_INVALIDATE_MODIFIER = WebSpeakEvents.createSimple();
    public final WebSpeakEvent<Consumer<Void>> ON_INVALIDATE_GLOBAL = WebSpeakEvents.createSimple();

    private AudioModifier globalModifier = AudioModifier.EMPTY;
    // No need to keep track of players & groups when they're no longer used.
    private final Map<WebSpeakPlayer, AudioModifier> playerModifiers = new WeakHashMap<>();
    private final Map<WebSpeakPlayerGroup, AudioModifier> groupModifiers = new WeakHashMap<>();

    
    protected boolean onAddPlayer(WebSpeakPlayer player) {
        return players.add(player);
    }

    protected boolean onRemovePlayer(WebSpeakPlayer player) {
        return players.remove(player);
    }

    public Set<WebSpeakPlayer> getPlayers() {
        return players;
    }
    
    public AudioModifier getGlobalModifier() {
        return globalModifier;
    }

    public void setGlobalModifier(AudioModifier audioModifier) {
        this.globalModifier = audioModifier;
        ON_INVALIDATE_GLOBAL.invoker().accept(null);
    }
    
    public Map<WebSpeakPlayerGroup, AudioModifier> getGroupModifiers() {
        return Collections.unmodifiableMap(groupModifiers);
    }

    public AudioModifier getGroupModifier(WebSpeakPlayerGroup group) {
        return groupModifiers.get(group);
    }

    /**
     * Return the audio modifier that is the result of a list of groups. Groups with
     * a higher index override groups with a lower index.
     * 
     * @param groups Group list.
     * @return Combined audio modifier.
     */
    public AudioModifier getGroupListModifier(List<? extends WebSpeakPlayerGroup> groups) {
        AudioModifier modifier = AudioModifier.EMPTY;
        for (var group : groups) {
            AudioModifier groupModifier = groupModifiers.get(group);
            if (groupModifier != null) {
                AudioModifier.combine(groupModifier, modifier);
            }
        }
        return modifier;
    }

    public AudioModifier setGroupAudioModifier(WebSpeakPlayerGroup group, AudioModifier modifier) {
        if (group == null)
            throw new NullPointerException("group");

        AudioModifier old;
        if (modifier == null) {
            old = groupModifiers.remove(group);
        } else {
            old = groupModifiers.put(group, modifier);
        }

        if (!Objects.equals(modifier, old)) {
            for (var player : group.players) {
                ON_INVALIDATE_MODIFIER.invoker().accept(player);
            }
        }
        return old;
    }

    public Map<WebSpeakPlayer, AudioModifier> getPlayerModifiers() {
        return Collections.unmodifiableMap(playerModifiers);
    }

    /**
     * Return the audio modifier a player should use according to this group.
     * @param player Target player.
     * @return The combined audio modifier.
     */
    public AudioModifier getPlayerAudioModifier(WebSpeakPlayer player) {
        AudioModifier modifier = AudioModifier.EMPTY;
        for (var group : player.getGroups()) {
            AudioModifier groupModifier = groupModifiers.get(group);
            if (groupModifier != null) {
                modifier = AudioModifier.combine(groupModifier, modifier);
            }
        }
        AudioModifier playerModifier = playerModifiers.get(player);
        if (playerModifier != null) {
            modifier = AudioModifier.combine(playerModifier, modifier);
        }

        return AudioModifier.combine(globalModifier, modifier);
    }

    /**
     * Get a collection of players that will be modified when the subject player is
     * in this group.
     * 
     * @return Player list.
     */
    public Collection<WebSpeakPlayer> getModifiedPlayers() {
        Set<WebSpeakPlayer> set = new HashSet<>();
        set.addAll(playerModifiers.keySet());
        for (var group : groupModifiers.keySet()) {
            set.addAll(group.players);
        }
        return set;
    }
}
