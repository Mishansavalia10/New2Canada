# New2Canada — International Student Assistance Search Engine

> **COMP 8547 — Advanced Computing Concepts** · Final Project

A Java backend + localhost website that helps new international students arriving in Canada compare **apartment rentals** across ten major Canadian rental sites — Kijiji, Craigslist, Zumper, PadMapper, ViewIt, 4Rent, RentSeeker, Realtor.ca, Liv.rent, and Rentola.

The project demonstrates every data structure and algorithm required by the course — Trie, HashMap, Priority Queue, Queue, LinkedList, Merge Sort, Edit Distance (Wagner-Fischer), Inverted Index, Regular Expressions, and a PageRank-style ranking pass — wrapped in a cloud-backed web app with Google Sign-In and Firestore persistence.

---

## What you get out of the box

| Feature | Tech |
|---|---|
| Live web crawler | Jsoup + a polite per-host throttler |
| HTML parser | Jsoup CSS-selector extraction |
| Spell checker | Levenshtein DP against a dictionary HashSet |
| Autocomplete | Trie + bounded min-heap (top-k by frequency) |
| Inverted index search | `HashMap<term, postings>` with AND-merge |
| Page ranking | TF·IDF + 2-pass PageRank-style smoothing |
| Result sorting | Hand-rolled merge sort |
| Regex validation | Postal code · email · phone · SIN |
| Pattern extraction | Prices · dates · phone numbers from free text |
| Auth | Google Sign-In via Firebase Authentication |
| Persistence | Google Cloud Firestore (cached listings + per-user history) |
| Web UI | Single-page localhost site (vanilla HTML/CSS/JS) |

---

## Quick start

### 1. Prerequisites

- **JDK 17+** (check with `java -version`)
- **Apache Maven 3.8+** (check with `mvn -version`)
- **VS Code** with the Java Extension Pack (recommended)
- **Chrome / any modern browser** for the web UI

### 2. Get the code

Clone or unzip the project into a folder, then open it in VS Code. Accept the prompt to install the Java Extension Pack if you haven't already.

### 3. Build and run

```bash
mvn clean package
java -jar target/new2canada.jar
```

You should see something like:

```
=== New2Canada Search Engine ===
DEMO mode — no serviceAccountKey.json found.
[crawler] starting refresh…
Server started on http://localhost:8080
```

Open **http://localhost:8080** in Chrome.

> **Note** — on first launch the page will say "no matches yet" for a few seconds while the live crawler hits the ten rental sites and builds the index. Refresh after \~15 seconds.

---

## Folder map

```
New2Canada/
├── pom.xml                            Maven build
├── README.md                          ← you are here
├── REPORT.md                          full project report
├── PRESENTATION.md                    slide-by-slide outline
├── VIVA.md                            likely viva questions + answers
├── .vscode/                           VS Code launch config
├── serviceAccountKey.json             [you add this; .gitignored]
└── src/main/
    ├── java/com/new2canada/
    │   ├── Main.java                  entry point
    │   ├── config/                    AppConfig · RunMode
    │   ├── crawler/                   WebCrawler + HousingCrawler + PoliteFetcher + Scheduler
    │   ├── parser/                    HTMLParser · DataExtractor + rentals/ extractors (Jsoup)
    │   ├── indexing/                  InvertedIndex · WordFrequencyCounter
    │   ├── ranking/                   PageRanker · ResultSorter (merge sort)
    │   ├── spellcheck/                SpellChecker · EditDistance (Wagner-Fischer)
    │   ├── autocomplete/              Trie · AutocompleteSystem (Trie + PQ)
    │   ├── regex/                     RegexValidator · PatternFinder
    │   ├── search/                    SearchEngine · SearchTracker
    │   ├── auth/                      User · FirebaseAuthVerifier · AuthMiddleware
    │   ├── database/                  FirestoreClient + repositories
    │   ├── models/                    Apartment · SearchResult
    │   ├── server/                    WebServer · ApiHandler · StaticFileHandler · JsonWriter
    │   └── utils/                     ResourceLoader · TextNormalizer
    └── resources/
        ├── dictionary.txt
        └── static/                    index.html + login.html + css + js
```

---

## REST API

