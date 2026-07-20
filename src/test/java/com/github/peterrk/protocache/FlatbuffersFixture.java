// Copyright (c) 2026, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class FlatbuffersFixture {
    static final Path FILE = Paths.get("test.fb");
    static final String GENERATION_COMMANDS =
            "flatc --binary -o . src/test/resources/test.fbs src/test/resources/test-fb.json\n" +
            "mv test-fb.bin test.fb";

    private FlatbuffersFixture() {}

    static boolean exists() {
        return Files.isRegularFile(FILE);
    }

    static String missingMessage() {
        return "test.fb is missing; generate it from the repository root:\n" +
                GENERATION_COMMANDS;
    }

    static byte[] read() throws IOException {
        if (!exists()) {
            throw new IOException(missingMessage());
        }
        return Files.readAllBytes(FILE);
    }
}
