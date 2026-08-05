// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

/**
 * Base class for schema-generated, zero-deserialization ProtoCache message views.
 * A view retains the supplied byte array; callers must not mutate it while the view is in use.
 */
public class Message implements IUnit {
    private final static byte[] empty = new byte[4];
    private final static byte[] emptyBytes = new byte[0];

    static {
        Data.putInt(empty, 0, 0);
    }

    private byte[] data;
    private int offset = -1;

    /** Creates an uninitialized view whose fields have their default values. */
    public Message() {}

    /**
     * Creates a view over an encoded ProtoCache message.
     *
     * @param data backing ProtoCache data
     * @param offset byte offset of the encoded message
     */
    public Message(byte[] data, int offset) {
        init(data, offset);
    }

    @Override
    public void init(byte[] data, int offset) {
        if (offset < 0) {
            this.data = empty;
            this.offset = 0;
            return;
        }
        this.data = data;
        this.offset = offset;
    }

    /**
     * Reports whether a zero-based field slot is present.
     *
     * @param id zero-based field slot
     * @return {@code true} when the field is present
     */
    public boolean hasField(int id) {
        int section = (int) data[offset] & 0xff;
        if (id < 12) {
            int v = Data.getInt(data, offset) >>> 8;
            int width = (v >>> (id << 1)) & 3;
            return width != 0;
        } else {
            int a = (id - 12) / 25;
            int b = (id - 12) % 25;
            if (a >= section) {
                return false;
            }
            long v = Data.getLong(data, offset + 4 + a * 8);
            int width = (int) (v >>> (b << 1)) & 3;
            return width != 0;
        }
    }

    private int calcFieldOffset(int id) {
        int section = (int) data[offset] & 0xff;
        int off = 1 + section * 2;
        if (id < 12) {
            int v = Data.getInt(data, offset) >>> 8;
            int width = (v >>> (id << 1)) & 3;
            if (width == 0) {
                return -1;
            }
            v &= ~(0xffffffff << (id << 1));
            v = (v & 0x33333333) + ((v >>> 2) & 0x33333333);
            v = v + (v >>> 4);
            v = (v & 0xf0f0f0f) + ((v >>> 8) & 0xf0f0f0f);
            v = v + (v >>> 16);
            off += (v & 0xff);
        } else {
            int a = (id - 12) / 25;
            int b = (id - 12) % 25;
            if (a >= section) {
                return -1;
            }
            long v = Data.getLong(data, offset + 4 + a * 8);
            int width = (int) (v >>> (b << 1)) & 3;
            if (width == 0) {
                return -1;
            }
            off += (int) (v >>> 50);
            v &= ~(0xffffffffffffffffL << (b << 1));
            v = (v & 0x3333333333333333L) + ((v >>> 2) & 0x3333333333333333L);
            v = v + (v >>> 4);
            v = (v & 0xf0f0f0f0f0f0f0fL) + ((v >>> 8) & 0xf0f0f0f0f0f0f0fL);
            v = v + (v >>> 16);
            v = v + (v >>> 32);
            off += ((int) v & 0xff);
        }
        return offset + off * 4;
    }

    /**
     * Reads a boolean field.
     *
     * @param id zero-based field slot
     * @return field value, or {@code false} when absent
     */
    public boolean fetchBool(int id) {
        int fieldOffset = calcFieldOffset(id);
        if (fieldOffset < 0) {
            return false;
        }
        return data[fieldOffset] != 0;
    }

    /**
     * Reads a 32-bit integer or enum field.
     *
     * @param id zero-based field slot
     * @return field value, or {@code 0} when absent
     */
    public int fetchInt32(int id) {
        int fieldOffset = calcFieldOffset(id);
        if (fieldOffset < 0) {
            return 0;
        }
        return Data.getInt(data, fieldOffset);
    }

