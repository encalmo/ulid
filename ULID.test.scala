package org.encalmo.models

class ULIDSpec extends munit.FunSuite {

  test("generate ULID") {
    val uids = (0 to 1000).map { _ =>
      val ulid = ULID.randomULID()
      val ulid2 = ULID.randomULIDPrefixed('T')
      println(ulid)
      assert(ULID.isValid(ulid))
      assert(ULID.isValid(ulid2))
      val timestamp2 = ulid2.timestamp
      val timestamp = ulid.timestamp
      val str = ulid.toString
      val parsed = ULID.fromString(str)
      assertEquals(ulid, parsed)
      Thread.sleep(1)
      ulid
    }
    assertEquals(uids.size, uids.distinct.size)
  }

  test("README example") {
    val ulid1: ULID = ULID.randomULID()
    val ulid2: ULID = ULID.randomULIDPrefixed('T')

    val ulid3 = ULID.fromString("01JNDAVQ10SPPTT64TKSZRQMT0")
    assert(ULID.isValid(ulid3))
  }

}
