# multiformat
Self-describing values for Future-proofing

[![ci](https://github.com/erwin-kok/multiformat/actions/workflows/ci.yaml/badge.svg)](https://github.com/erwin-kok/multiformat/actions/workflows/ci.yaml)
[![License](https://img.shields.io/github/license/erwin-kok/multiformat.svg)](https://github.com/erwin-kok/multiformat/blob/master/LICENSE)

## Introduction

This project implements various protocols defined at: https://multiformats.io/

Notably, the following protocols are implemented:

- multihash (https://multiformats.io/multihash/)
- multiaddr (https://multiformats.io/multiaddr/)
- multibase (https://github.com/multiformats/multibase)
- multicodec (https://github.com/multiformats/multicodec)

Next to this, it also implements Cid: https://github.com/multiformats/cid


## Using the Result Monad

This project is using the [result-monad](https://github.com/erwin-kok/result-monad)

This means that (almost) all methods of this project return a `Result<...>`. The caller can check whether an error was generated, 
or it can use the value. For example:

```kotlin
val selected = MultistreamMuxer.selectOneOf(setOf("/a", "/b", "/c"), connection)
    .getOrElse {
        log.error { "Error selecting protocol: ${errorMessage(it)}" }
        return Err(it)
    }
```

In the examples below `OnFailure` is used as a convenience, but other methods can be used as well.

If you would like to throw the Error instead, do:

```kotlin
val selected = MultistreamMuxer.selectOneOf(setOf("/a", "/b", "/c"), connection).getOrThrow()
```

This will return the key pair when no error occurred, and throws an `Error` exception when an error occurred. 

## Sub-modules

This project has three sub-modules:

git submodule add https://github.com/multiformats/multicodec src/main/kotlin/org/erwinkok/multiformat/spec/multicodec

git submodule add https://github.com/multiformats/multibase src/main/kotlin/org/erwinkok/multiformat/spec/multibase

git submodule add https://github.com/multiformats/multihash src/main/kotlin/org/erwinkok/multiformat/spec/multihash

## Contributing

Bug reports and pull requests are welcome on [GitHub](https://github.com/erwin-kok/multiformat).

## Contact

If you want to contact me, please write an e-mail to: [github@erwinkok.org](mailto:github@erwinkok.org)

## License

This project is licensed under the BSD-3-Clause license, see [`LICENSE`](LICENSE) file for more details. 
