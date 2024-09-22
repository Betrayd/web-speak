package net.betrayd.webspeak.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class WebSpeakEvents {
    public static interface WebSpeakEvent<T> {
        void addListener(T listener);
        T invoker();
    }

    public static <T> WebSpeakEvent<Consumer<T>> createSimple() {
        return new SimpleEvent<>();
    }

    public static <T> WebSpeakEvent<T> createArrayBacked(Function<List<T>, T> invokerFactory) {
        return new ArrayBackedEvent<>(invokerFactory);
    }

    private static class SimpleEvent<T> implements WebSpeakEvent<Consumer<T>> {

        List<Consumer<T>> listeners = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void addListener(Consumer<T> listener) {
            listeners.add(listener);
        }

        @Override
        public Consumer<T> invoker() {
            return val -> {
                // Make copy for thread safety and in case a listener tries to modify list.
                for (var l : List.copyOf(listeners)) {
                    l.accept(val);
                }
            };
        }
    }

    private static class ArrayBackedEvent<T> implements WebSpeakEvent<T> {
        private Function<List<T>, T> invokerFactory;
        private List<T> listeners = new ArrayList<>();
        private T invoker;

        ArrayBackedEvent(Function<List<T>, T> invokerFactory) {
            this.invokerFactory = invokerFactory;
            invoker = invokerFactory.apply(Collections.emptyList());
        }

        @Override
        public synchronized void addListener(T listener) {
            listeners.add(listener);
            // Remember the lambda of the invoker is going to store this.
            // Make copy for thread safety and in case a listener tries to modify list.
            invoker = invokerFactory.apply(List.copyOf(listeners));
        }

        @Override
        public T invoker() {
            return invoker;
        }
    }
}