package net.betrayd.webspeak.impl.net;

import com.google.gson.Gson;

public abstract class S2CPacket<T> {
    private final String id;

    public S2CPacket(String id) {
        this.id = id;
    }

    public abstract String write(T val);

    public String getId() {
        return id;
    }

    public static class JsonS2CPacket<T> extends S2CPacket<T> {
        private static final Gson GSON = new Gson();

        public JsonS2CPacket(String id) {
            super(id);
        }

        @Override
        public String write(T val) {
            return GSON.toJson(val);
        }
    }

    public static class StringS2CPacket extends S2CPacket<String> {

        public StringS2CPacket(String id) {
            super(id);
        }

        @Override
        public String write(String val) {
            return val;
        }
        
    }
}