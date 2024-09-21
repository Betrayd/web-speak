module net.betrayd.webspeak {
    requires com.google.common;
    requires com.google.gson;
    requires transitive io.javalin;

    exports net.betrayd.webspeak;
    exports net.betrayd.webspeak.util;
}
