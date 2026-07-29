package com.github.peterrk.protocache;

public abstract class Int64Dict extends DictType {
    private byte[] tmp = null;

    protected void init(byte[] data, int offset, int word) {
        init(data, offset, 2, word);
    }

    public long getKey(int idx) {
        return Data.getLong(index.data, keyFieldOffset(idx));
    }

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

    public static class BoolValue extends Int64Dict {
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        public boolean getValue(int idx) {
            return index.data[valueFieldOffset(idx)] != 0;
        }
    }

    public static class Int32Value extends Int64Dict {
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        public int getValue(int idx) {
            return Data.getInt(index.data, valueFieldOffset(idx));
        }
    }

    public static class Int64Value extends Int64Dict {
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 2);
        }
        public long getValue(int idx) {
            return Data.getLong(index.data, valueFieldOffset(idx));
        }
    }

    public static class Float32Value extends Int64Dict {
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 1);
        }
        public float getValue(int idx) {
            return Data.getFloat(index.data, valueFieldOffset(idx));
        }
    }

    public static class Float64Value extends Int64Dict {
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 2);
        }
        public double getValue(int idx) {
            return Data.getDouble(index.data, valueFieldOffset(idx));
        }
    }

    public static class StringValue extends Int64Dict {
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        public String getValue(int idx) {
            return Bytes.extractString(index.data, IUnit.jump(index.data, valueFieldOffset(idx)));
        }
    }

    public static class BytesValue extends Int64Dict {
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        public byte[] getValue(int idx) {
            return Bytes.extractBytes(index.data, IUnit.jump(index.data, valueFieldOffset(idx)));
        }
    }

    public static class ObjectValue<V extends IUnit> extends Int64Dict {
        @Override
        public void init(byte[] data, int offset) {
            init(data, offset, 0);
        }

        public V getValue(int idx, V unit) {
            return IUnit.initByField(index.data, valueFieldOffset(idx), unit);
        }
    }
}
