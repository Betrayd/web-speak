module net.betrayd.webspeak {
    requires com.google.common;
    requires com.google.gson;
    requires transitive io.javalin;
    requires transitive io.javalin.community.ssl;

    exports net.betrayd.webspeak;
    exports net.betrayd.webspeak.util;
}
