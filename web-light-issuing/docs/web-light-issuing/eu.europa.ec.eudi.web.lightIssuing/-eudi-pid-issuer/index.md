//[web-light-issuing](../../../index.md)/[eu.europa.ec.eudi.web.lightIssuing](../index.md)/[EudiPidIssuer](index.md)

# EudiPidIssuer

[androidJvm]\
object [EudiPidIssuer](index.md)

Eudi PID issuer singleton object

## Types

| Name | Summary |
|---|---|
| [Country](-country/index.md) | [androidJvm]<br>enum [Country](-country/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[EudiPidIssuer.Country](-country/index.md)&gt; <br>Country |
| [Result](-result/index.md) | [androidJvm]<br>interface [Result](-result/index.md)<br>Result |

## Functions

| Name | Summary |
|---|---|
| [issueDocument](issue-document.md) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [issueDocument](issue-document.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html), country: [EudiPidIssuer.Country](-country/index.md) = Country.FC, certificates: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, onResult: ([EudiPidIssuer.Result](-result/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html))<br>Issues the document. Issues the EU PID document for the given country and certificates. |
