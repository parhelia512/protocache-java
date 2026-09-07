package com.github.peterrk.protocache;

/** A zero-deserialization view of a ProtoCache map with 32-bit integer keys. */
public abstract class Int32Dict extends DictType {
    /** Creates an uninitialized map view; call {@code init} before access. */
    public Int32Dict() {}
    private byte[] tmp = null;

    /**
     * Initializes a map view.
     *
     * @param data backing ProtoCache data
     * @param offset offset of the encoded map
     * @param word expected value width in four-byte words, or {@code 0} for references
     */
    protected void init(byte[] data, int offset, int word) {
        init(data, offset, 1, word);
    }

    /**
     * Returns the key stored at an index.
     *
     * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
     * @return key at the index
     */
    public int getKey(int idx) {
        return Data.getInt(index.data, keyFieldOffset(idx));
    }

    /**
     * Finds the storage index of a key.
     *
     * @param key key to find
     * @return matching index, or {@code -1} when absent
     */
    public int find(int key) {
        if (tmp == null) {
            tmp = new byte[4];
        }
        Data.putInt(tmp, 0, key);
        int idx = index.locate(tmp);
        if (idx >= index.getSize() || key != this.getKey(idx)) {
            return -1;
        }
        return idx;
    }

    /** A map view with boolean values. */
    public static class BoolValue extends Int32Dict {
        /** Creates an uninitialized map view; call {@code init} before access. */
        public BoolValue() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
         * @return value at the index
         */
        public boolean getValue(int idx) {
            return index.data[valueFieldOffset(idx)] != 0;
        }
    }

    /** A map view with 32-bit integer values. */
    public static class Int32Value extends Int32Dict {
        /** Creates an uninitialized map view; call {@code init} before access. */
        public Int32Value() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
         * @return value at the index
         */
        public int getValue(int idx) {
            return Data.getInt(index.data, valueFieldOffset(idx));
        }
    }

    /** A map view with 64-bit integer values. */
    public static class Int64Value extends Int32Dict {
        /** Creates an uninitialized map view; call {@code init} before access. */
        public Int64Value() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 2);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
         * @return value at the index
         */
        public long getValue(int idx) {
            return Data.getLong(index.data, valueFieldOffset(idx));
        }
    }

    /** A map view with 32-bit floating-point values. */
    public static class Float32Value extends Int32Dict {
        /** Creates an uninitialized map view; call {@code init} before access. */
        public Float32Value() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
         * @return value at the index
         */
        public float getValue(int idx) {
            return Data.getFloat(index.data, valueFieldOffset(idx));
        }
    }

    /** A map view with 64-bit floating-point values. */
    public static class Float64Value extends Int32Dict {
        /** Creates an uninitialized map view; call {@code init} before access. */
        public Float64Value() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 2);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
         * @return value at the index
         */
        public double getValue(int idx) {
            return Data.getDouble(index.data, valueFieldOffset(idx));
        }
    }

    /** A map view with UTF-8 string values. */
    public static class StringValue extends Int32Dict {
        /** Creates an uninitialized map view; call {@code init} before access. */
        public StringValue() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        /**
         * Returns the value stored at an index.
         *
         * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
         * @return value at the index
         */
        public String getValue(int idx) {
            return Bytes.extractString(index.data, IUnit.jump(index.data, valueFieldOffset(idx)));
        }
    }

    /** A map view with byte-string values. */
    public static class BytesValue extends Int32Dict {
        /** Creates an uninitialized map view; call {@code init} before access. */
        public BytesValue() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        /**
         * Returns the value stored at an index.
         *
         * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
         * @return value at the index
         */
        public byte[] getValue(int idx) {
            return Bytes.extractBytes(index.data, IUnit.jump(index.data, valueFieldOffset(idx)));
        }
    }

    /**
     * A map view with generated ProtoCache object values.
     *
     * @param <V> generated object view type
     */
    public static class ObjectValue<V extends IUnit> extends Int32Dict {
        /** Creates an uninitialized map view; call {@code init} before access. */
        public ObjectValue() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        /**
         * Initializes and returns {@code unit} as a view of the value at an index.
         *
         * @param idx map storage index; must satisfy {@code 0 <= idx && idx < size()}
         * @param unit object view to initialize
         * @return {@code unit}
         */
        public V getValue(int idx, V unit) {
            return IUnit.initByField(index.data, valueFieldOffset(idx), unit);
        }
    }
}
