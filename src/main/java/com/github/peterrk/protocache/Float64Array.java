// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

/** A zero-deserialization view of an array of 64-bit floating-point values. */
public class Float64Array extends ArrayType {
    /** Creates an uninitialized, empty array view. */
    public Float64Array() {}

    @Override
    public void init(byte[] data, int offset) {
        init(data, offset, 2);
    }

    /**
     * Returns an element.
     *
     * @param idx zero-based element index
     * @return the 64-bit floating-point value
     * @throws IndexOutOfBoundsException if {@code idx} is out of range
     */
    public double get(int idx) {
        return Data.getDouble(data, fieldOffset(idx));
    }
}