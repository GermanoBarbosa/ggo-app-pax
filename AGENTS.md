# ggo-app-pax — AGENTS.md

## Build & Run

- Requires Android Studio; build via Gradle wrapper: `.\gradlew assemble<Paxuniao|Nacionalpax|Unipax|Jardim|Jardim2|Jardimtimon><Debug|Release>`
- APK output: `app_<flavor>_<versionName>.apk`
- Release builds use **debug signing config** — replace before production deploy
- Gradle 8.13, AGP 8.13.2, Kotlin 2.2.0, Compile/Target SDK 36, Min SDK 24

## Architecture (Hybrid WebView + Compose)

- **Entry point**: `MainActivity.kt` — Jetpack Compose shell hosting a single full-screen WebView
- **UI is entirely HTML-based**; Compose only provides the WebView container and exit dialog
- HTML pages live in `app/src/main/assets/` (loaded via `file:///android_asset/`)
- JS→Android bridge: `WebAppInterface` class registered as `Android` — JS calls `Android.methodName()`
- Android→JS: `webView.evaluateJavascript("jsFunc(args)", null)` — must run on UI thread via `webView.post {}`

## 6 Product Flavors

| Flavor | App Name | applicationIdSuffix |
|---|---|---|
| `paxuniao` (default) | PAX União | _(none)_ |
| `nacionalpax` | Nacional PAX | `.nacionalpax` |
| `unipax` | Unipax | `.unipax` |
| `jardim` | Jardim da Ressurreição | `.jardim` |
| `jardim2` | Jardim 2 | `.jardim.ii` |
| `jardimtimon` | Jardim Timon | `.jardim.timon` |

- `BuildConfig.FLAVOR` sent in every API request
- Plan names differ by flavor: `unipax` uses gem names (Safira/Rubi/Esmeralda/Diamante); all others use Simples/Luxo/Super Luxo
- Flavor dirs (e.g. `app/src/unipax/assets/`) override brand assets (logos, `styles.css`, `convenios.html`, `informacoes.html`)

## Key Source Files

| File | Role |
|---|---|
| `MainActivity.kt` | Entry point, WebView setup, SMS receiver, JS bridge (`WebAppInterface`) |
| `ApiClient.java` | OkHttp client, all POST/JSON, base URL from `Parametros.BASE_URL` |
| `Dados.java` | SQLite (`dados.db` + `log.db`) with manual schema migration |
| `Parametros.java` | API credentials (`appKey`, `authorization`, `BASE_URL`), timeout |
| `AppSignatureHelper.kt` | SMS Retriever API — computes 11-char app hash for SMS verification |

## API

- Base: `https://api.ggo-interno.com.br/`
- All calls: **POST** with `Content-Type: application/json`
- First call always `gerarToken()` → gets `access_token`, then passes it to all subsequent calls

## Database

- Two SQLite DBs: `dados.db` (app data) and `log.db` (logs/errors)
- Schema versioning: `TBSYS` table with `S_KEY='ver'` and sequential step migrations
- Tables: `TB_LOGIN`, `TB_CLI`, `TB_DEPENDENTES`, `TB_CX`, `TB_CONVENIADOS`
- Data fully synced from server on each login; local cache is ephemeral

## Tests

- Unit tests (JUnit 4) in `app/src/test/java/`
- Instrumentation tests (AndroidX Test + Espresso + Compose UI Test) in `app/src/androidTest/java/`
- No test runner command shortcuts defined

## Notable Quirks

- All API calls pass `flavor` as a JSON field (from `BuildConfig.FLAVOR`)
- `sincronizarDadosDoServidor` runs a chain of 4+ sequential API calls per login
- SMS Retriever API auto-fills 6-digit codes via `javascript:preencherCodigoSMS('...')`
- Keystores (`chave.jks`, `chave2.jks`) and `pass.txt` at repo root
- No CI/CD, no lint/format config found
