// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

/**
 * A compact perfect-hash index used by ProtoCache maps.
 * Instances can either wrap encoded index data or be built from a key source.
 */
public class PerfectHash {
    final byte[] data;
    final int offset;
    final int byteSize;
    final int size;
    private final int section;

    /**
     * Creates a view over an encoded perfect-hash index.
     *
     * @param data encoded index data
     * @throws NullPointerException if {@code data} is {@code null}
     * @throws IllegalArgumentException if the data is too short
     */
    public PerfectHash(byte[] data) {
        this(data, 0);
    }

    PerfectHash(byte[] data, int offset) {
        if (data.length - offset < 4) {
            throw new IllegalArgumentException("too short data");
        }
        this.data = data;
        this.offset = offset;
        this.size = Data.getInt(data, offset) & 0xfffffff;
        if (size < 2) {
            section = 0;
            byteSize = 4;
            return;
        }
        section = calcSectionSize(size);
        int n = calcBitmapSize(section);
        if (size > 0xffff) {
            n += n / 2;
        } else if (size > 0xff) {
            n += n / 4;
        } else if (size > 24) {
            n += n / 8;
        }
        byteSize = n + 8;
        if (data.length - offset < byteSize) {
            throw new IllegalArgumentException("too short data");
        }
    }

    private PerfectHash(byte[] data, int size, int section) {
        this.data = data;
        this.offset = 0;
        this.size = size;
        this.section = section;
        this.byteSize = data.length;
    }

    private static int calcSectionSize(int size) {
        return Math.max(10, (int) ((size * 105L + 255) / 256));
    }

    private static int calcBitmapSize(int section) {
        return ((section * 3 + 31) & ~31) / 4;
    }

    private static int countValidSlot(long v) {
        v &= (v >>> 1);
        v = (v & 0x1111111111111111L) + ((v >>> 2) & 0x1111111111111111L);
        v = v + (v >>> 4);
        v = v + (v >>> 8);
        v = (v & 0xf0f0f0f0f0f0f0fL) + ((v >>> 16) & 0xf0f0f0f0f0f0f0fL);
        v = v + (v >>> 32);
        return 32 - ((int) v & 0xff);
    }

    private static int[] hash(int seed, int section, byte[] key) {
        int[] out = new int[3];
        Hash.V128 h = Hash.hash128(key, (long) seed & 0xffffffffL);
        out[0] = Integer.remainderUnsigned((int) h.low, section);
        out[1] = Integer.remainderUnsigned((int) (h.low >>> 32), section) + section;
        out[2] = Integer.remainderUnsigned((int) h.high, section) + section * 2;
        return out;
    }

    private static int bit2(byte[] data, int offset, int pos) {
        int blk = pos >>> 2;
        int sft = (pos & 3) << 1;
        return (data[offset + blk] >>> sft) & 3;
    }

    private static void setBit2on11(byte[] data, int offset, int pos, int val) {
        int blk = pos >>> 2;
        int sft = (pos & 3) << 1;
        data[offset + blk] ^= (byte) ((~val & 3) << sft);
    }

    private static byte[] build(IKeySource src, int width) {
        int size = src.total();
        int section = calcSectionSize(size);
        int bmsz = calcBitmapSize(section);
        int bytes = 8 + bmsz;
        if (bmsz > 8) {
            bytes += (bmsz / 8) * width;
        }
        byte[] data = new byte[bytes];

        Graph graph = new Graph(size);
        int[] free = new int[size];
        BitSet book = new BitSet(section*3);

        Data.View bitmap = new Data.View(data, 8);
        int tableOffset = 8 + bmsz;
        Data.putInt(data, 0, size);

        Random rand = new Random();
        for (int chance = (width == 1) ? 40 : 16; chance >= 0; chance--) {
            int seed = rand.nextInt();
            Data.putInt(data, 4, seed);
            graph.init(seed, src);
            if (!graph.tear(free, book)) {
                continue;
            }
            graph.mapping(free, book, data, bitmap.offset);
            if (bmsz > 8) {
                int cnt = 0;
                switch (width) {
                    case 4:
                        for (int i = 0; i < bmsz / 8; i++) {
                            Data.putInt(data, tableOffset + i * 4, cnt);
                            cnt += countValidSlot(Data.getLong(data, 8 + i * 8));
                        }
                        break;
                    case 2:
                        for (int i = 0; i < bmsz / 8; i++) {
                            Data.putShort(data, tableOffset + i * 2, (short) cnt);
                            cnt += countValidSlot(Data.getLong(data, 8 + i * 8));
                        }
                        break;
                    default:
                        for (int i = 0; i < bmsz / 8; i++) {
                            data[tableOffset + i] = (byte) cnt;
                            cnt += countValidSlot(Data.getLong(data, 8 + i * 8));
                        }
                        break;
                }
                if (cnt != size) {
                    throw new RuntimeException("item lost");
                }
            }
            return data;
        }

        throw new RuntimeException("fail to build perfect-hash");
    }

    /**
     * Builds a perfect-hash index for the keys supplied by {@code src}.
     * Keys must be stable and unique across each traversal.
     *
     * @param src repeatable key source
     * @return built perfect-hash index
     * @throws NullPointerException if {@code src} is {@code null}
     * @throws IllegalArgumentException if the reported key count is outside supported limits
     */
    public static PerfectHash build(IKeySource src) {
        int size = src.total();
        if (size < 0 || size > 0xfffffff) {
            throw new IllegalArgumentException("illegal size");
        }
        byte[] data = null;
        if (size > 0xffff) {
            data = build(src, 4);
        } else if (size > 0xff) {
            data = build(src, 2);
        } else if (size > 1) {
            data = build(src, 1);
        } else {
            byte[] raw = new byte[4];
            Data.putInt(raw, 0, size);
            return new PerfectHash(raw, size, 0);
        }
        return new PerfectHash(data, size, calcSectionSize(size));
    }

