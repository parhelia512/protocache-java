// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

abstract class ArrayType implements IUnit {
    /** Backing ProtoCache data. */
    protected byte[] data;
    private int bodyOffset = -1;
    private int size;
    private int width;

    /**
     * Initializes an array view.
     *
     * @param data backing ProtoCache data
     * @param offset offset of the encoded array
     * @param word expected element width in four-byte words, or {@code 0} for references
     */
    protected void init(byte[] data, int offset, int word) {
        if (offset < 0) {
            this.data = null;
            this.bodyOffset = -1;
            size = 0;
            width = word * 4;
            return;
        }
        int mark = Data.getInt(data, offset);
        if ((word != 0 && (mark & 3) != word) || (mark & 3) == 0) {
            throw new IllegalArgumentException("illegal array");
        }
        size = mark >>> 2;
        width = (mark & 3) * 4;
        this.data = data;
        this.bodyOffset = offset + 4;
    }

    /**
     * Returns the number of elements in this array.
     *
     * @return element count
     */
    public int size() {
        return size;
    }

    /**
     * Returns the encoded field offset for an element.
     *
     * @param idx zero-based element index
     * @return byte offset in the backing data
     */
    protected int fieldOffset(int idx) {
        return bodyOffset + idx * width;
    }
}
