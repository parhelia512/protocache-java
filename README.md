# ProtoCache Java

Alternative flat binary format for [Protobuf schema](https://protobuf.dev/programming-guides/proto3/). It works like FlatBuffers, but it's usually smaller and supports maps. Flat means no deserialization overhead. [A benchmark](src/test/java/com/github/peterrk/protocache/AccessBenchmark.java) shows that Protobuf has considerable deserialization overhead and significant reflection overhead. FlatBuffers is fast but wastes space. ProtoCache strikes a balance between data size and read speed, so it's useful in data caching.

## Requirements and build

ProtoCache Java requires Java 11 or newer. Build and run the test suite with:

```sh
mvn verify
```

CI runs the same build on Java 11, 17, and 21. To install the current artifact
in your local Maven repository, run `mvn install`, then use these coordinates:

```xml
<dependency>
    <groupId>com.github.peterrk</groupId>
    <artifactId>protocache</artifactId>
    <version>0.1</version>
</dependency>
```

The POM contains the project, license, developer, SCM, and issue-tracker
metadata needed for publication. Deployment repository and signing credentials
are release-environment concerns and are not stored in this repository.

|  | Protobuf | ProtoCache | FlatBuffers | Fory | Fory-Java |
|:-------|----:|----:|----:|----:|----:|
| Data Size | 574B | 780B | 1296B | 655B | 500B |
| Compressed Size | 566B | 571B | 856B | 651B | 476B |
| Decode + Traverse | 2624ns | 819ns | 1280ns | 1796ns | 1310ns |
| Decompress | 411ns | 626ns | 1323ns | 427ns | 412ns |

Protobuf and ProtoCache benchmark inputs are generated from the JSON test
resource. The FlatBuffers binary is intentionally not stored in the repository;
generate it from the repository root when needed:

```sh
flatc --binary -o . src/test/resources/test.fbs src/test/resources/test-fb.json
mv test-fb.bin test.fb
```

The FlatBuffers fixture test is skipped with this instruction when `test.fb`
is absent.

The Fory data size in this Java benchmark is produced by the Java runtime from
`foryc`-generated Java classes. The C++ benchmark generated from the same FDL
writes `test.fr` as 615B, while Java currently writes 655B for the same object.
The schemas are readable across runtimes, but the serialized bytes are not size
identical yet.

Without zero-copy techniques, the Java version is slow. [Fory](https://fory.apache.org) claims better performance than Protobuf and FlatBuffers, and our benchmark shows that's true.

See details in the [C++ version](https://github.com/peterrk/protocache).

## Code Gen

```sh
protoc --pcjv_out=. test.proto
```

The external [`protoc-gen-pcjv`](https://github.com/peterrk/protocache/blob/main/tools/protoc-gen-pcjv.cc)
plugin generates the Java access package. It is maintained in the C++
ProtoCache repository and is intentionally not built or verified by this Maven
project. The generated files are short and human friendly.

## Basic APIs

```java
pb.Main pb = pb.Main.parseFrom(raw);
raw = ProtoCache.serialize(pb);

pc.Main root = new pc.Main(raw);
```

Serializing a protobuf message with `ProtoCache.serialize` is the only way to create a ProtoCache binary at present. The data can be accessed by wrapping it with generated code.

## Reflection

Runtime reflection for reading ProtoCache data is not on the roadmap. Use the
schema-generated access classes instead.
