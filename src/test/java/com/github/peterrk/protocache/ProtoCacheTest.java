// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

import com.github.peterrk.protocache.pc.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProtoCacheTest {

    @Test
    public void binaryTest() throws IOException {
        byte[] raw = ProtoCache.serialize(TestData.protobufMain());

        Main root = new Main(raw);

        Assertions.assertEquals(-999, root.getI32());
        Assertions.assertEquals(1234, root.getU32());
        Assertions.assertEquals(-9876543210L, root.getI64());
        Assertions.assertEquals(98765432123456789L, root.getU64());
        Assertions.assertTrue(root.getFlag());
        Assertions.assertEquals(Mode.MODE_C, root.getMode());
        Assertions.assertEquals("Hello World!", root.getStr());
        Assertions.assertArrayEquals(root.getData(), "abc123!?$*&()'-=@~".getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(-2.1f, root.getF32());
        Assertions.assertEquals(1.0, root.getF64());

        Small leaf = root.getObject();
        Assertions.assertEquals(88, leaf.getI32());
        Assertions.assertFalse(leaf.getFlag());
        Assertions.assertEquals("tmp", leaf.getStr());

        Int32Array i32v = root.getI32V();
        Assertions.assertEquals(2, i32v.size());
        Assertions.assertEquals(1, i32v.get(0));
        Assertions.assertEquals(2, i32v.get(1));

        Int64Array u64v = root.getU64V();
        Assertions.assertEquals(1, u64v.size());
        Assertions.assertEquals(12345678987654321L, u64v.get(0));

        String[] expectedStrv = new String[]{
                "abc", "apple", "banana", "orange", "pear", "grape",
                "strawberry", "cherry", "mango", "watermelon"};
        StringArray strv = root.getStrv();
        Assertions.assertEquals(expectedStrv.length, strv.size());
        for (int i = 0; i < expectedStrv.length; i++) {
            Assertions.assertEquals(expectedStrv[i], strv.get(i));
        }

        Float32Array f32v = root.getF32V();
        Assertions.assertEquals(2, f32v.size());
        Assertions.assertEquals(1.1f, f32v.get(0));
        Assertions.assertEquals(2.2f, f32v.get(1));

        double[] expectedF64v = new double[]{9.9, 8.8, 7.7, 6.6, 5.5};
        Float64Array f64v = root.getF64V();
        Assertions.assertEquals(f64v.size(), expectedF64v.length);
        for (int i = 0; i < expectedF64v.length; i++) {
            Assertions.assertEquals(f64v.get(i), expectedF64v[i]);
        }

        boolean[] expectedFlags = new boolean[]{true, true, false, true, false, false, false};
        BoolArray flags = root.getFlags();
        Assertions.assertEquals(flags.size(), expectedFlags.length);
        for (int i = 0; i < expectedFlags.length; i++) {
            Assertions.assertEquals(flags.get(i), expectedFlags[i]);
        }

        leaf = new Small();
        ObjectArray<Small> objv = root.getObjectv();
        Assertions.assertEquals(3, objv.size());
        Assertions.assertEquals(1, objv.get(0, leaf).getI32());
        Assertions.assertTrue(objv.get(1, leaf).getFlag());
        Assertions.assertEquals("good luck!", objv.get(2, leaf).getStr());

        Assertions.assertFalse(root.hasField(Main.FIELD_t_i32));

        StringDict.Int32Value map1 = root.getIndex();
        Assertions.assertEquals(6, map1.size());
        int idx = map1.find("abc-1");
        Assertions.assertTrue(idx >= 0);
        Assertions.assertEquals(1, map1.getValue(idx));
        idx = map1.find("abc-2");
        Assertions.assertTrue(idx >= 0);
        Assertions.assertEquals(2, map1.getValue(idx));
        Assertions.assertFalse(map1.find("abc-3") >= 0);
        Assertions.assertFalse(map1.find("abc-4") >= 0);

        Int32Dict.ObjectValue<Small> map2 = root.getObjects();
        for (int i = 0; i < map2.size(); i++) {
            int key = map2.getKey(i);
            Assertions.assertNotEquals(0, key);
            idx = map2.find(key);
            Assertions.assertTrue(idx >= 0);
            Assertions.assertEquals(key, map2.getValue(i, leaf).getI32());
        }

        Vec2D matrix = root.getMatrix();
        Assertions.assertEquals(3, matrix.size());
        Vec2D.Vec1D line = matrix.get(2, new Vec2D.Vec1D());
        Assertions.assertEquals(3, line.size());
        Assertions.assertEquals(9, line.get(2));

        ObjectArray<ArrMap> vector = root.getVector();
        Assertions.assertEquals(2, vector.size());
        ArrMap map3 = vector.get(0, new ArrMap());
        idx =  map3.find("lv2");
        Assertions.assertTrue(idx >= 0);
        ArrMap.Array val3 = map3.getValue(idx, new ArrMap.Array());
        Assertions.assertEquals(2, val3.size());
        Assertions.assertEquals(21, val3.get(0));
        Assertions.assertEquals(22, val3.get(1));

        ArrMap map4 = root.getArrays();
        idx =  map4.find("lv5");
        Assertions.assertTrue(idx >= 0);
        ArrMap.Array val4 = map4.getValue(idx, new ArrMap.Array());
        Assertions.assertEquals(2, val4.size());
        Assertions.assertEquals(51, val4.get(0));
        Assertions.assertEquals(52, val4.get(1));
    }

    @Test
    public void aliasTest() throws IOException {
        byte[] raw = ProtoCache.serialize(TestData.protobufMain("test-alias.json"));
        Assertions.assertEquals(68, raw.length);
        Assertions.assertEquals(0xd, Data.getInt(raw, 20));
        Assertions.assertEquals(1, Data.getInt(raw, 24));
        Assertions.assertEquals(1, Data.getInt(raw, 28));
    }

    @Test
    public void compressTest() throws IOException {
        byte[] raw = ProtoCache.serialize(TestData.protobufMain());

        byte[] compressed = Utils.compress(raw);
        Assertions.assertTrue(compressed.length != 0 && compressed.length < raw.length);
        byte[] back = Utils.decompress(compressed);
        Assertions.assertArrayEquals(raw, back);
    }
}
