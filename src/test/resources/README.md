# Test resource generation

## Generated Java access classes

`test.proto` is the canonical schema for both generated test packages. The
Protobuf classes use its checked-in `java_package`. To regenerate the
ProtoCache access classes, make a temporary copy, change `java_package` from
`com.github.peterrk.protocache.pb` to `com.github.peterrk.protocache.pc`, and
run `protoc` with `--pcjv_out`. Do not check in a derived `test-pc.proto`.

Benchmark schemas and JSON inputs live in `src/benchmark/resources`. See the
repository README for FlatBuffers fixture generation and the `benchmark` profile.
