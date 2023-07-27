# multiformat
Self-describing values for Future-proofing

[![ci](https://github.com/erwin-kok/multiformat/actions/workflows/ci.yaml/badge.svg)](https://github.com/erwin-kok/multiformat/actions/workflows/ci.yaml)
[![Maven Central](https://img.shields.io/maven-central/v/org.erwinkok.multiformat/multiformat)](https://central.sonatype.com/artifact/org.erwinkok.result/result-monad)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/github/license/erwin-kok/multiformat.svg)](https://github.com/erwin-kok/multiformat/blob/master/LICENSE)

## Usage

Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.erwinkok.multiformat:multiformat:$latest")
}
```

## Introduction

This project implements various protocols defined at: https://multiformats.io/

Notably, the following protocols are implemented:

- [multiaddr](https://github.com/multiformats/multiaddr): network addresses
- [multibase](https://github.com/multiformats/multibase): base encodings
- [multicodec](https://github.com/multiformats/multicodec): serialization codes
- [multihash](https://github.com/multiformats/multihash): cryptographic hashes
- [multistream-select](https://github.com/multiformats/multistream-select): Friendly protocol multiplexing.

Next to this, it also implements Cid: https://github.com/multiformats/cid


## Using the Result Monad

This project is using the [result-monad](https://github.com/erwin-kok/result-monad)

This means that (almost) all methods of this project return a `Result<...>`. The caller can check whether an error was generated, 
or it can use the value. 

## Usage

A (very) brief description on how to use multiformats:

...but please also look at the various tests.

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

These are the official specifications repositories, which are used here for auto-generation code and or verifying the 
test results are according to spec.

## Contributing

Bug reports and pull requests are welcome on [GitHub](https://github.com/erwin-kok/multiformat).

## Contact

If you want to contact me, please write an e-mail to: [erwin-kok@gmx.com](mailto:erwin-kok@gmx.com)

## License

This project is licensed under the BSD-3-Clause license, see [`LICENSE`](LICENSE) file for more details. 
