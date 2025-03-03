package org.encalmo.models

import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

opaque type ULID = String

object ULID
    extends OpaqueStringWithPattern[
      ULID,
      "[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}"
    ] {

  val MaxValue: ULID = "7ZZZZZZZZZZZZZZZZZZZZZZZZZ"
  val MinTime = 0L
  val MaxTime = (~0L) >>> (64 - 48) // Timestamp uses 48-bit range

  private lazy val generator: ULIDGenerator = {
    val random: scala.util.Random = SecureRandom.getInstanceStrong
    val randGen = { () =>
      val r = new Array[Byte](10)
      random.nextBytes(r)
      r
    }
    new ULIDGenerator(randGen)
  }

  def randomULID(): ULID = generator.newULIDString

  def randomULIDPrefixed(c: Char): ULID =
    require(
      CrockfordBase32.decode(c) != -1,
      s"Invalid prefix character '$c', must be one of 0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    )
    c +: generator.newULIDString.drop(1)

  def fromString(ulid: String): ULID = {
    require(ulid != null, "The input ULID string was null")
    require(
      ulid.length == 26,
      s"ULID must have 26 characters: ${ulid} (length: ${ulid.length})"
    )
    require(
      CrockfordBase32.isValidBase32(ulid),
      s"Invalid Base32 character is found in ${ulid}"
    )
    ulid
  }

  /** Create an ULID from a given timestamp (48-bit) and a random value (80-bit)
    * @param unixTimeMillis
    *   48-bit unix time millis
    * @param randHi
    *   16-bit hi-part of 80-bit random value
    * @param randLow
    *   64-bit low-part of 80-bit random value
    * @return
    */
  def of(unixTimeMillis: Long, randHi: Long, randLow: Long): ULID = {
    if (unixTimeMillis < 0L || unixTimeMillis > MaxTime) {
      throw new IllegalArgumentException(
        f"unixtime must be between 0 to ${MaxTime}%,d: ${unixTimeMillis}%,d"
      )
    }
    val hi: Long = (unixTimeMillis << (64 - 48)) | (randHi & 0xffff)
    val low: Long = randLow
    new ULID(CrockfordBase32.encode128bits(hi, low))
  }

  /** Create a ne ULID from a byte sequence (16-bytes)
    */
  def fromBytes(bytes: Array[Byte]): ULID = fromBytes(bytes, 0)

  /** Create a ne ULID from a byte sequence (16-bytes)
    */
  def fromBytes(bytes: Array[Byte], offset: Int): ULID = {
    require(
      offset + 16 <= bytes.length,
      s"ULID needs 16 bytes. offset:${offset}, size:${bytes.length}"
    )
    var i = 0
    var hi = 0L
    while (i < 8) {
      hi <<= 8
      hi |= bytes(offset + i) & 0xffL
      i += 1
    }
    var low = 0L
    while (i < 16) {
      low <<= 8
      low |= bytes(offset + i) & 0xffL
      i += 1
    }
    new ULID(CrockfordBase32.encode128bits(hi, low))
  }

  extension (ulid: ULID) {

    /** Return 48-bit UNIX-time of this ULID in milliseconds
      */
    inline def timestamp: Long =
      CrockfordBase32.decode48bits(ulid.substring(0, 10))

    /** Return 80-bits randomness value of this ULID using a pair of (Long (16-bit), Long (64-bit))
      */
    inline def randomness: (Long, Long) = {
      val (hi, low) = CrockfordBase32.decode128bits(ulid)
      (hi & 0xffffL, low)
    }

    inline def toInstant: Instant = Instant.ofEpochMilli(timestamp)

    /** Get a 128-bit (16 byte) binary representation of this ULID.
      */
    inline def toBytes: Array[Byte] = {
      val (hi, low) = CrockfordBase32.decode128bits(ulid)
      val b = new Array[Byte](16)
      for (i <- 0 until 8) {
        b(i) = ((hi >>> (64 - (i + 1) * 8)) & 0xffL).toByte
      }
      for (i <- 0 until 8) {
        b(i + 8) = ((low >>> (64 - (i + 1) * 8)) & 0xffL).toByte
      }
      b
    }
  }

  /** check a given string is valid as ULID
    * @param ulid
    * @return
    */
  def isValid(ulid: String): Boolean = {
    ulid.length == 26 && CrockfordBase32.isValidBase32(ulid)
  }

  /** Base 32 encoding by Douglas Crockford: https://www.crockford.com/base32.html
    */
  object CrockfordBase32 {

    private val ENCODING_CHARS: Array[Char] = Array(
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P',
      'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'
    )

    private val DECODING_CHARS: Array[Byte] = Array[Byte](
      -1, -1, -1, -1, -1, -1, -1, -1, // 0
      -1, -1, -1, -1, -1, -1, -1, -1, // 8
      -1, -1, -1, -1, -1, -1, -1, -1, // 16
      -1, -1, -1, -1, -1, -1, -1, -1, // 24
      -1, -1, -1, -1, -1, -1, -1, -1, // 32
      -1, -1, -1, -1, -1, -1, -1, -1, // 40
      0, 1, 2, 3, 4, 5, 6, 7, // 48
      8, 9, -1, -1, -1, -1, -1, -1, // 56
      -1, 10, 11, 12, 13, 14, 15, 16, // 64
      17, 1, 18, 19, 1, 20, 21, 0, // 72
      22, 23, 24, 25, 26, -1, 27, 28, // 80
      29, 30, 31, -1, -1, -1, -1, -1, // 88
      -1, 10, 11, 12, 13, 14, 15, 16, // 96
      17, 1, 18, 19, 1, 20, 21, 0, // 104
      22, 23, 24, 25, 26, -1, 27, 28, // 112
      29, 30, 31 // 120
    )

    inline def decode(ch: Char): Byte = DECODING_CHARS(ch & 0x7f)
    inline def encode(i: Int): Char = ENCODING_CHARS(i & 0x1f)

    /** Decode a string representation of 128 bit value (26 characters) as a pair of (Long, Long) (128 bits)
      *
      * Note that technically 26 characters x 5 bite can represent 130-bit values. This method will discard the top 2
      * bits from the string as ULID only uses 128 bits.
      */
    inline def decode128bits(s: String): (Long, Long) = {

      /** `| hi (64-bits) | low (64-bits) |`
        */
      val len = s.length
      if (len != 26) {
        throw new IllegalArgumentException(
          s"String length must be 26: ${s} (length: ${len})"
        )
      }
      var i = 0
      var hi = 0L
      var low = 0L
      val carryMask = ~(~0L >>> 5)
      while (i < 26) {
        val v = decode(s.charAt(i))
        val carry = (low & carryMask) >>> (64 - 5)
        low <<= 5
        low |= v
        hi <<= 5
        hi |= carry
        i += 1
      }
      (hi, low)
    }

    /** Encode 128-bit values (Long, Long) with Crockford Base32
      */
    inline def encode128bits(hi: Long, low: Long): String = {
      val s = new StringBuilder(26)
      var i = 0
      var h = hi
      var l = low
      // encode from lower 5-bit
      while (i < 26) {
        s += encode((l & 0x1fL).toInt)
        val carry = (h & 0x1fL) << (64 - 5)
        l >>>= 5
        l |= carry
        h >>>= 5
        i += 1
      }
      s.reverseInPlace().toString()
    }

    /** Decode 10-character Crockford Base32 as a 48-bit unsigned value. This is used for decoding ULID timestamp
      * (48-bit value)
      */
    inline def decode48bits(s: String): Long = {
      val len = s.length
      if (len != 10) {
        throw new IllegalArgumentException(
          s"String size must be 10: ${s} (length:${len})"
        )
      }
      var l: Long = decode(s.charAt(0))
      var i = 1
      while (i < len) {
        l <<= 5
        l |= decode(s.charAt(i))
        i += 1
      }
      l
    }

    inline def isValidBase32(s: String): Boolean = {
      s.forall { decode(_) != -1 }
    }
  }

  /** ULID generator.
    * @param timeSource
    *   a function that returns the current time in milliseconds (e.g. java.lang.System.currentTimeMillis())
    * @param random
    *   a function that returns a 80-bit random values in Array[Byte] (size:10)
    */
  final class ULIDGenerator(random: () => Array[Byte]) {
    private val baseSystemTimeMillis = System.currentTimeMillis()
    private val baseNanoTime = System.nanoTime()

    private val lastValue = new AtomicReference((0L, 0L))

    private def currentTimeInMillis: Long = {
      // Avoid unexpected rollback of the system clock
      baseSystemTimeMillis + TimeUnit.NANOSECONDS.toMillis(
        System.nanoTime() - baseNanoTime
      )
    }

    /** Generate a new ULID string.
      *
      * Tips for optimizing performance:
      *
      *   1. Reduce the number of Random number generation. SecureRandom is quite slow, so within the same milliseconds,
      *      just incrementing the randomness part will provide better performance. 2. Generate random in Array[Byte]
      *      (10 bytes = 80 bits). Regular Random uses 48-bit seed, so calling Random.nextInt (32 bits) x 3 is faster,
      *      but SecureRandom has optimization for Array[Byte] generation, which is much faster than calling nextInt
      *      three times. 3. ULIDs are often used in the string value form (e.g., transaction IDs, object IDs which can
      *      be embedded to URLs, etc.). Generating ULID String from the beginning is ideal. 4. In base32
      *      encoding/decoding, use bit-shift operators as much as possible to utilize CPU registers and memory cache.
      */
    inline def newULIDString: String = {
      newULIDFromMillis(currentTimeInMillis)
    }

    def newULIDFromMillis(unixTimeMillis: Long): String = {
      if (unixTimeMillis > MaxTime) {
        throw new IllegalStateException(
          f"unixtime should be less than: ${MaxTime}%,d: ${unixTimeMillis}%,d"
        )
      }
      // Add a guard so that only a single-thread can generate ULID based on the previous value
      synchronized {
        val (hi, low) = lastValue.get()
        val lastUnixTime = (hi >>> 16) & 0xffffffffffffL
        if (lastUnixTime == unixTimeMillis) {
          // do increment
          if (low != ~0L) {
            generateFrom(hi, low + 1L)
          } else {
            var nextHi = (hi & ~(~0L << 16)) + 1
            if ((nextHi & (~0L << 16)) != 0) {
              // Random number overflow. Wait for one millisecond and retry
              Thread.sleep(1)
              newULIDString
            } else {
              nextHi |= unixTimeMillis << (64 - 48)
              generateFrom(nextHi, 0)
            }
          }
        } else {
          // No conflict at millisecond level. We can generate a new ULID safely
          generateFrom(unixTimeMillis, random())
        }
      }
    }

    private inline def generateFrom(
        unixTimeMillis: Long,
        rand: Array[Byte]
    ): String = {
      // We need a 80-bit random value here.
      require(
        rand.length == 10,
        s"random value array must have length 10, but ${rand.length}"
      )

      val hi = ((unixTimeMillis & 0xffffffffffffL) << (64 - 48)) |
        (rand(0) & 0xffL) << 8 | (rand(1) & 0xffL)
      val low: Long =
        ((rand(2) & 0xffL) << 56) |
          ((rand(3) & 0xffL) << 48) |
          ((rand(4) & 0xffL) << 40) |
          ((rand(5) & 0xffL) << 32) |
          ((rand(6) & 0xffL) << 24) |
          ((rand(7) & 0xffL) << 16) |
          ((rand(8) & 0xffL) << 8) |
          (rand(9) & 0xffL)
      generateFrom(hi, low)
    }

    private inline def generateFrom(hi: Long, low: Long): String = {
      lastValue.set((hi, low))
      CrockfordBase32.encode128bits(hi, low)
    }
  }

}
