// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

/** A zero-deserialization view of an array of byte strings. */
public class BytesArray extends ArrayType {
    /** Creates an uninitialized, empty array view. */
    public BytesArray() {}

    @Override
    public void init(byte[] data, int offset) {
        init(data, offset, 0);
    }

    /**
     * Returns a copy of an element.
     *
     * @param idx element index; must satisfy {@code 0 <= idx && idx < size()}
     * @return decoded bytes
     */
    public byte[] get(int idx) {
        return Bytes.extractBytes(data, IUnit.jump(data, fieldOffset(idx)));
    }
}
