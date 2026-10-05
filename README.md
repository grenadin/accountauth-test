# AccountManager auth test client

Minimal Android app that requests auth tokens through the Android framework `AccountManager`, used to test
the consent dialogs of [microG](https://github.com/microg/GmsCore) (`AskPermissionActivity` and
`AskPackageOverrideActivity`) that are shown when a token request needs user consent.

It uses only the Android framework API, with no Google libraries:

- `AccountManager.newChooseAccountIntent(...)` to choose a Google account
- `AccountManager.getAuthToken(account, scope, options, activity, callback, null)` to request a token

The result (token length, error or returned keys) is shown on screen and logged with tag `AccountAuthTest`
(`Log.i`, since some ROMs hide `Log.d` of third-party apps).

## Usage

Choose an account and press "Request token" in the app, or start a request with adb:

```
adb shell am start -n org.microg.test.accountauth/.MainActivity --es account <email> --es scope mail
```

- `scope`: auth token type, for example `mail` or `oauth2:https://www.googleapis.com/auth/userinfo.email`.
  OAuth2 scopes need an app registered with Google, so legacy services like `mail` are easier for testing.
- `override` (optional): passes `overridePackage` in the options, for example
  `--es override com.android.vending`, to request a token on behalf of another package.

To reach the permission dialog in microG, disable "Trust Google for app permissions" in the microG settings.

## Build

```
./gradlew assembleDebug
```

Requires Android SDK (set `sdk.dir` in `local.properties`) and JDK 17+. Minimum Android version is 6.0 (API 23).
