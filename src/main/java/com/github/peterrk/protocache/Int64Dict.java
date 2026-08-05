package com.github.peterrk.protocache;

/** A zero-deserialization view of a ProtoCache map with 64-bit integer keys. */
public abstract class Int64Dict extends DictType {
    /** Creates an uninitialized, empty map view. */
    public Int64Dict() {}
    private byte[] tmp = null;

    /**
     * Initializes a map view.
     *
     * @param data backing ProtoCache data
     * @param offset offset of the encoded map
     * @param word expected value width in four-byte words, or {@code 0} for references
     */
    protected void init(byte[] data, int offset, int word) {
        init(data, offset, 2, word);
    }

    /**
     * Returns the key stored at an index.
     *
     * @param idx zero-based map storage index
     * @return key at the index
     * @throws IndexOutOfBoundsException if {@code idx} is out of range
     */
    public long getKey(int idx) {
        return Data.getLong(index.data, keyFieldOffset(idx));
    }

    /**
     * Finds the storage index of a key.
     *
     * @param key key to find
     * @return matching index, or {@code -1} when absent
     */
    public int find(long key) {
        if (tmp == null) {
            tmp = new byte[8];
        }
        Data.putLong(tmp, 0, key);
        int idx = index.locate(tmp);
        if (idx >= index.getSize() || key != this.getKey(idx)) {
            return -1;
        }
        return idx;
    }

    /** A map view with boolean values. */
    public static class BoolValue extends Int64Dict {
        /** Creates an uninitialized, empty map view. */
        public BoolValue() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx zero-based map storage index
         * @return value at the index
         * @throws IndexOutOfBoundsException if {@code idx} is out of range
         */
        public boolean getValue(int idx) {
            return index.data[valueFieldOffset(idx)] != 0;
        }
    }

    /** A map view with 32-bit integer values. */
    public static class Int32Value extends Int64Dict {
        /** Creates an uninitialized, empty map view. */
        public Int32Value() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx zero-based map storage index
         * @return value at the index
         * @throws IndexOutOfBoundsException if {@code idx} is out of range
         */
        public int getValue(int idx) {
            return Data.getInt(index.data, valueFieldOffset(idx));
        }
    }

    /** A map view with 64-bit integer values. */
    public static class Int64Value extends Int64Dict {
        /** Creates an uninitialized, empty map view. */
        public Int64Value() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 2);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx zero-based map storage index
         * @return value at the index
         * @throws IndexOutOfBoundsException if {@code idx} is out of range
         */
        public long getValue(int idx) {
            return Data.getLong(index.data, valueFieldOffset(idx));
        }
    }

    /** A map view with 32-bit floating-point values. */
    public static class Float32Value extends Int64Dict {
        /** Creates an uninitialized, empty map view. */
        public Float32Value() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx zero-based map storage index
         * @return value at the index
         * @throws IndexOutOfBoundsException if {@code idx} is out of range
         */
        public float getValue(int idx) {
            return Data.getFloat(index.data, valueFieldOffset(idx));
        }
    }

    /** A map view with 64-bit floating-point values. */
    public static class Float64Value extends Int64Dict {
        /** Creates an uninitialized, empty map view. */
        public Float64Value() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 2);
        }
        /**
         * Returns the value stored at an index.
         *
         * @param idx zero-based map storage index
         * @return value at the index
         * @throws IndexOutOfBoundsException if {@code idx} is out of range
         */
        public double getValue(int idx) {
            return Data.getDouble(index.data, valueFieldOffset(idx));
        }
    }

    /** A map view with UTF-8 string values. */
    public static class StringValue extends Int64Dict {
        /** Creates an uninitialized, empty map view. */
        public StringValue() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        /**
         * Returns the value stored at an index.
         *
         * @param idx zero-based map storage index
         * @return value at the index
         * @throws IndexOutOfBoundsException if {@code idx} is out of range
         */
        public String getValue(int idx) {
            return Bytes.extractString(index.data, IUnit.jump(index.data, valueFieldOffset(idx)));
        }
    }

    /** A map view with byte-string values. */
    public static class BytesValue extends Int64Dict {
        /** Creates an uninitialized, empty map view. */
        public BytesValue() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        /**
         * Returns the value stored at an index.
         *
         * @param idx zero-based map storage index
         * @return value at the index
         * @throws IndexOutOfBoundsException if {@code idx} is out of range
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
    public static class ObjectValue<V extends IUnit> extends Int64Dict {
        /** Creates an uninitialized, empty map view. */
        public ObjectValue() {}
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        /**
         * Initializes and returns {@code unit} as a view of the value at an index.
         *
         * @param idx zero-based map storage index
         * @param unit object view to initialize
         * @return {@code unit}
         * @throws IndexOutOfBoundsException if {@code idx} is out of range
         */
        public V getValue(int idx, V unit) {
            return IUnit.initByField(index.data, valueFieldOffset(idx), unit);
        }
    }
}
