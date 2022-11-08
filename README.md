# multiformat

[![ci](https://github.com/erwin-kok/multiformat/actions/workflows/ci.yaml/badge.svg)](https://github.com/erwin-kok/multiformat/actions/workflows/ci.yaml)
[![License](https://img.shields.io/github/license/erwin-kok/multiformat.svg)](https://github.com/erwin-kok/multiformat/blob/master/LICENSE)

## Introduction

This project contains various cryptographic utilities used by libp2p. It can generate key pairs (private/public keys), 
marshal and unmarshal these public and private keys. Further it is possible to sign and verify messages. And lastly it
supports converting these private/public to/from protocol buffer format. 

Four cryptographic key types are currently supported:
- ecdsa
- ed25519
- secp256k1
- rsa

## Using the Result Monad

This project is using the [result-monad](https://github.com/erwin-kok/result-monad)

This means that all methods of `CryptoUtil` return a `Result<...>`. The caller can check whether an error was generated, 
or it can use the value. For example:

```kotlin
val (privateKey, publicKey) = CryptoUtil.generateKeyPair(KeyType.ECDSA)
    .getOrElse {
        log.error { "Could not generate new key pair. ${errorMessage(it)}" }
        return Err(it)
    }
```

In the examples below `OnFailure` is used as a convenience, but other methods can be used as well.

If you would like to throw the Error instead, do:

```kotlin
val (privateKey, publicKey) = CryptoUtil.generateKeyPair(KeyType.ECDSA).getOrThrow()
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
