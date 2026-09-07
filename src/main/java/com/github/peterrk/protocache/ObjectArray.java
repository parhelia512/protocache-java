package com.github.peterrk.protocache;

/**
 * A zero-deserialization view of an array of ProtoCache objects.
 *
 * @param <T> generated object view type
 */
public class ObjectArray<T extends IUnit> extends ArrayType {
    /** Creates an uninitialized, empty array view. */
    public ObjectArray() {}

    @Override
    public void init(byte[] data, int offset) {
        init(data, offset, 0);
    }

    /**
     * Initializes and returns {@code unit} as a view of an element.
     * Reusing a unit avoids allocating one object per access.
     *
     * @param idx element index; must satisfy {@code 0 <= idx && idx < size()}
     * @param unit object view to initialize
     * @return {@code unit}
     */
    public T get(int idx, T unit) {
        return IUnit.initByField(data, fieldOffset(idx), unit);
    }
}
