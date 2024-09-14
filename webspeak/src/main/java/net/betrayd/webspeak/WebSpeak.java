package net.betrayd.webspeak;

import io.javalin.Javalin;

public class WebSpeak {

    public static Javalin app;

    public static boolean someLibraryMethod() {
        return true;
    }

    public void start()
    {
        app = Javalin.create(/*config*/)
        .get("/", ctx -> ctx.result("Hello World"))
        .start(9090);
    }

    public String getTestString() {
        return "Hello World!";
    }
}
