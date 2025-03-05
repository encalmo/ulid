![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/ulid_3?style=for-the-badge)

# ULID

Scala 3 implementation of the ULID: `Universally Unique Lexicographically Sortable Identifier`.

Official specification page: https://github.com/ulid/spec.

Essential parts borrowed from [petitviolet/ulid4s](https://github.com/petitviolet/ulid4s).

See: 
- https://victoryosayi.medium.com/ulid-universally-unique-lexicographically-sortable-identifier-d75c253bc6a8

Provided ULID is an [opaque String type](https://docs.scala-lang.org/scala3/book/types-opaque-types.html).

Example ULID identifiers:

```
01JNDAYY54SSG0WJ0Z6HM29NYG
01JNDAYY56HV0A9563TXDX7XJ6
01JNDAYY579Q2QEMSEJ45G8FTQ
01JNDAYY584QV21KEA5MVARSHG
01JNDAYY5AH5QZ8W99KA3Y998Q
01JNDAYY5BS9MCXA4X4VF8J7MW
01JNDAYY5CRYTX9PH460M87Q4K
01JNDAYY5DX3QKSZMH2K136GQ7
01JNDAYY5FRCGCFGNZ8QT7MYZR
01JNDAYY5G0M0AGJ9EKRM1HRWZ
01JNDAYY5H0XH2FXT7WNMD2REH
01JNDAYY5K5AMY2FZ8845VDC1H
01JNDAYY5MKJA5C3FW9R7NJYCB
01JNDAYY5N9K4A8VQKFN5JR79Q
01JNDAYY5P7XAPDQPNX531V40G
```

## Dependencies

- Scala >= 3.3.5 LTS
- [encalmo/opaque-type](https://github.com/encalmo/opaque-type)


## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "ulid" % "0.9.0"

or with SCALA-CLI

    //> using dep org.encalmo::ulid:0.9.0

## Example

```scala
import org.encalmo.models.ULID

val ulid1: ULID = ULID.randomULID()
val ulid2: ULID = ULID.randomULIDPrefixed('T')

val ulid3 = ULID.fromString("01JNDAVQ10SPPTT64TKSZRQMT0")
assert(ULID.isValid(ulid3))
```
