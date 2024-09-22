package net.betrayd.webspeak.util;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A simple vector class for use in WebSpeak
 */
@JsonAdapter(WebSpeakVectorJson.class)
public record WebSpeakVector(double x, double y, double z) {
    public static WebSpeakVector fromArray(double[] array) {
        return new WebSpeakVector(array[0], array[1], array[2]);
    }

    public static final WebSpeakVector ZERO = new WebSpeakVector(0, 0, 0);
}

class WebSpeakVectorJson extends TypeAdapter<WebSpeakVector> {

    @Override
    public void write(JsonWriter out, WebSpeakVector value) throws IOException {
        out.beginArray();
        out.value(value.x());
        out.value(value.y());
        out.value(value.z());
        out.endArray();
    }

    @Override
    public WebSpeakVector read(JsonReader in) throws IOException {
        in.beginArray();
        double x = in.nextDouble();
        double y = in.nextDouble();
        double z = in.nextDouble();
        in.endArray();
        return new WebSpeakVector(x, y, z);
    }
    
}