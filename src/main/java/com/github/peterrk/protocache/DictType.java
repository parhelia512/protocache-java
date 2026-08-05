package com.github.peterrk.protocache;

abstract class DictType implements IUnit {
    private final static PerfectHash emptyIndex;

    static {
        byte[] empty = new byte[4];
        Data.putInt(empty, 0, 0);
        emptyIndex = new PerfectHash(empty);
    }

    /** Perfect-hash index for the encoded keys. */
    protected PerfectHash index;
    /** Byte offset of the first encoded key/value pair. */
    protected int bodyOffset;
    /** Encoded key width in bytes. */
    protected int keyWidth;
    /** Encoded value width in bytes. */
    protected int valueWidth;

    /**
     * Initializes a map view.
     *
     * @param data backing ProtoCache data
     * @param offset offset of the encoded map
     * @param keyWord expected key width in four-byte words, or {@code 0} for references
     * @param valueWord expected value width in four-byte words, or {@code 0} for references
     */
    protected void init(byte[] data, int offset, int keyWord, int valueWord) {
        if (offset < 0) {
            index = emptyIndex;
            bodyOffset = 4;
            keyWidth = keyWord * 4;
            valueWidth = valueWord * 4;
            return;
        }
        index = new PerfectHash(data, offset);
        bodyOffset = offset + ((index.byteSize + 3) & 0xfffffffc);
        int mark = Data.getInt(data, offset);
        keyWidth = ((mark >>> 30) & 3) * 4;
        valueWidth = ((mark >>> 28) & 3) * 4;
        if ((keyWord != 0 && keyWidth != keyWord*4) || keyWidth == 0
                || (valueWord != 0 && valueWidth != valueWord*4) || valueWidth == 0) {
            throw new IllegalArgumentException("illegal map");
        }
    }

    /**
     * Returns the number of entries in this map.
     *
     * @return entry count
     */
    public int size() {
        return index.getSize();
    }

    /**
     * Returns the encoded key offset for an entry.
     *
     * @param idx zero-based map storage index
     * @return byte offset in the backing data
     */
    protected int keyFieldOffset(int idx) {
        return bodyOffset + idx * (keyWidth + valueWidth);
    }

    /**
     * Returns the encoded value offset for an entry.
     *
     * @param idx zero-based map storage index
     * @return byte offset in the backing data
     */
    protected int valueFieldOffset(int idx) {
        return keyFieldOffset(idx) + keyWidth;
    }
}
