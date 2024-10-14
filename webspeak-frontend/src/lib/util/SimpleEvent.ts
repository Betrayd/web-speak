export type EventListener<T> = (val: T) => void;

export default class SimpleEvent<T> {
    private listeners: Set<EventListener<T>> = new Set();

    public addListener(listener: EventListener<T>) {
        this.listeners.add(listener);
    }

    public removeListener(listener: EventListener<T> | any) {
        return this.listeners.delete(listener);
    }

    public dispatch(val: T) {
        for (let listener of this.listeners) {
            listener(val);
        }
    }
}