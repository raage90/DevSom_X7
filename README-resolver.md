# Setting up the URL resolver (domain-swap system)

This is the "if my domain gets banned, swap to another one without
rebuilding the app" system we designed earlier.

## How it works
`UrlResolver.kt` checks a small list of URLs (in order) for a JSON file
like `{"domain": "https://your-backend.com/"}`. First one that responds
wins. If none respond, it falls back to the URL baked into the app at
build time (`FALLBACK_API_URL`).

## Setup (optional -- the app works fine without this using just the
## fallback URL, until you actually need to swap domains)

1. Create 2-3 **separate GitHub accounts** (different emails), one repo each
2. In each repo, add a file `config.json`:
   ```json
   { "domain": "https://your-current-backend.com/" }
   ```
3. Get the **raw** URL for each (Add file -> View raw), e.g.:
   ```
   https://raw.githubusercontent.com/account1/resolver1/main/config.json
   ```
4. Open `app/src/main/java/com/tijaabo/app/network/UrlResolver.kt` and add
   these URLs to the `resolverUrls` list:
   ```kotlin
   private val resolverUrls = listOf(
       "https://raw.githubusercontent.com/account1/resolver1/main/config.json",
       "https://raw.githubusercontent.com/account2/resolver2/main/config.json"
   )
   ```
5. Rebuild the app once with this list included.

## To swap domains later (no rebuild needed)
Just edit `config.json` in any of those repos to point to the new domain.
Every installed app picks it up automatically the next time it opens.