    /**
     * Returns a view of the encoded index bytes.
     *
     * @return encoded-data view
     */
    public Data.View getData() {
        return new Data.View(data, offset, offset + byteSize);
    }

    /**
     * Returns the number of indexed keys.
     *
     * @return key count
     */
    public int getSize() {
        return size;
    }

    int locate(byte[] key) {
        if (size < 2) {
            return 0;
        }
        int[] slots = hash(Data.getInt(data, offset + 4), section, key);

        int bitmapOffset = offset + 8;
        int m = bit2(data, bitmapOffset, slots[0]) + bit2(data, bitmapOffset, slots[1]) + bit2(data, bitmapOffset, slots[2]);
        int slot = slots[m % 3];

        int a = slot >>> 5;
        int b = slot & 31;
        int tableOffset = bitmapOffset + calcBitmapSize(section);

        int off = 0;
        if (size > 0xffff) {
            off = Data.getInt(data, tableOffset + a * 4);
        } else if (size > 0xff) {
            off = (int) Data.getShort(data, tableOffset + a * 2) & 0xffff;
        } else if (size > 24) {
            off = (int) data[tableOffset + a] & 0xff;
        }

        long block = Data.getLong(data, bitmapOffset + a * 8);
        block |= 0xffffffffffffffffL << (b << 1);
        return off + countValidSlot(block);
    }

    /** Supplies a repeatable sequence of keys when building an index. */
    public interface IKeySource {
        /** Resets the source so the next read returns the first key. */
        void reset();
        /**
         * Returns the number of keys.
         *
         * @return key count
         */
        int total();
        /**
         * Returns the next key.
         *
         * @return key bytes
         */
        byte[] next();
    }

    private static class Vertex {
        public int slot;
        public int prev;
        public int next;
    }

    private static final class Graph {
        public final Vertex[][] edges;
        public final int[] nodes;

        public Graph(int size) {
            edges = new Vertex[size][];
            for (int i = 0; i < size; i++) {
                edges[i] = new Vertex[3];
                for (int j = 0; j < 3; j++) {
                    edges[i][j] = new Vertex();
                }
            }
            int section = calcSectionSize(size);
            nodes = new int[section * 3];
        }

        private static boolean testAndSet(BitSet book, int pos) {
            if (book.get(pos)) {
                return false;
            }
            book.set(pos);
            return true;
        }

        public void init(int seed, IKeySource src) {
            Arrays.fill(nodes, -1);
            int section = nodes.length / 3;
            int total = src.total();
            src.reset();
            for (int i = 0; i < total; i++) {
                int[] slots = hash(seed, section, src.next());
                Vertex[] edge = edges[i];
                for (int j = 0; j < 3; j++) {
                    Vertex v = edge[j];
                    v.slot = slots[j];
                    v.prev = -1;
                    v.next = nodes[v.slot];
                    nodes[v.slot] = i;
                    if (v.next != -1) {
                        edges[v.next][j].prev = i;
                    }
                }
            }
        }

        public boolean tear(int[] free, BitSet book) {
            book.clear();

            int tail = 0;
            for (int i = edges.length - 1; i >= 0; i--) {
                Vertex[] edge = edges[i];
                for (int j = 0; j < 3; j++) {
                    Vertex v = edge[j];
                    if (v.prev == -1 && v.next == -1 && testAndSet(book, i)) {
                        free[tail++] = i;
                    }
                }
            }

            for (int head = 0; head < tail; head++) {
                int curr = free[head];
                Vertex[] edge = edges[curr];
                for (int j = 0; j < 3; j++) {
                    Vertex v = edge[j];
                    int i = -1;
                    if (v.prev != -1) {
                        i = v.prev;
                        edges[i][j].next = v.next;
                    }
                    if (v.next != -1) {
                        i = v.next;
                        edges[i][j].prev = v.prev;
                    }
                    if (i == -1) {
                        continue;
                    }
                    Vertex u = edges[i][j];
                    if (u.prev == -1 && u.next == -1 && testAndSet(book, i)) {
                        free[tail++] = i;
                    }
                }
            }

            return tail == free.length;
        }


        public void mapping(int[] free, BitSet book, byte[] data, int offset) {
            book.clear();
            Arrays.fill(data, offset, offset + calcBitmapSize(nodes.length / 3), (byte) -1);

            for (int i = free.length - 1; i >= 0; i--) {
                Vertex[] edge = edges[free[i]];
                int a = edge[0].slot;
                int b = edge[1].slot;
                int c = edge[2].slot;
                if (testAndSet(book, a)) {
                    book.set(b);
                    book.set(c);
                    int sum = bit2(data, offset, b) + bit2(data, offset, c);
                    setBit2on11(data, offset, a, (6 - sum) % 3);
                } else if (testAndSet(book, b)) {
                    book.set(c);
                    int sum = bit2(data, offset, a) + bit2(data, offset, c);
                    setBit2on11(data, offset, b, (7 - sum) % 3);
                } else if (testAndSet(book, c)) {
                    int sum = bit2(data, offset, a) + bit2(data, offset, b);
                    setBit2on11(data, offset, c, (8 - sum) % 3);
                } else {
                    throw new RuntimeException("broken graph");
                }
            }
        }
    }
}
