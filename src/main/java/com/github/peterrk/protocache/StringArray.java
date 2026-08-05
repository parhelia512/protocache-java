// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

/** A zero-deserialization view of an array of UTF-8 strings. */
public class StringArray extends ArrayType {
    /** Creates an uninitialized, empty array view. */
    public StringArray() {}

    @Override
    public void init(byte[] data, int offset) {
        init(data, offset, 0);
    }

    /**
     * Returns an element decoded as UTF-8.
     *
     * @param idx zero-based element index
     * @return decoded string
     * @throws IndexOutOfBoundsException if {@code idx} is out of range
     */
    public String get(int idx) {
        return Bytes.extractString(data, IUnit.jump(data, fieldOffset(idx)));
    }
}
