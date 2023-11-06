//[web-light-issuing](../../../../index.md)/[eu.europa.ec.eudi.web.lightIssuing](../../index.md)/[EudiPidIssuer](../index.md)/[Result](index.md)

# Result

interface [Result](index.md)

Result

#### Inheritors

| |
|---|
| [Success](-success/index.md) |
| [Failure](-failure/index.md) |

## Types

| Name | Summary |
|---|---|
| [Failure](-failure/index.md) | [androidJvm]<br>data class [Failure](-failure/index.md)(val throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [EudiPidIssuer.Result](index.md)<br>Failure issuance result. Contains the throwable. |
| [Success](-success/index.md) | [androidJvm]<br>class [Success](-success/index.md)(val documentBytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)) : [EudiPidIssuer.Result](index.md)<br>Success issuance result. Contains the document bytes. |
