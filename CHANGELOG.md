# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.6.3] - 2022-22-08

### Added

- [walletconnect-requests](walletconnect-requests) module
  , [SwitchEthChain](walletconnect-requests/src/main/java/walletconnect/requests/wallet/SwitchEthChain.kt)
  , [CustomRpcMethods](walletconnect-requests/src/main/java/walletconnect/requests/CustomRpcMethods.kt)
- `SwitchEthChain` button in sample app
- Proguard rule in Readme for `walletconnect-requests` module

### Changed

- [Wallet.approveRequest](walletconnect-core/src/main/java/walletconnect/core/Wallet.kt) `result:Any` -> `result:Any?`

## [0.6.2] - 2022-16-08

### Changed

- [SessionStore#getAll()](walletconnect-core/src/main/java/walletconnect/core/session_state/SessionStore.kt)
  returns `Set` instead of `List`, like `getAllAsFlow()`

## [0.6.1] - 2022-16-08

### Added

- [Cryptography#generateSymmetricKey()](walletconnect-core/src/main/java/walletconnect/core/cryptography/Cryptography.kt)

### Changed

- Readme

## [0.6.0] - 2022-08-08

### Changed

- [DAppManager](walletconnect/src/main/java/walletconnect/DAppManager.kt)
  , [WalletManager](walletconnect/src/main/java/walletconnect/WalletManager.kt)
  , [WalletConnectCore](walletconnect/src/main/java/walletconnect/WalletConnectCore.kt) constructors takes `Socket` as
  parameter instead of `socketFactory`

### Added

- Dokka plugin for kotlin Javadoc (still may not work)

### Fixed

- `walletconnect-adapter-gson` and `walletconnect-adapter-moshi` modules artifacts bug

## [0.5.0] - 2022-08-08

### Added

- Working Android sample for dApp, wallet communication
- Serialization/Deserialization adapter using [Gson](https://github.com/google/gson)
  / [Moshi](https://github.com/square/moshi)
- Store/Restore Sessions using File/SharedPreferences
- Socket implementation using [Scarlet](https://github.com/tinder/scarlet)
- Pure kotlin modules
- Unit tests (except `:walletconnect` module)

[Unreleased]: https://github.com/jemshit/walletconnect/compare/main...develop

[0.6.3]: https://github.com/jemshit/walletconnect/compare/0.6.2..0.6.3

[0.6.2]: https://github.com/jemshit/walletconnect/compare/0.6.1..0.6.2

[0.6.1]: https://github.com/jemshit/walletconnect/compare/0.6.0..0.6.1

[0.6.0]: https://github.com/jemshit/walletconnect/compare/0.5.0..0.6.0

[0.5.0]: https://github.com/jemshit/walletconnect/releases/tag/0.5.0