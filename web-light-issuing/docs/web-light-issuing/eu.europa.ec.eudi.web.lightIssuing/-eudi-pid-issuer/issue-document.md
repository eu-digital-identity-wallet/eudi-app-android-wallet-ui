//[web-light-issuing](../../../index.md)/[eu.europa.ec.eudi.web.lightIssuing](../index.md)/[EudiPidIssuer](index.md)/[issueDocument](issue-document.md)

# issueDocument

[androidJvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun [issueDocument](issue-document.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html), country: [EudiPidIssuer.Country](-country/index.md) = Country.FC, certificates: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, onResult: ([EudiPidIssuer.Result](-result/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html))

Issues the document. Issues the EU PID document for the given country and certificates.

In certificates use the certificates from the wallet for the document to be issued. These certificates contain the device publicKeys that will be used during signing of the document by the issuer.

Example:

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

#### Parameters

androidJvm

| | |
|---|---|
| activity | the context activity. Is needed to launch the Browser |
| country | the country to issue the document for |
| certificates | the certificates to use for the document |
| onResult | the callback to be called when the document is issued |
