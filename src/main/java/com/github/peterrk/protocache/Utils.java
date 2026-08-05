package com.github.peterrk.protocache;

import java.io.ByteArrayOutputStream;

/** Compression utilities for ProtoCache byte arrays. */
public class Utils {
    /** Creates a compression utility instance. */
    public Utils() {}

    private static final class CompressContext {
        byte[] src;
        int k;
        ByteArrayOutputStream out;
        CompressContext(byte[] src) {
            this.src = src;
            k = 0;
            out = new ByteArrayOutputStream();
            int n = src.length;
            while ((n & ~0x7f) != 0) {
                out.write(0x80 | (n & 0x7f));
                n >>>= 7;
            }
            out.write(n);
        }

        int pick() {
            int cnt = 1;
            byte ch = src[k++];
            if (ch == ((int)ch >> 1)) {
                while (k < src.length && cnt < 4 && src[k] == ch) {
                    k++;
                    cnt++;
                }
                return 0x8 | (ch & 0x4) | (cnt - 1);
            } else {
                while (k < src.length && cnt < 7 && src[k] != 0 && src[k] != (byte)0xff) {
                    k++;
                    cnt++;
                }
                return cnt;
            }
        }
    }

    /**
     * Compresses a byte array using ProtoCache run-length encoding.
     *
     * @param src bytes to compress
     * @return compressed representation; an empty input produces an empty output
     * @throws NullPointerException if {@code src} is {@code null}
     */
    public static byte[] compress(byte[] src) {
        if (src.length == 0) {
            return new byte[0];
        }
        CompressContext context = new CompressContext(src);
        while (context.k < src.length) {
            int x = context.k;
            int a = context.pick();
            if (context.k == src.length) {
                context.out.write(a);
                if ((a & 0x8) == 0) {
                    context.out.write(src, x, a);
                }
                break;
            }
            int y = context.k;
            int b = context.pick();
            context.out.write(a|(b<<4));
            if ((a & 0x8) == 0) {
                context.out.write(src, x, a);
            }
            if ((b & 0x8) == 0) {
                context.out.write(src, y, b);
            }
        }
        return context.out.toByteArray();
    }

    private static final class DecompressContext {
        byte[] src;
        byte[] out;
        int k;
        int off;
        DecompressContext(byte[] src, int k, int size) {
            this.src = src;
            this.k = k;
            out = new byte[size];
            off = 0;
        }

        boolean unpack(int mark) {
            if ((mark & 8) != 0) {
                int cnt = (mark & 3) + 1;
                if (off+cnt > out.length) {
                    return false;
                }
                byte v = 0;
                if ((mark & 4) != 0) {
                    v = (byte)0xff;
                }
                for (; cnt != 0; cnt--) {
                    out[off++] = v;
                }
            } else {
                int l = mark & 7;
                if (k+l > src.length) {
                    return false;
                }
                for (; l != 0; l--) {
                    out[off++] = src[k++];
                }
            }
            return true;
        }
    }

    private static byte[] decompress(byte[] src, int off, int size) {
        DecompressContext context = new DecompressContext(src, off, size);
        while (context.k < src.length) {
            int mark = src[context.k++] & 0xff;
            if (!context.unpack(mark&0xf) || !context.unpack(mark>>4)) {
                throw new IllegalArgumentException("broken data");
            }
        }
        if (context.off != size) {
            throw new IllegalArgumentException("size mismatch");
        }
        return context.out;
    }

    /**
     * Decompresses data produced by {@link #compress(byte[])}.
     *
     * @param src compressed bytes
     * @return decompressed bytes; an empty input produces an empty output
     * @throws NullPointerException if {@code src} is {@code null}
     * @throws IllegalArgumentException if {@code src} is malformed
     */
    public static byte[] decompress(byte[] src) {
        if (src.length == 0) {
            return new byte[0];
        }
        int k = 0;
        int size = 0;
        for (int sft = 0; sft < 32; sft += 7) {
            byte b = src[k++];
            size |= ((int) b & 0x7f) << sft;
            if ((b & 0x80) == 0) {
                return decompress(src, k, size);
            }
        }
        throw new IllegalArgumentException("broken data");
    }
}
