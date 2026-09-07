package com.github.peterrk.protocache;

import com.google.protobuf.DescriptorProtos.*;
import com.google.protobuf.Descriptors.*;
import com.google.protobuf.DynamicMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompatibilityTest {
    @Test
    void genericEmptyMapSupportsEveryKeyAndValueView() throws Exception {
        for (Class<?> keyType : new Class<?>[]{StringDict.class, Int32Dict.class, Int64Dict.class}) {
            for (Class<?> valueType : keyType.getDeclaredClasses()) {
                DictType view = (DictType) valueType.getConstructor().newInstance();
                // Embed the generic empty map at a nonzero offset.
                view.init(new byte[]{9, 9, 9, 9, 0, 0, 0, 0x50}, 4);
                assertEquals(0, view.size(), valueType.getName());
                if (view instanceof StringDict) {
                    assertEquals(-1, ((StringDict) view).find("absent"));
                } else if (view instanceof Int32Dict) {
                    assertEquals(-1, ((Int32Dict) view).find(Integer.MIN_VALUE));
                } else {
                    assertEquals(-1, ((Int64Dict) view).find(Long.MIN_VALUE));
                }
            }
        }
    }

    @Test
    void mapWidthsRemainValidated() {
        for (int mark : new int[]{0, 0x40000000, 0x10000000}) {
            byte[] data = new byte[4];
            Data.putInt(data, 0, mark);
            assertThrows(IllegalArgumentException.class, () -> new Int64Dict.Float64Value().init(data, 0));
        }
        // A single-entry map needs the actual scalar widths, even with enough backing bytes.
        for (int mark : new int[]{0x50000001, 0x60000001, 0x90000001}) {
            byte[] data = new byte[20];
            Data.putInt(data, 0, mark);
            assertThrows(IllegalArgumentException.class, () -> new Int64Dict.Float64Value().init(data, 0));
        }
    }

    @Test
    void mapViewCanBeReusedAcrossNonemptyEmptyAndAbsentFields() {
        byte[] one = new byte[20];
        Data.putInt(one, 0, 0xa0000001);
        Data.putLong(one, 4, Long.MIN_VALUE);
        Data.putDouble(one, 12, 2.5);
        Int64Dict.Float64Value view = new Int64Dict.Float64Value();
        for (int i = 0; i < 2; i++) {
            view.init(one, 0);
            assertEquals(1, view.size());
            assertEquals(0, view.find(Long.MIN_VALUE));
            assertEquals(2.5, view.getValue(0));
            view.init(new byte[]{0, 0, 0, 0x50}, 0);
            assertEquals(0, view.size());
            assertEquals(-1, view.find(Long.MIN_VALUE));
            view.init(null, -1);
            assertEquals(0, view.size());
            assertEquals(-1, view.find(Long.MIN_VALUE));
        }
    }

    @Test
    void deprecatedFieldsAreOmittedWithoutRenumberingLiveFields() throws Exception {
        DescriptorProto.Builder type = DescriptorProto.newBuilder().setName("Row");
        for (int i = 1; i <= 20; i++) {
            type.addField(field("f" + i, i, i != 3 && i != 20, i == 2));
        }
        Descriptor descriptor = descriptor(type);
        DynamicMessage.Builder input = DynamicMessage.newBuilder(descriptor);
        for (FieldDescriptor field : descriptor.getFields()) {
            if (field.isRepeated()) input.addRepeatedField(field, 123);
            else input.setField(field, field.getNumber());
        }
        byte[] raw = ProtoCache.serialize(input.build());
        DynamicMessage onlyLive = DynamicMessage.newBuilder(descriptor)
                .setField(descriptor.findFieldByNumber(3), 3)
                .setField(descriptor.findFieldByNumber(20), 20).build();
        assertArrayEquals(ProtoCache.serialize(onlyLive), raw);
        Message root = new Message(raw, 0);
        for (int i = 1; i <= 20; i++) {
            assertEquals(i == 3 || i == 20, root.hasField(i - 1), "field " + i);
        }
        assertEquals(3, root.fetchInt32(2));
        assertEquals(20, root.fetchInt32(19));
    }

    @Test
    void allDeprecatedFieldsProduceAnEmptyMessageIncludingAliasNames() throws Exception {
        for (String name : new String[]{"retired", "_", "_x_"}) {
            Descriptor descriptor = descriptor(DescriptorProto.newBuilder().setName("Row")
                    .addField(field(name, 1, true, true)));
            DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                    .addRepeatedField(descriptor.getFields().get(0), 123).build();
            assertArrayEquals(new byte[4], ProtoCache.serialize(input));
        }
    }

    @Test
    void deprecatedDeclarationsStillCountTowardSchemaLimits() throws Exception {
        for (int number : new int[]{100, 6388}) {
            Descriptor descriptor = descriptor(DescriptorProto.newBuilder().setName("Row")
                    .addField(field("live", 1, false, false))
                    .addField(field("retired", number, true, false)));
            assertThrows(IllegalArgumentException.class,
                    () -> ProtoCache.serialize(DynamicMessage.getDefaultInstance(descriptor)));
        }
    }

    private static FieldDescriptorProto field(String name, int number, boolean deprecated, boolean repeated) {
        return FieldDescriptorProto.newBuilder().setName(name).setNumber(number)
                .setType(FieldDescriptorProto.Type.TYPE_INT32)
                .setLabel(repeated ? FieldDescriptorProto.Label.LABEL_REPEATED : FieldDescriptorProto.Label.LABEL_OPTIONAL)
                .setOptions(FieldOptions.newBuilder().setDeprecated(deprecated)).build();
    }

    private static Descriptor descriptor(DescriptorProto.Builder type) throws Exception {
        return FileDescriptor.buildFrom(FileDescriptorProto.newBuilder().setName("compatibility.proto")
                .setSyntax("proto3").addMessageType(type).build(), new FileDescriptor[0])
                .getMessageTypes().get(0);
    }
}
