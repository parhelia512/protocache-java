// Copyright (c) 2026, Ruan Kunliang.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.github.peterrk.protocache;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BenchmarkFixtureTest {
    @Test
    public void generatedFixturesAreAvailableFromCleanCheckout() throws IOException {
        AccessBenchmark.ProtoCacheState protocache = new AccessBenchmark.ProtoCacheState();
        protocache.setup();

        AccessBenchmark.ProtobufState protobuf = new AccessBenchmark.ProtobufState();
        protobuf.setup();
    }

    @Test
    public void flatbuffersFixtureIsValidWhenGenerated() throws IOException {
        Assumptions.assumeTrue(FlatbuffersFixture.exists(), FlatbuffersFixture.missingMessage());

        AccessBenchmark.FlatbuffersState flatbuffers = new AccessBenchmark.FlatbuffersState();
        flatbuffers.setup();
        flatbuffers.traverse(com.github.peterrk.protocache.fb.Main.getRootAsMain(
                ByteBuffer.wrap(flatbuffers.raw)));
    }
}
