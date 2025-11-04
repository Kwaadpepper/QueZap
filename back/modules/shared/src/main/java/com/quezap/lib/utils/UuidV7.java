package com.quezap.lib.utils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
 * @implNote October 2025: Updated to use a faster random generation method.
 */
public class UuidV7 {
  private UuidV7() {}

  /**
   * Static factory to retrieve a type 7 (pseudo randomly generated) UUID.
   *
   * @return a randomly generated UUIDv7
   */
  public static UUID randomUuid() {
    final var longs = fasterRandomLongs();
    return new UUID(longs[0], longs[1]);
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

  /**
   * A faster method to generate UUIDv7 using ThreadLocalRandom.
   *
   * @see <a
   *     href="https://github.com/robsonkades/uuidv7/blob/master/src/main/java/io/github/robsonkades/uuidv7/UUIDv7.java">UUIDv7
   *     by robsonkades</a>
   */
  private static long[] fasterRandomLongs() {
    final long[] output = new long[] {0L, 0L};

    // 1) Fetch current time in ms, mask to 48 bits
    final long currentMillis = System.currentTimeMillis();
    final long ts48 = currentMillis & 0xFFFFFFFFFFFFL; // 48-bit mask

    // 2) Get 74 bits of entropy from ThreadLocalRandom: 64 + 32 bits
    final long random64 = ThreadLocalRandom.current().nextLong();
    final int random32 = ThreadLocalRandom.current().nextInt();

    // Assemble the high 64 bits:
    //   [ 48-bit timestamp ] [ 4-bit version=7 ] [ 12 high random bits ]
    long high = (ts48 << 16); // place 48 ms bits at bits 0–47 of high<<16 = bits 16–63
    final long randHigh12 = (random64 >>> 52) & 0x0FFFL; // top 12 bits of random64
    high |= randHigh12; // bits 52–63

    high &= ~0x0000_0000_0000_F000L; // clear version bits (bits 48–51)
    high |= 0x0000000000007000L; // set version (4 bits = 0b0111) at bits 48–51

    // Assemble the low 64 bits:
    //   [ 2-bit variant=10 ] [ 52 low bits of random64 ] [ 10 high bits of random32 ]
    long low = 0L; // set variant 0b10 at bits 64–65
    final long randLow52 = random64 & 0x000FFFFFFFFFFFFFL; // lower 52 bits of random64
    final int rand32High10 = (random32 >>> 22) & 0x3FF; // top 10 bits of random32
    low |= (randLow52 << 10); // place 52 bits at bits 66–117
    low |= rand32High10; // place 10 bits at bits 118–127

    low &= 0x3FFF_FFFF_FFFF_FFFFL; // clear variant bits (bits 64–65)
    low |= 0x8000_0000_0000_0000L; // set variant 0b10 at bits 64–65

    output[0] = high;
    output[1] = low;

    return output;
  }
}
