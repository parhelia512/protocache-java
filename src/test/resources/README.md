# Test resource generation

## Generated Java access classes

`test.proto` is the canonical schema for both generated test packages. The
Protobuf classes use its checked-in `java_package`. To regenerate the
ProtoCache access classes, make a temporary copy, change `java_package` from
`com.github.peterrk.protocache.pb` to `com.github.peterrk.protocache.pc`, and
run `protoc` with `--pcjv_out`. Do not check in a derived `test-pc.proto`.

## FlatBuffers fixture

The FlatBuffers binary is generated from the checked-in schema and JSON:

```sh
flatc --binary -o . src/test/resources/test.fbs src/test/resources/test-fb.json
mv test-fb.bin test.fb
```

`test.fb` is a local generated artifact and is not committed. Its fixture test
is skipped with the generation instructions when the file is absent.
