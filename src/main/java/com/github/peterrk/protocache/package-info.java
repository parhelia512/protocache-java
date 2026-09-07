/**
 * Views and serialization utilities for the ProtoCache binary format.
 *
 * <p>Readers require valid encoded data and access classes generated from a
 * compatible schema. They do not fully validate buffers. Indexed access requires
 * {@code 0 <= idx && idx < size()}; logical bounds are not checked consistently.
 *
 * <p>Views are mutable and must be confined to one thread, including read-only
 * queries on integer maps. Initialize each view before access, and do not retain
 * its previous contents after reinitializing it. Multiple threads may wrap the
 * same backing byte array in separate views if the data is safely published and
 * remains unchanged while any view reads it.
 */
package com.github.peterrk.protocache;