| Method | Path | Auth | Returns |
|---|---|---|---|
| GET  | `/api/search?type=apartments&q=…&city=…&province=…` | open | Ranked JSON results |
| GET  | `/api/autocomplete?q=…&limit=10` | open | Top-k completions |
| GET  | `/api/spellcheck?word=…` | open | `{ corrected, suggestions[] }` |
| POST | `/api/validate` (body: `type`, `value`) | open | `{ valid, message }` |
| GET  | `/api/pagerank?type=…` | open | Top-ranked items overall |
| GET  | `/api/history` | **required** | Caller's frequency map + recent searches |
| GET  | `/api/me` | **required** | Current user profile |
| POST | `/api/refresh-crawl` | **required** | Trigger an on-demand re-crawl |
| GET  | `/api/debug` | open | Index sizes, Trie size, dictionary size |

Auth is via `Authorization: Bearer <Firebase-ID-token>`. In DEMO mode the middleware short-circuits every caller to an anonymous user.

---

## Troubleshooting

- **`java -jar` fails with "no main manifest attribute"** — make sure you ran `mvn clean package` first; the shade plugin builds the runnable JAR.
- **Browser shows "no matches yet"** — the live crawler hasn't finished its first pass. Wait 10–20s then refresh.
- **Crawler logs `403` or `429`** — the target site is rate-limiting us. The polite fetcher already throttles per host; you may need to wait or run during off-peak hours. The Firestore cache (FULL mode) lets the UI keep serving stale data in the meantime.
- **`FirebaseApp with name [DEFAULT] doesn't exist`** — your `serviceAccountKey.json` is missing, malformed, or pointing at a wrong project. Re-download from Firebase Console.
- **"Sign in" button does nothing** — you haven't pasted your `firebaseConfig` snippet into `src/main/resources/static/js/auth.js` yet. The page log will say so.

---

## Screenshots (placeholders)

> Add screenshots under `docs/screens/` and reference them here:
> - `docs/screens/01-landing.png`
> - `docs/screens/02-apartments-search.png`
> - `docs/screens/03-did-you-mean.png`
> - `docs/screens/04-autocomplete.png`
> - `docs/screens/05-validate.png`
> - `docs/screens/06-history-firestore.png`

---

---

## Testing

```bash
./run.sh test        # macOS / Linux   (Windows: run.bat test)
mvn test             # any platform
```

The suite (JUnit 5) covers the data-structure code (inverted index, trie, autocomplete,
edit distance, spell checker, merge sort), the regex validators and price parser, the
robots.txt rules and fetcher, and the crawl cooldown gate. Tests that need the internet
use a local HTTP server instead.

## Running

| Platform | Command |
|---|---|
| macOS / Linux | `./run.sh` |
| Windows | `run.bat` |

Both scripts check for JDK 17+ and Maven and print a clear message if either is missing.
Set `JAVA_HOME` if you have several JDKs installed.

### Configuration (environment variables, all optional)

| Variable | Effect |
|---|---|
| `NEW2CANADA_CONTACT` | Contact (email or URL) placed in the crawler's User-Agent so site owners can reach us. Set this before live crawling. |
| `NEW2CANADA_CRAWL=off` | Turns off live scraping and uses cached data only. Useful for demos and development. |

## Security and privacy changes

- **Input limits:** search text, filters, spell-check words and validation values are capped in length. Form bodies are capped at 4 KB.
- **No internal details in errors:** server errors return a generic message; the exception is logged on the server only.
- **No wildcard CORS:** the API is same-origin, so cross-origin browser access is no longer allowed. Responses set `nosniff` and `no-store`.
- **Static files:** any path containing `..` is refused, instead of rewriting it.
- **DEMO mode history:** the shared visitor history is no longer returned, so one visitor cannot see another's searches.
- **Crawl abuse:** manual refreshes are limited to one run at a time with a 10-minute cooldown (`429` / `409` responses).
- **Firestore rules:** `firestore.rules` denies all direct client access. The server uses the Admin SDK, which is not affected by these rules.
- **Scraping politeness:** every page is checked against the site's `robots.txt` first. A site whose robots.txt returns a server error is skipped for an hour.

## Terms and privacy

`/terms.html` and `/privacy.html` are **drafts**. They describe what the code actually does, but they are not legal advice and have not been reviewed. Items in square brackets (contact, retention period, governing law) must be decided before publishing.

## Design and assets

Fonts and icons are self-hosted in `static/fonts/`, and the stylesheet is prebuilt in `static/css/tailwind.css`, so the pages no longer depend on a CDN.
To rebuild the stylesheet after changing colours or classes:

```bash
cd ui-build && npm install && npm run build
```

## Known limits

- Live scraping depends on each rental site's HTML. A site redesign breaks its extractor. There are no fixture tests for the extractors yet, so a breakage shows up only when a crawl runs.
- Search history cannot yet be deleted by the user. It is a planned feature.
- Results are held in memory and rebuilt from the Firestore cache at startup.
