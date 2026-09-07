// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

/** A zero-deserialization view of an array of 32-bit floating-point values. */
public class Float32Array extends ArrayType {
    /** Creates an uninitialized, empty array view. */
    public Float32Array() {}

    @Override
    public void init(byte[] data, int offset) {
        init(data, offset, 1);
    }

    /**
     * Returns an element.
     *
     * @param idx element index; must satisfy {@code 0 <= idx && idx < size()}
     * @return the 32-bit floating-point value
     */
    public float get(int idx) {
        return Data.getFloat(data, fieldOffset(idx));
    }
}