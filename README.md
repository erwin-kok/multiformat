# multiformat
Self-describing values for Future-proofing

[![ci](https://github.com/erwin-kok/multiformat/actions/workflows/ci.yaml/badge.svg)](https://github.com/erwin-kok/multiformat/actions/workflows/ci.yaml)
[![Maven Central](https://img.shields.io/maven-central/v/org.erwinkok.multiformat/multiformat)](https://central.sonatype.com/artifact/org.erwinkok.result/result-monad)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/github/license/erwin-kok/multiformat.svg)](https://github.com/erwin-kok/multiformat/blob/master/LICENSE)

## Introduction

This project provides **Kotlin implementations of the Multiformats protocols**.

Multiformats define self-describing data formats designed to remain interoperable across systems, languages, and decades. They are a foundational building block in systems such as IPFS and libp2p.

The goal of this project is to offer **clear, explicit, and specification-faithful implementations** of the core Multiformats standards, optimized for correctness and readability rather than convenience abstractions.

Specifications are defined at https://multiformats.io.

## Installation

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.erwinkok.multiformat:multiformat:$latest")
}
```

## Implemented Protocols

- **[multiaddr](https://github.com/multiformats/multiaddr)** — Self-describing network addresses
- **[multibase](https://github.com/multiformats/multibase)** — Base encoding descriptors
- **[multicodec](https://github.com/multiformats/multicodec)** — Self-describing serialization codes
- **[multihash](https://github.com/multiformats/multihash)** — Cryptographic hash identifiers
- **[multistream-select](https://github.com/multiformats/multistream-select)** — Protocol negotiation

Additionally, this project implements:
- **[CID](https://github.com/multiformats/cid)** - Content Identifier

## Error Handling Model

This project uses an explicit Result monad for error handling.

With few exceptions, public APIs return Result<T> instead of throwing exceptions. This makes error propagation explicit, composable, and visible in the type system.

This is a deliberate design choice.

Exceptions are implicit and non-local: once execution enters a try block, it is no longer clear which operation caused control flow to jump to a catch clause. This complicates reasoning about resource ownership and cleanup.

By contrast, Result values make success and failure part of normal control flow.

Example:
```kotlin
val connection = createConnection()
    .getOrElse {
        log.error { "Could not create connection: ${errorMessage(it)}" }
        return Err(it)
    }

connection.write(...)
```
Compared to exception-based control flow:

```kotlin
var connection: Connection? = null
try {
    ...
    connection = createConnection()
    ...
    methodThrowingException()
    ...
} catch (e: Exception) {
    if (connection != null) {
        connection.close()
    }
}
```

In the catch block, it is unclear whether the connection was successfully created or not.

Using Result makes this explicit:
```kotlin
val connection = createConnection()
    .getOrElse {
        log.error { "Could not create connection: ${errorMessage(it)}" }
        return Err(it)
    }
...
methodGeneratingError()
    .onFailure {
        connection.close()
        return
    }
...
```
The control flow and resource lifetime are explicit and locally reasoned about.

## Usage

This section provides a brief overview. For more comprehensive examples, see the test suite.

### multiaddr

```kotlin
val addr1 = Multiaddress.fromString("/ip4/127.0.0.1/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC/tcp/1234")
    .getOrElse {
        log.error { "Could not parse Multiaddress: ${errorMessage(it)}" }
        return Err(it)
    }
val ip6Addr = Multiaddress.fromString("/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095").getOrThrow()
val tcpAddr = Multiaddress.fromString("/tcp/8000").getOrThrow()
val webAddr = Multiaddress.fromString("/ws").getOrThrow()
val actual1 = Multiaddress.fromString("/").expectNoErrors()
    .encapsulate(ip6Addr).expectNoErrors()
    .encapsulate(tcpAddr).expectNoErrors()
    .encapsulate(webAddr).expectNoErrors()
    .toString()
```

### multibase

```kotlin
val multibase = Multibase.encode("base16", "foobar".toByteArray()).getOrThrow()
val bytes = Multibase.decode("f666f6f626172").getOrThrow()
```

### multicodec
```kotlin
val codec = Multicodec.nameToType("cidv2")
```

### multihash
```kotlin
val multihash = Multihash.fromBase58("QmPfjpVaf593UQJ9a5ECvdh2x17XuJYG5Yanv5UFnH3jPE")
```

### multistream-select

```kotlin
val selected = MultistreamMuxer.selectOneOf(setOf("/a", "/b", "/c"), connection)
    .getOrElse {
        log.error { "Error selecting protocol: ${errorMessage(it)}" }
        return Err(it)
    }
```

## Sub-modules

This project has three submodules:

```shell
git submodule add https://github.com/multiformats/multicodec src/main/kotlin/org/erwinkok/multiformat/spec/multicodec
git submodule add https://github.com/multiformats/multibase src/main/kotlin/org/erwinkok/multiformat/spec/multibase
git submodule add https://github.com/multiformats/multihash src/main/kotlin/org/erwinkok/multiformat/spec/multihash
```

They are used for:

- Code generation
- Conformance testing
- Verifying behavior against the specifications

## License

This project is licensed under the BSD-3-Clause license, see [`LICENSE`](LICENSE) file for more details. 
