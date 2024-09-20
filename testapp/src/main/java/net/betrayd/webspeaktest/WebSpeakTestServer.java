package net.betrayd.webspeaktest;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.WebSpeakServer;

public class WebSpeakTestServer implements Executor {
    public static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak Server");

    private final Thread thread;
    private final int port;
    private WebSpeakServer webSpeakServer;

    private boolean shutdownQueued;
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    private Queue<Runnable> queue = new ConcurrentLinkedDeque<>();

    public WebSpeakTestServer(int port) {
        this.port = port;
        thread = new Thread(this::runThread, "WebSpeak Server");
        thread.setDaemon(false);
        thread.start();
    }

    @Override
    public void execute(Runnable command) {
        queue.add(command);
    }

    public WebSpeakServer getWebSpeakServer() {
        return webSpeakServer;
    }

    public boolean isOnThread() {
        return Thread.currentThread().equals(thread);
    }

    public void assertOnThread() {
        if (!isOnThread())
            throw new IllegalStateException("WebSpeak server called from incorrect thread: " + Thread.currentThread().getName());
    }

    protected void runThread() {
        webSpeakServer = new WebSpeakServer();
        webSpeakServer.start(port);
        while (!shutdownQueued) {
            Runnable task;
            while ((task = queue.poll()) != null) {
                task.run();
            }

            tick();
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                LOGGER.info("WebSpeak Server was inturrupted during sleep.");
            }
        }

        webSpeakServer.stop();
        shutdownFuture.complete(null);
    }

    protected void tick() {
        webSpeakServer.tick();
    }
    
    public boolean isShutdownQueued() {
        return shutdownQueued;
    }

    public void shutdown() {
        shutdownQueued = true;
    }

    public final CompletableFuture<Void> getShutdownFuture() {
        return shutdownFuture;
    }
    
    public final CompletableFuture<Void> shutdownAsync() {
        shutdown();
        return shutdownFuture;
    }
    
    public final void shutdownAndWait() throws InterruptedException {
        shutdown();
        thread.join();
    }
}
