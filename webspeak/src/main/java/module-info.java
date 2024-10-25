module net.betrayd.webspeak {
    requires com.google.common;
    requires com.google.gson;
    requires org.eclipse.jetty.ee10.websocket.jetty.server;
    requires org.eclipse.jetty.websocket.client;

    exports net.betrayd.webspeak;
    exports net.betrayd.webspeak.util;
}
