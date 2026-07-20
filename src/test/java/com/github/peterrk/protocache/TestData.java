// Copyright (c) 2026, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class TestData {
    private TestData() {}

    static byte[] readResource(String name) throws IOException {
        try (InputStream input = TestData.class.getResourceAsStream("/" + name)) {
            if (input == null) {
                throw new IOException("test resource not found: " + name);
            }
            return input.readAllBytes();
        }
    }

    static com.github.peterrk.protocache.pb.Main protobufMain() throws IOException {
        return protobufMain("test.json");
    }

    static com.github.peterrk.protocache.pb.Main protobufMain(String resource) throws IOException {
        byte[] json = readResource(resource);
        com.github.peterrk.protocache.pb.Main.Builder builder =
                com.github.peterrk.protocache.pb.Main.newBuilder();
        JsonFormat.parser().ignoringUnknownFields()
                .merge(new String(json, StandardCharsets.UTF_8), builder);
        return builder.build();
    }
}