    /**
     * Reads a 64-bit integer field.
     *
     * @param id zero-based field slot
     * @return field value, or {@code 0} when absent
     */
    public long fetchInt64(int id) {
        int fieldOffset = calcFieldOffset(id);
        if (fieldOffset < 0) {
            return 0;
        }
        return Data.getLong(data, fieldOffset);
    }

    /**
     * Reads a 32-bit floating-point field.
     *
     * @param id zero-based field slot
     * @return field value, or {@code 0.0f} when absent
     */
    public float fetchFloat32(int id) {
        int fieldOffset = calcFieldOffset(id);
        if (fieldOffset < 0) {
            return 0;
        }
        return Data.getFloat(data, fieldOffset);
    }

    /**
     * Reads a 64-bit floating-point field.
     *
     * @param id zero-based field slot
     * @return field value, or {@code 0.0} when absent
     */
    public double fetchFloat64(int id) {
        int fieldOffset = calcFieldOffset(id);
        if (fieldOffset < 0) {
            return 0;
        }
        return Data.getDouble(data, fieldOffset);
    }

    /**
     * Reads a byte-string field.
     *
     * @param id zero-based field slot
     * @return a copy of the field bytes, or an empty array when absent
     */
    public byte[] fetchBytes(int id) {
        int fieldOffset = calcFieldOffset(id);
        if (fieldOffset < 0) {
            return emptyBytes;
        }
        return Bytes.extractBytes(data, IUnit.jump(data, fieldOffset));
    }

    /**
     * Reads a UTF-8 string field.
     *
     * @param id zero-based field slot
     * @return decoded string, or the empty string when absent
     */
    public String fetchString(int id) {
        int fieldOffset = calcFieldOffset(id);
        if (fieldOffset < 0) {
            return "";
        }
        return Bytes.extractString(data, IUnit.jump(data, fieldOffset));
    }

    /**
     * Initializes and returns {@code unit} as a view of an object, array, or map field.
     *
     * @param id zero-based field slot
     * @param unit view to initialize
     * @param <T> view type
     * @return {@code unit}; an empty view is initialized when the field is absent
     */
    public <T extends IUnit> T fetchObject(int id, T unit) {
        int fieldOffset = calcFieldOffset(id);
        if (fieldOffset < 0) {
            unit.init(null, -1);
            return unit;
        }
        return IUnit.initByField(data, calcFieldOffset(id), unit);
    }

    /**
     * Reads a boolean array field.
     *
     * @param id zero-based field slot
     * @return array view, empty when the field is absent
     */
    public BoolArray fetchBoolArray(int id) {
        return fetchObject(id, new BoolArray());
    }

    /**
     * Reads a 32-bit integer or enum array field.
     *
     * @param id zero-based field slot
     * @return array view, empty when the field is absent
     */
    public Int32Array fetchInt32Array(int id) {
        return fetchObject(id, new Int32Array());
    }

    /**
     * Reads a 64-bit integer array field.
     *
     * @param id zero-based field slot
     * @return array view, empty when the field is absent
     */
    public Int64Array fetchInt64Array(int id) {
        return fetchObject(id, new Int64Array());
    }

    /**
     * Reads a 32-bit floating-point array field.
     *
     * @param id zero-based field slot
     * @return array view, empty when the field is absent
     */
    public Float32Array fetchFloat32Array(int id) {
        return fetchObject(id, new Float32Array());
    }

    /**
     * Reads a 64-bit floating-point array field.
     *
     * @param id zero-based field slot
     * @return array view, empty when the field is absent
     */
    public Float64Array fetchFloat64Array(int id) {
        return fetchObject(id, new Float64Array());
    }

    /**
     * Reads a UTF-8 string array field.
     *
     * @param id zero-based field slot
     * @return array view, empty when the field is absent
     */
    public StringArray fetchStringArray(int id) {
        return fetchObject(id, new StringArray());
    }

    /**
     * Reads a byte-string array field.
     *
     * @param id zero-based field slot
     * @return array view, empty when the field is absent
     */
    public BytesArray fetchBytesArray(int id) {
        return fetchObject(id, new BytesArray());
    }
}