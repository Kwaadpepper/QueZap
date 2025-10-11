package com.quezap.lib.utils;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Utility class for generating UUID version 7 (UUIDv7) identifiers.
 *
 * <p>UUIDv7 is a time-ordered UUID format, which embeds the current timestamp in the UUID, allowing
 * for lexicographically sortable identifiers. This implementation uses a combination of the current
 * timestamp and random bytes to generate a UUIDv7-compliant identifier.
 *
 * <p>The generated UUIDs conform to the UUIDv7 draft specification:
 *
 * <ul>
 *   <li>Timestamp (milliseconds since Unix epoch) is embedded in the first 6 bytes.
 *   <li>Version (7) is set in the appropriate bits.
 *   <li>Variant is set according to RFC 4122.
 *   <li>Remaining bytes are randomly generated for uniqueness.
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * UUID uuid = UUIDv7.randomUuid();
 * }</pre>
 *
 * @see <a href="https://antonz.org/uuidv7/#java">UUIDv7 in 33 languages</a>
 */
public class UuidV7 {
  private static final SecureRandom random = new SecureRandom();

  private UuidV7() {}

  /**
   * Static factory to retrieve a type 7 (pseudo randomly generated) UUID.
   *
   * @return a randomly generated UUIDv7
   */
  public static UUID randomUuid() {
    byte[] value = randomBytes();
    ByteBuffer buf = ByteBuffer.wrap(value);
    long high = buf.getLong();
    long low = buf.getLong();
    return new UUID(high, low);
  }

  /**
   * Extract the Instant corresponding to the embedded timestamp in a UUIDv7.
   *
   * @param uuid the UUID (expected version 7)
   * @return the Instant encoded in the UUIDv7 (epoch milliseconds)
   * @throws NullPointerException if uuid is null
   * @throws IllegalArgumentException if the provided UUID is not version 7
   */
  public static Instant extractInstant(UUID uuid) {
    if (uuid.version() != 7) {
      throw new IllegalArgumentException("UUID is not version 7");
    }

    // Recreate the original byte array layout used by randomBytes()
    byte[] bytes = new byte[16];
    ByteBuffer.wrap(bytes)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits());

    // timestamp was stored in bytes[0..5]; reconstruct an 8-byte big-endian long with two leading
    // zeros
    byte[] ts = new byte[8];
    ts[0] = 0;
    ts[1] = 0;
    System.arraycopy(bytes, 0, ts, 2, 6);

    long epochMillis = ByteBuffer.wrap(ts).getLong();
    return Instant.ofEpochMilli(epochMillis);
  }

  private static byte[] randomBytes() {
    // random bytes
    byte[] value = new byte[16];
    random.nextBytes(value);

    // current timestamp in ms
    ByteBuffer timestamp = ByteBuffer.allocate(Long.BYTES);
    timestamp.putLong(System.currentTimeMillis());

    // timestamp
    System.arraycopy(timestamp.array(), 2, value, 0, 6);

    // version and variant
    value[6] = (byte) ((value[6] & 0x0F) | 0x70);
    value[8] = (byte) ((value[8] & 0x3F) | 0x80);

    return value;
  }
}
