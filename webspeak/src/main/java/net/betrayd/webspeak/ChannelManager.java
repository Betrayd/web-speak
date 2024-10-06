package net.betrayd.webspeak;


import java.util.Collection;

import net.betrayd.webspeak.impl.RelationGraph;

public final class ChannelManager {
    private final WebSpeakServer server;

    public ChannelManager(WebSpeakServer server) {
        this.server = server;
    }

    public WebSpeakServer getServer() {
        return server;
    }

    private final RelationGraph<WebSpeakChannel> channelConnections = new RelationGraph<>();

    public boolean areChannelsConnected(WebSpeakChannel a, WebSpeakChannel b) {
        return channelConnections.containsRelation(a, b);
    }

    public void setChannelsConnected(WebSpeakChannel a, WebSpeakChannel b, boolean connected) {
        if (a.getServer() != server) {
            throw new IllegalArgumentException("Channel a belongs to the wrong server.");
        }
        if (b.getServer() != server) {
            throw new IllegalArgumentException("Channel b belongs to the wrong server.");
        }
        if (connected) {
            if (channelConnections.add(a, b)) {
                onConnectChannels(a, b);
            }
        } else {
            if (channelConnections.remove(a, b)) {
                onDisconnectChannels(a, b);
            }
        }
    }

    public void connectChannels(WebSpeakChannel a, WebSpeakChannel b) {
        setChannelsConnected(a, b, true);
    }
    
    public void disconnectChannels(WebSpeakChannel a, WebSpeakChannel b) {
        setChannelsConnected(a, b, false);
    }

    /**
     * Remove all connections to a channel.
     * @param channel Channel to remove.
     */
    public void removeAll(WebSpeakChannel channel) {
        Collection<WebSpeakChannel> relations = channelConnections.getRelations(channel);
        if (channelConnections.removeAll(channel)) {
            for (var other : relations) {
                onDisconnectChannels(channel, other);
            }
        };
    }

    public Collection<WebSpeakChannel> getConnectedChannels(WebSpeakChannel channel) {
        return channelConnections.getRelations(channel);
    }

    private void onConnectChannels(WebSpeakChannel a, WebSpeakChannel b) {
        a.onConnectTo(b);
        b.onConnectTo(a);
    }

    private void onDisconnectChannels(WebSpeakChannel a, WebSpeakChannel b) {
        a.onDisconnectFrom(b);
        b.onDisconnectFrom(a);
    }
}
