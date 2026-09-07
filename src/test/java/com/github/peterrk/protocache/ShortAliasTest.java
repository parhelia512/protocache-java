package com.github.peterrk.protocache;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE;
import static org.junit.jupiter.api.Assertions.*;

class ShortAliasTest {
    static Stream<Arguments> shortAliases() {
        List<Arguments> cases = new ArrayList<>();
        for (String spelling : new String[]{"_", "_x_"}) {
            for (int size = 0; size <= 4; size++) {
                cases.add(Arguments.of(spelling, TYPE_BOOL, size, false));
                cases.add(Arguments.of(spelling, TYPE_BOOL, size, true));
                cases.add(Arguments.of(spelling, TYPE_INT32, size, false));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}, {1}, size={2}, allFalse={3}")
    @MethodSource("shortAliases")
    void shortAliasesRoundTrip(String spelling, FieldDescriptorProto.Type type,
                               int size, boolean allFalse) throws Exception {
        Descriptor holderType = schema(spelling, type);
        FieldDescriptor rowField = holderType.findFieldByName("row");
        FieldDescriptor rowsField = holderType.findFieldByName("rows");
        Descriptor rowType = rowField.getMessageType();
        DynamicMessage.Builder row = DynamicMessage.newBuilder(rowType);
        for (int i = 0; i < size; i++) {
            Object value;
            if (type == TYPE_BOOL) {
                value = !allFalse && i % 2 == 0;
            } else {
                value = i - 1;
            }
            row.addRepeatedField(rowType.getFields().get(0), value);
        }

        byte[] raw = ProtoCache.serialize(row.build());
        int expectedSize = type == TYPE_BOOL ? ((size + 4) / 4) * 4 : (size + 1) * 4;
        assertEquals(expectedSize, raw.length, "top-level alias must contain its complete encoding");
        assertEquals((size << 2) | (type == TYPE_BOOL ? 0 : 1), raw[0] & 0xff);
        IUnit top = newView(type);
        top.init(raw, 0);
        assertContents(top, type, size, allFalse);

        DynamicMessage holder = DynamicMessage.newBuilder(holderType)
                .setField(rowField, row.build())
                .addRepeatedField(rowsField, DynamicMessage.getDefaultInstance(rowType))
                .addRepeatedField(rowsField, row.build())
                .build();
        Message root = new Message(ProtoCache.serialize(holder), 0);
        if (size > 0) {
            assertTrue(root.hasField(0), "nonempty aliases must not be omitted");
        }
        assertContents(root.fetchObject(0, newView(type)), type, size, allFalse);

        ObjectArray<IUnit> rows = root.fetchObject(1, new ObjectArray<>());
        assertEquals(2, rows.size());
        IUnit reused = newView(type);
        assertContents(rows.get(0, reused), type, 0, allFalse);
        assertContents(rows.get(1, reused), type, size, allFalse);
        assertContents(rows.get(0, reused), type, 0, allFalse);
    }

    @Test
    void emptyOrdinaryMessageIsStillOmitted() {
        com.github.peterrk.protocache.pb.Main input = com.github.peterrk.protocache.pb.Main.newBuilder()
                .setObject(com.github.peterrk.protocache.pb.Small.getDefaultInstance())
                .build();
        byte[] raw = ProtoCache.serialize(input);
        assertArrayEquals(new byte[4], raw);
        assertFalse(new Message(raw, 0).hasField(com.github.peterrk.protocache.pc.Main.FIELD_object));
    }

    @Test
    void emptyNestedMapAliasesRemainReadable() throws Exception {
        for (String spelling : new String[]{"_", "_x_"}) {
            Descriptor holder = schema(spelling, TYPE_MESSAGE);
            FieldDescriptor field = holder.findFieldByName("row");
            DynamicMessage input = DynamicMessage.newBuilder(holder)
                    .setField(field, DynamicMessage.getDefaultInstance(field.getMessageType())).build();
            Message root = new Message(ProtoCache.serialize(input), 0);
            assertEquals(0, root.fetchObject(0, new Int64Dict.Float64Value()).size());
        }
    }

    static Stream<Arguments> wideAliases() {
        return Stream.of("_", "_x_").flatMap(spelling -> Stream.of(
                FieldDescriptorProto.Type.TYPE_INT64, FieldDescriptorProto.Type.TYPE_UINT64,
                FieldDescriptorProto.Type.TYPE_SINT64, FieldDescriptorProto.Type.TYPE_FIXED64,
                FieldDescriptorProto.Type.TYPE_SFIXED64, FieldDescriptorProto.Type.TYPE_DOUBLE)
                .map(type -> Arguments.of(spelling, type)));
    }

    @ParameterizedTest
    @MethodSource("wideAliases")
    void wideAliasesRoundTrip(String spelling, FieldDescriptorProto.Type type) throws Exception {
        Descriptor holder = schema(spelling, type);
        FieldDescriptor rowField = holder.findFieldByName("row");
        Descriptor rowType = rowField.getMessageType();
        DynamicMessage empty = DynamicMessage.getDefaultInstance(rowType);
        Object value;
        if (type == FieldDescriptorProto.Type.TYPE_DOUBLE) {
            value = 1.5d;
        } else {
            value = Long.MIN_VALUE;
        }
        DynamicMessage nonempty = DynamicMessage.newBuilder(rowType)
                .addRepeatedField(rowType.getFields().get(0), value).build();
        byte[] raw = ProtoCache.serialize(empty);
        assertArrayEquals(new byte[]{2, 0, 0, 0}, raw);
        IUnit view = type == FieldDescriptorProto.Type.TYPE_DOUBLE ? new Float64Array() : new Int64Array();
        view.init(raw, 0);
        assertEquals(0, ((ArrayType) view).size());
        Message root = new Message(ProtoCache.serialize(DynamicMessage.newBuilder(holder)
                .setField(rowField, empty)
                .addRepeatedField(holder.findFieldByName("rows"), empty)
                .addRepeatedField(holder.findFieldByName("rows"), nonempty).build()), 0);
        assertEquals(0, ((ArrayType) root.fetchObject(0, view)).size());
        if (type == FieldDescriptorProto.Type.TYPE_DOUBLE) {
            assertEquals(0, root.fetchFloat64Array(0).size());
        } else {
            assertEquals(0, root.fetchInt64Array(0).size());
        }
        ObjectArray<IUnit> rows = root.fetchObject(1, new ObjectArray<>());
        assertEquals(2, rows.size());
        assertEquals(0, ((ArrayType) rows.get(0, view)).size());
        assertEquals(1, ((ArrayType) rows.get(1, view)).size());
        if (view instanceof Float64Array) {
            assertEquals(1.5d, ((Float64Array) view).get(0));
        } else {
            assertEquals(Long.MIN_VALUE, ((Int64Array) view).get(0));
        }
        assertEquals(0, ((ArrayType) rows.get(0, view)).size());
    }

    @Test
    void emptyMapAliasesRoundTrip() throws Exception {
        for (String spelling : new String[]{"_", "_x_"}) {
            Descriptor holder = schema(spelling, TYPE_MESSAGE);
            DynamicMessage empty = DynamicMessage.getDefaultInstance(
                    holder.findFieldByName("row").getMessageType());
            Int64Dict.Float64Value view = new Int64Dict.Float64Value();
            byte[] raw = ProtoCache.serialize(empty);
            assertArrayEquals(new byte[]{0, 0, 0, 0x50}, raw);
            view.init(raw, 0);
            assertEquals(0, view.size());
            assertEquals(-1, view.find(Long.MIN_VALUE));
            Message root = new Message(ProtoCache.serialize(DynamicMessage.newBuilder(holder)
                    .addRepeatedField(holder.findFieldByName("rows"), empty).build()), 0);
            ObjectArray<Int64Dict.Float64Value> rows = root.fetchObject(1, new ObjectArray<>());
            assertEquals(1, rows.size());
            assertEquals(0, rows.get(0, view).size());
            assertEquals(-1, view.find(Long.MAX_VALUE));
        }
    }

    @Test
    void otherEmptyAliasesUseTheirContainerEncoding() throws Exception {
        for (String spelling : new String[]{"_", "_x_"}) {
            for (FieldDescriptorProto.Type type : new FieldDescriptorProto.Type[]{
                    FieldDescriptorProto.Type.TYPE_FLOAT, FieldDescriptorProto.Type.TYPE_STRING,
                    FieldDescriptorProto.Type.TYPE_BYTES}) {
                Descriptor row = schema(spelling, type).findFieldByName("row").getMessageType();
                byte[] raw = ProtoCache.serialize(DynamicMessage.getDefaultInstance(row));
                assertArrayEquals(new byte[]{1, 0, 0, 0}, raw);
                ArrayType view = type == FieldDescriptorProto.Type.TYPE_FLOAT ? new Float32Array()
                        : type == FieldDescriptorProto.Type.TYPE_STRING ? new StringArray() : new BytesArray();
                view.init(raw, 0);
                assertEquals(0, view.size());
            }
        }
    }

    @Test
    void scalarAliasDeclarationsAreRejected() throws Exception {
        for (String spelling : new String[]{"_", "_x_"}) {
            Descriptor row = schema(spelling, TYPE_INT32).findFieldByName("row").getMessageType();
            DescriptorProto scalar = row.toProto().toBuilder().setField(0,
                    row.getFields().get(0).toProto().toBuilder()
                            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)).build();
            Descriptor type = FileDescriptor.buildFrom(FileDescriptorProto.newBuilder()
                    .setName("scalar-alias.proto").setSyntax("proto3").addMessageType(scalar).build(),
                    new FileDescriptor[0]).getMessageTypes().get(0);
            assertThrows(IllegalArgumentException.class,
                    () -> ProtoCache.serialize(DynamicMessage.getDefaultInstance(type)));
        }
    }

    private static IUnit newView(FieldDescriptorProto.Type type) {
        return type == TYPE_BOOL ? new BoolArray() : new Int32Array();
    }

    private static void assertContents(IUnit view, FieldDescriptorProto.Type type,
                                       int size, boolean allFalse) {
        if (type == TYPE_BOOL) {
            BoolArray values = (BoolArray) view;
            assertEquals(size, values.size());
            for (int i = 0; i < size; i++) {
                assertEquals(!allFalse && i % 2 == 0, values.get(i), "element " + i);
            }
        } else {
            Int32Array values = (Int32Array) view;
            assertEquals(size, values.size());
            for (int i = 0; i < size; i++) {
                assertEquals(i - 1, values.get(i), "element " + i);
            }
        }
    }

    private static Descriptor schema(String spelling, FieldDescriptorProto.Type type) throws Exception {
        FieldDescriptorProto.Builder value = FieldDescriptorProto.newBuilder()
                .setName(spelling).setNumber(1).setType(type)
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
        DescriptorProto.Builder row = DescriptorProto.newBuilder().setName("Row");
        if (type == TYPE_MESSAGE) {
            row.addNestedType(DescriptorProto.newBuilder().setName("Entry")
                    .setOptions(com.google.protobuf.DescriptorProtos.MessageOptions.newBuilder().setMapEntry(true))
                    .addField(FieldDescriptorProto.newBuilder().setName("key").setNumber(1)
                            .setType(FieldDescriptorProto.Type.TYPE_INT64)
                            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL))
                    .addField(FieldDescriptorProto.newBuilder().setName("value").setNumber(2)
                            .setType(FieldDescriptorProto.Type.TYPE_DOUBLE)
                            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)));
            value.setTypeName(".shortalias.Row.Entry");
        }
        row.addField(value);
        DescriptorProto holder = DescriptorProto.newBuilder().setName("Holder")
                .addField(FieldDescriptorProto.newBuilder().setName("row").setNumber(1)
                        .setType(TYPE_MESSAGE).setTypeName(".shortalias.Row")
                        .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL))
                .addField(FieldDescriptorProto.newBuilder().setName("rows").setNumber(2)
                        .setType(TYPE_MESSAGE).setTypeName(".shortalias.Row")
                        .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED))
                .build();
        return FileDescriptor.buildFrom(FileDescriptorProto.newBuilder()
                        .setName("short-alias.proto").setPackage("shortalias").setSyntax("proto3")
                        .addMessageType(row).addMessageType(holder).build(), new FileDescriptor[0])
                .findMessageTypeByName("Holder");
    }
}
