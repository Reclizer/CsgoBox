package com.reclizer.csgobox.utils;

import java.util.function.Supplier;

/** Cache only the current hovered entry; refresh dynamic descriptions at most every 250 ms. */
public final class HoverCache<T> {
    private Object key;
    private int context;
    private long refreshed;
    private T value;

    public T get(Object key, int context, long now, Supplier<T> build) {
        if (this.key != key || this.context != context || value == null || now < refreshed || now - refreshed >= 250) {
            value = build.get();
            this.key = key;
            this.context = context;
            refreshed = now;
        }
        return value;
    }

    public void clear() { key = null; value = null; }
}
