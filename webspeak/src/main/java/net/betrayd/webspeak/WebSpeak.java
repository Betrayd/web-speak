package net.betrayd.webspeak;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;

import io.javalin.Javalin;
import io.javalin.websocket.WsConnectContext;

public class WebSpeak {

    public Javalin app;

    private Map<String, String> avalibleSessions = new HashMap<>();
    private Map<Session, String> connectedUsers = new HashMap<>();

    public static boolean someLibraryMethod() {
        return true;
    }

    public void start()
    {
        app = Javalin.create(/*config*/)
        .get("/", ctx -> ctx.result("Hello World: " + ctx))
        .ws("/{session}", ws -> {
            ws.onConnect(ctx -> {
                playerConnection(ctx.pathParam("session"), ctx);
            });
        })
        .start(9090);
    }

    public void stop()
    {
        app.stop();
    }

    public String getTestString() {
        return "Hello World!";
    }

    private void playerConnection(String session, WsConnectContext ctx)
    {
        //TODO: double check this is okay 
        //careful about this because we are dirrectly logging data sent by the user. just don't let this be read by some kinda parser
        System.out.println("Attempted connection with session: " + session);

        String uuid = avalibleSessions.get(session);
        if(uuid != null)
        {
            System.out.println("Connection with session: " + session + ", refused.");
            ctx.send("The session request you sent has either already been taken or does not exist");
            ctx.closeSession();
            return;
        }
        avalibleSessions.remove(session);
        connectedUsers.put(ctx.session, uuid);

        //ctx.
    }
}
