# Changelog

## 1.0.0 — Unreleased

First stable release of the Java reader and Protobuf serializer subset.
Requires Java 11 or newer; validated on Java 11, 17, and 21.

### Fixes

- Preserve nonempty boolean container aliases whose encoding fits in four bytes.
- Encode empty boolean and 64-bit numeric array aliases with their correct headers,
  including top-level aliases and elements of repeated containers.
- Accept generic empty-map encodings for all supported key/value view types while
  retaining width validation for nonempty maps.
- Omit deprecated Protobuf fields without renumbering the remaining fields or
  changing schema density and maximum-field-number checks.

### Internal cleanup and tooling

- Reuse container serialization for aliases and avoid duplicate field-offset work.
  A sole scalar field named `_` or `_x_` is explicitly rejected as an invalid
  container alias; valid aliases require a repeated field.
- Keep benchmark sources and dependencies behind `-Pbenchmark`; ordinary builds
  run functional tests without FlatBuffers/Fory/JMH.
- Consume benchmark results through JMH Blackhole or return values. Earlier
  benchmark numbers are not directly comparable with the updated harness.
- Check cross-language fixtures directly in Java/C++, without parsing diagnostic text.

### Usage and upgrades

- Views require valid data, compatible generated access classes, valid indexes,
  and one view instance per thread. Shared backing arrays must remain unchanged.
  These contracts are documented in the README and API Javadoc.
- Container aliases and maps remain supported. Map keys are strings or 32-/64-bit
  integers; boolean map keys and runtime reflection reads are outside this release.
- Data previously written with an incorrect empty-array header should be regenerated
  from its source. Boolean values omitted by the old short-alias bug cannot be
  recovered from the encoded data alone. No malformed-header migration mode is added.
- Serializing a Protobuf message now discards values in deprecated fields.
- The existing format is retained. Cross-language fixtures, including empty 64-bit
  aliases, pass against C++ commit `5ed4a80995473c78838a4accba3bc9c99a2d1529`.
  This validates the shared subset, not all C++ APIs or arbitrary historical data.
