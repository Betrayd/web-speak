package net.betrayd.webspeak.net;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.betrayd.webspeak.WebSpeakPlayer;

public interface PacketType<T> {

    public abstract String write(T packet);
    public abstract T read(String serialized);
    public abstract void apply(WebSpeakPlayer player, T packet);
    
    public static final class SimplePacketType<T> implements PacketType<T> {
        private final Function<T, String> writeFunction;
        private final Function<String, T> readFunction;
        private final BiConsumer<WebSpeakPlayer, T> applicator;

        public SimplePacketType(String id, Function<T, String> writeFunction, Function<String, T> readFunction, BiConsumer<WebSpeakPlayer, T> applicator) {
            this.writeFunction = writeFunction;
            this.readFunction = readFunction;
            this.applicator = applicator;
        }

        @Override
        public String write(T packet) {
            return writeFunction.apply(packet);
        }
        

        @Override
        public T read(String serialized) {
            return readFunction.apply(serialized);
        }

        @Override
        public void apply(WebSpeakPlayer player, T packet) {
            applicator.accept(player, packet);
        }
    }

    public static final class WriteOnlyPacketType<T> implements PacketType<T> {

        private final Function<T, String> writeFunction;

        public WriteOnlyPacketType(Function<T, String> writeFunction) {
            this.writeFunction = writeFunction;
        }

        @Override
        public String write(T packet) {
            return writeFunction.apply(packet);
        }

        @Override
        public T read(String serialized) {
            throw new UnsupportedOperationException("This packet may not be sent from the client to the server.");
        }

        @Override
        public void apply(WebSpeakPlayer player, T packet) {
            throw new UnsupportedOperationException("This packet cannot be applied on the server.");
        }
        
    }

    public static abstract class JsonPacketType<T> implements PacketType<T> {
        private final Gson gson;
        private final Class<T> clazz;
        private final TypeToken<T> typeToken;

        public JsonPacketType(Gson gson, Class<T> clazz) {
            this.gson = gson;
            this.clazz = clazz;
            this.typeToken = null;
        }

        public JsonPacketType(Gson gson, TypeToken<T> typeToken) {
            this.gson = gson;
            this.clazz = null;
            this.typeToken = typeToken;
        }

        @Override
        public String write(T packet) {
            return gson.toJson(packet);
        }

        @Override
        public T read(String serialized) {
            if (typeToken != null) {
                return gson.fromJson(serialized, typeToken);
            } else if (clazz != null) {
                return gson.fromJson(serialized, clazz);
            } else {
                throw new IllegalStateException("Somehow, neither class nor type token is valid.");
            }
        }
    }

    public static final class SimpleJsonPacketType<T> extends JsonPacketType<T> {

        private final BiConsumer<WebSpeakPlayer, T> applicator;

        public SimpleJsonPacketType(Gson gson, Class<T> clazz, BiConsumer<WebSpeakPlayer, T> applicator) {
            super(gson, clazz);
            this.applicator = applicator;
        }

        public SimpleJsonPacketType(Gson gson, TypeToken<T> typeToken, BiConsumer<WebSpeakPlayer, T> applicator) {
            super(gson, typeToken);
            this.applicator = applicator;
        }

        @Override
        public void apply(WebSpeakPlayer player, T packet) {
            applicator.accept(player, packet);
        }
        
    }

    public static final class WriteOnlyJsonPacketType<T> extends JsonPacketType<T> {

        public WriteOnlyJsonPacketType(Gson gson, Class<T> clazz) {
            super(gson, clazz);
        }

        public WriteOnlyJsonPacketType(Gson gson, TypeToken<T> typeToken) {
            super(gson, typeToken);
        }

        @Override
        public void apply(WebSpeakPlayer player, T packet) {
            throw new UnsupportedOperationException("This packet cannot be applied on the server.");
        }
        
    }
}
