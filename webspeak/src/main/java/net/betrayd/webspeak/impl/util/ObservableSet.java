package net.betrayd.webspeak.impl.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import net.betrayd.webspeak.util.WebSpeakEvents;
import net.betrayd.webspeak.util.WebSpeakEvents.WebSpeakEvent;

public class ObservableSet<T> extends AbstractSet<T> {

    private final Set<T> base;
    

    public ObservableSet(Set<T> base) {
        if (base == null) {
            throw new NullPointerException("base");
        }
        this.base = base;
    }

    public final WebSpeakEvent<Consumer<T>> ON_ADDED = WebSpeakEvents.createSimple();
    public final WebSpeakEvent<Consumer<T>> ON_REMOVED = WebSpeakEvents.createSimple();

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return base.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new ObservableIterator(base.iterator());
    }

    @Override
    public Object[] toArray() {
        return base.toArray();
    }

    @Override
    public <F> F[] toArray(F[] a) {
        return base.toArray(a);
    }

    @Override
    public <F> F[] toArray(IntFunction<F[]> generator) {
        return base.toArray(generator);
    }

    @Override
    public boolean add(T e) {
        boolean added = base.add(e);
        if (added) {
            ON_ADDED.invoker().accept(e);
        }
        return added;
    }

    @Override
    @SuppressWarnings("unchecked") // remove is only successful if it's the right type
    public boolean remove(Object o) {
        boolean removed = base.remove(o);
        if (removed) {
            ON_REMOVED.invoker().accept((T) o);
        }
        return removed;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return base.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean success = false;
        for (var item : c) {
            if (add(item)) {
                success = true;
            }
        }
        return success;
    }

    @Override
    @SuppressWarnings("unchecked") // Item only in collection if it's the correct type.
    public void clear() {
        Object[] items = base.toArray();
        base.clear();
        for (var item : items) {
            ON_REMOVED.invoker().accept((T) item);
        }
    }

    private class ObservableIterator implements Iterator<T> {
        final Iterator<T> base;
        T currentVal;

        ObservableIterator(Iterator<T> base) {
            this.base = base;
        }

        @Override
        public boolean hasNext() {
            return base.hasNext();
        }

        @Override
        public T next() {
            currentVal = base.next();
            return currentVal;
        }

        @Override
        public void remove() {
            base.remove();
            ON_REMOVED.invoker().accept(currentVal);
        }
    }
}
