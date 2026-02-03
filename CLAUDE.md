# EUDI Wallet Development Reference

## PID Credential Issuance Setup

### Issuer Configuration

**Issuer URL:** `https://issuer.theaustraliahack.com/draft13`

**Issue Endpoint:** `POST https://issuer.theaustraliahack.com/openid4vc/mdoc/issue`
- Note: The endpoint does NOT include `/draft13` prefix

**Credential Configuration ID:** `eu.europa.ec.eudi.pid.1`

### Request JSON Template

Save to `/tmp/pid_request.json`:

```json
{
  "issuerKey": {
    "type": "jwk",
    "jwk": {
      "kty": "EC",
      "crv": "P-256",
      "x": "xFzQmEqI7re2LVrI1fb93kyJP2I2VC6MLi_VlOLv_9M",
      "y": "w7A10oopjoyX320_qnyVkpK5Q-AO1r-qvhTthjIhUIk",
      "d": "eNbw3WYOf0iNTF7wvWqwzJ9nd3RzRVByya_ULDbEO2U"
    }
  },
  "issuerDid": "did:jwk:eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6InhGelFtRXFJN3JlMkxWckkxZmI5M2t5SlAySTJWQzZNTGlfVmxPTHZfOU0iLCJ5IjoidzdBMTBvb3Bqb3lYMzIwX3FueVZrcEs1US1BTzFyLXF2aFR0aGpJaFVJayJ9",
  "credentialConfigurationId": "eu.europa.ec.eudi.pid.1",
  "mdocData": {
    "eu.europa.ec.eudi.pid.1": {
      "family_name": "DOE",
      "given_name": "JOHN",
      "birth_date": "1990-01-15",
      "age_over_18": true,
      "issuing_authority": "Test Authority",
      "issuing_country": "US"
    }
  },
  "x5Chain": ["-----BEGIN CERTIFICATE-----\nMIIBzzCCAXWgAwIBAgIUN9yppHkZd/j5jlilaBGv8qrTyR4wCgYIKoZIzj0EAwIw\nPTELMAkGA1UEBhMCVVMxFDASBgNVBAoMC1Rlc3QgSXNzdWVyMRgwFgYDVQQDDA9U\nZXN0IFBJRCBJc3N1ZXIwHhcNMjYwMjAyMDg0OTMzWhcNMjcwMjAyMDg0OTMzWjA9\nMQswCQYDVQQGEwJVUzEUMBIGA1UECgwLVGVzdCBJc3N1ZXIxGDAWBgNVBAMMD1Rl\nc3QgUElEIElzc3VlcjBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABMRc0JhKiO63\nti1ayNX2/d5MiT9iNlQujC4v1ZTi7//Tw7A10oopjoyX320/qnyVkpK5Q+AO1r+q\nvhTthjIhUImjUzBRMB0GA1UdDgQWBBQkYSHlVztY3+kCACDxbOSltm6uUDAfBgNV\nHSMEGDAWgBQkYSHlVztY3+kCACDxbOSltm6uUDAPBgNVHRMBAf8EBTADAQH/MAoG\nCCqGSM49BAMCA0gAMEUCIFaHHzuMRWRPiJk2zCbtmemDTuTmdQSTrJMiWVPmahAv\nAiEAonDzD588CXQSdUl4CPL38FeSUirnZEExJo1mopIXX+4=\n-----END CERTIFICATE-----"]
}
```

### Issue Credential Offer Command

```bash
curl -sS "https://issuer.theaustraliahack.com/openid4vc/mdoc/issue" \
  -X POST \
  -H "Content-Type: application/json" \
  -d @/tmp/pid_request.json
```

Returns: `openid-credential-offer://?credential_offer_uri=...`

### Generate QR Code

```bash
OFFER_URL='<offer-url-from-above>'
qrencode -t UTF8 "$OFFER_URL"
```

## EUDI Wallet Modifications

### 1. Trust Store Certificate

**File:** `resources-logic/src/main/res/raw/test_pid_issuer.pem`

Contains the test issuer certificate (EC P-256, valid until 2027-02-02).

### 2. Wallet Core Config

**File:** `core-logic/src/dev/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`

**Changes:**

1. Added test issuer to `vciConfig` with `ClientAuthenticationType.None`:
```kotlin
OpenId4VciManager.Config.Builder()
    .withIssuerUrl(issuerUrl = "https://issuer.theaustraliahack.com/draft13")
    .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.None(clientId = "eudi-wallet"))
    .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
    .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
    .build()
```

2. Added certificate to trust store:
```kotlin
configureReaderTrustStore(
    context,
    // ... other certs ...
    R.raw.test_pid_issuer
)
```

### Build & Install

```bash
cd /tmp/eudi-wallet
./gradlew :assembly-logic:assembleDevDebug --quiet
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

## Key Technical Notes

- **ClientAuthenticationType.None** requires a `clientId` parameter
- **ClientAuthenticationType.AttestationBased** is for production wallets with attestation providers
- **x5Chain** must be in PEM format (with BEGIN/END CERTIFICATE headers)
- The issuer metadata endpoint is at `/.well-known/openid-credential-issuer`
- Check docker logs with: `docker logs docker-compose-issuer-api-1`

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| 500 NullPointerException on x509 | x5Chain not in PEM format | Add BEGIN/END CERTIFICATE headers |
| "Client attestation required" | Wrong ClientAuthenticationType | Use `None(clientId = "...")` |
| Credential issued but not stored | Certificate not trusted | Add cert to `configureReaderTrustStore` |
| 404 on issue endpoint | Wrong URL path | Use `/openid4vc/mdoc/issue` (no /draft13) |
