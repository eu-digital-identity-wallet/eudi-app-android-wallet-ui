# EUDI Wallet Reference Implementation

:heavy_exclamation_mark: **Important!** Before you proceed, please read
the [EUDI Wallet Reference Implementation project description](https://github.com/eu-digital-identity-wallet/.github-private/blob/main/profile/reference-implementation.md)

----

# EUDI Web Light Issuing library

## Overview

This library is built for testing purposes in order to issue of Personal Identity Documents (PIDs)
for specific countries for the EUDI Android Wallet. This in not an official release and
it should not be used for production applications.

Currently it facilitates the document issuance from two different countries,

- FC Foreign country (Utopia)
- PT Portugal

## Disclaimer

The released software is a initial development release version:

- The initial development release is an early endeavor reflecting the efforts of a short timeboxed
  period, and by no means can be considered as the final product.
- The initial development release may be changed substantially over time, might introduce new
  features but also may change or remove existing ones, potentially breaking compatibility with your
  existing code.
- The initial development release is limited in functional scope.
- The initial development release may contain errors or design flaws and other problems that could
  cause system or other failures and data loss.
- The initial development release has reduced security, privacy, availability, and reliability
  standards relative to future releases. This could make the software slower, less reliable, or more
  vulnerable to attacks than mature software.
- The initial development release is not yet comprehensively documented.
- Users of the software must perform sufficient engineering and additional testing in order to
  properly evaluate their application and determine whether any of the open-sourced components is
  suitable for use in that application.
- We strongly recommend to not put this version of the software into production use.
- Only the latest version of the software will be supported

## Requirements

- Android 8 (API level 26) or higher

## How to Use

Below is a quick overview of how to use the library.

For source code documentation, see in [docs](./docs/index.md) directory.

### EudiPidIssuer

In [EuDiPidIssuer](./docs/web-light-issuing/eu.europa.ec.eudi.web.lightIssuing/-eudi-pid-issuer/index.md)
class, the
[issueDocument()](./docs/web-light-issuing/eu.europa.ec.eudi.web.lightIssuing/-eudi-pid-issuer/issue-document.md)
method invokes the browser to issue a document for the given country. When the process is finished,
the result is returned in the callback and can be either a
[Success](./docs/web-light-issuing/eu.europa.ec.eudi.web.lightIssuing/-eudi-pid-issuer/-result/-success/index.md)
or a
[Failure](./docs/web-light-issuing/eu.europa.ec.eudi.web.lightIssuing/-eudi-pid-issuer/-result/-failure/index.md)
object.

```kotlin
package com.example.myapp

import androidx.appcompat.app.AppCompatActivity
import java.security.cert.X509Certificate

class MainActivity : AppCompatActivity() {

  // rest of activity code omitted for brevity

  fun issueDocument() {
    val country = EudiPidIssuer.Country.FC
    val certificates = listOf<X509Certificate>(
      // list of wallet certificates
    )
    EudiPidIssuer.issueDocument(this, country, certificates) { result ->
      when (result) {
        is EudiPidIssuer.Result.Success -> {
          val documentBytes = result.documentBytes
          // add document to wallet
        }

        is EudiPidIssuer.Result.Failure -> {
          val error = result.throwable
          // handle error
        }
      }
    }
  }
}
```

## How to contribute

We welcome contributions to this project. To ensure that the process is smooth for everyone
involved, follow the guidelines found in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

### Third-party component licenses

See [licenses.md](licenses.md) for details.

### License details

Copyright (c) 2023 European Commission

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.