// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

/** A zero-deserialization view of an array of 32-bit integer values. */
public class Int32Array extends ArrayType {
    /** Creates an uninitialized, empty array view. */
    public Int32Array() {}

    @Override
    public void init(byte[] data, int offset) {
        init(data, offset, 1);
    }

    /**
     * Returns an element.
     *
     * @param idx element index; must satisfy {@code 0 <= idx && idx < size()}
     * @return the 32-bit integer value
     */
    public int get(int idx) {
        return Data.getInt(data, fieldOffset(idx));
    }
}