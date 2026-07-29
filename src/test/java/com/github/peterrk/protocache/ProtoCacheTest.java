// Copyright (c) 2023, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

import com.github.peterrk.protocache.pc.*;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
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

    @Test
    public void floatingPointMapTest() throws Descriptors.DescriptorValidationException {
        DescriptorProtos.DescriptorProto.Builder schema = DescriptorProtos.DescriptorProto.newBuilder()
                .setName("FloatMaps");
        addMap(schema, "string_float", 1, "StringFloatEntry",
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING,
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT);
        addMap(schema, "string_double", 2, "StringDoubleEntry",
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING,
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE);
        addMap(schema, "int32_float", 3, "Int32FloatEntry",
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32,
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT);
        addMap(schema, "int32_double", 4, "Int32DoubleEntry",
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32,
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE);
        addMap(schema, "int64_float", 5, "Int64FloatEntry",
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64,
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT);
        addMap(schema, "int64_double", 6, "Int64DoubleEntry",
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64,
                DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE);

        Descriptors.FileDescriptor file = Descriptors.FileDescriptor.buildFrom(
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("floating-point-maps.proto")
                        .setPackage("floating")
                        .setSyntax("proto3")
                        .addMessageType(schema)
                        .build(),
                new Descriptors.FileDescriptor[0]);
        Descriptors.Descriptor descriptor = file.findMessageTypeByName("FloatMaps");
        DynamicMessage.Builder input = DynamicMessage.newBuilder(descriptor);
        putMap(input, "string_float", "alpha", 1.25f);
        putMap(input, "string_double", "beta", 2.5);
        putMap(input, "int32_float", -7, -3.75f);
        putMap(input, "int32_double", 8, 4.5);
        putMap(input, "int64_float", 9876543210L, 5.25f);
        putMap(input, "int64_double", -123456789L, -6.5);

        Message root = new Message(ProtoCache.serialize(input.build()), 0);

        StringDict.Float32Value stringFloat = root.fetchObject(0, new StringDict.Float32Value());
        Assertions.assertEquals(1.25f, stringFloat.getValue(stringFloat.find("alpha")));
        StringDict.Float64Value stringDouble = root.fetchObject(1, new StringDict.Float64Value());
        Assertions.assertEquals(2.5, stringDouble.getValue(stringDouble.find("beta")));

        Int32Dict.Float32Value int32Float = root.fetchObject(2, new Int32Dict.Float32Value());
        Assertions.assertEquals(-3.75f, int32Float.getValue(int32Float.find(-7)));
        Int32Dict.Float64Value int32Double = root.fetchObject(3, new Int32Dict.Float64Value());
        Assertions.assertEquals(4.5, int32Double.getValue(int32Double.find(8)));

        Int64Dict.Float32Value int64Float = root.fetchObject(4, new Int64Dict.Float32Value());
        Assertions.assertEquals(5.25f, int64Float.getValue(int64Float.find(9876543210L)));
        Int64Dict.Float64Value int64Double = root.fetchObject(5, new Int64Dict.Float64Value());
        Assertions.assertEquals(-6.5, int64Double.getValue(int64Double.find(-123456789L)));
    }

    private static void addMap(
            DescriptorProtos.DescriptorProto.Builder owner,
            String fieldName,
            int fieldNumber,
            String entryName,
            DescriptorProtos.FieldDescriptorProto.Type keyType,
            DescriptorProtos.FieldDescriptorProto.Type valueType) {
        owner.addNestedType(DescriptorProtos.DescriptorProto.newBuilder()
                .setName(entryName)
                .setOptions(DescriptorProtos.MessageOptions.newBuilder().setMapEntry(true))
                .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                        .setName("key")
                        .setNumber(1)
                        .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                        .setType(keyType))
                .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                        .setName("value")
                        .setNumber(2)
                        .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                        .setType(valueType)));
        owner.addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName(fieldName)
                .setNumber(fieldNumber)
                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                .setTypeName(".floating.FloatMaps." + entryName));
    }

    private static void putMap(DynamicMessage.Builder owner, String fieldName, Object key, Object value) {
        Descriptors.FieldDescriptor field = owner.getDescriptorForType().findFieldByName(fieldName);
        Descriptors.Descriptor entryType = field.getMessageType();
        DynamicMessage entry = DynamicMessage.newBuilder(entryType)
                .setField(entryType.findFieldByName("key"), key)
                .setField(entryType.findFieldByName("value"), value)
                .build();
        owner.addRepeatedField(field, entry);
    }
}
