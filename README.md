# Neguentropía — backend

Local Java 25 and Spring Boot base. All tools, tests and builds run in Docker; Java and Gradle do not need to be installed on the host.

## Development and tests

Run from this folder. The following commands assume a Linux host:

```sh
docker build --target development -t neguentropia-backend-dev .

docker run --rm --user "$(id -u):$(id -g)" \
  --mount "type=bind,src=$PWD,dst=/workspace" \
  --workdir /workspace neguentropia-backend-dev \
  ./gradlew --no-daemon --console=plain test
```

The Gradle cache stays in `.gradle/`, excluded from Git and from the Docker context. The wrapper pins Gradle; the Spring Boot BOM manages the versions of the application and test dependencies. The first run downloads dependencies and needs network access.

## Runnable image

```sh
docker build -t neguentropia-backend .
docker run --rm --name neguentropia-backend \
  -p 127.0.0.1:8080:8080 -e PORT=8080 neguentropia-backend
```

The build runs the tests and produces the JAR inside the container. The final stage contains the JRE and the JAR, without Gradle or sources, and runs Java with the unprivileged UID/GID `10001:10001`. Both base images are pinned by digest.

The application listens on `0.0.0.0` inside the container. `PORT` changes the internal port and defaults to `8080`; the `-p` mapping must match. Local publishing is limited to the host's loopback interface.

## Contract and scope

`GET /api/health` reports the health of the process, not the availability of the data. Without a loaded corpus file, the contractual response is HTTP 200 with:

```json
{"status":"up","corpusState":"unavailable"}
```

The HTTP contract is defined in an OpenAPI specification maintained outside this repository. The catalog `GET /api/papers`, the detail `GET /api/papers/{id}` and the ranking `POST /api/ranking` are implemented. The ranking receives an empty JSON object, sorts the same frequency list of the 20 abstracts with Counting, Radix and Bucket, checks that the three complete lists match and returns the top 15 and the time of each method. The explanations are in [ranking report](docs/Ranking%20de%20T%C3%A9rminos%20Mediante%20Algoritmos%20de%20Ordenamiento%20no%20Comparativos.docx). Similarity, clustering, search and models are not implemented. No external providers are enabled and the corpus is not downloaded.

## External corpus and local preparation

| Setting | Behavior |
| --- | --- |
| `CORPUS_MANIFEST` | Internal path of the private `corpus.json` file; empty by default. Mount it externally read-only; never copy it into the repository, resources or image. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins applied as configured, for example `http://127.0.0.1:4174`. Whitespace is stripped and empty entries are ignored. Empty by default: CORS stays closed. GET and POST are allowed without credentials. |

The local runtime must use the owner's UID/GID if the file keeps `0600` permissions; do not widen permissions to make it readable. The `sourceFile` paths are provenance references: **they are not opened, resolved or interpreted as input paths**. The application does not read any PDF either, and corpus paths are not accepted over HTTP.

`corpus.json` is a plain JSON document with one `papers` array; each paper has `id`, `title`, `authors`, `originalAbstract` and `sourceFile`:

```json
{"papers":[{"id":"paper-01","title":"…","authors":["…"],"originalAbstract":"…","sourceFile":"…"}]}
```

The application reads it once at startup and maps those fields as provided, without runtime integrity validation; the team that curates the file is responsible for the content, completeness and order of the twenty records.

Missing or empty configuration, an unreadable corpus file or a loading failure leaves the corpus unavailable: catalog, detail and ranking return HTTP 503 `CORPUS_UNAVAILABLE`, even for an unknown ID, while health reports `up`/`unavailable`. An incomplete but readable file is not rejected. With the corpus ready, health returns `up`/`ready`, the catalog includes twenty summaries, the detail keeps the exact original abstract, and an unknown ID returns HTTP 404 `PAPER_NOT_FOUND`. Responses do not include private references, review flags or loading failure details.

`TextPreparation` computes in Java the NFC `baseText` and the tokens NFC → `Locale.ROOT` lowercase → NFC; it keeps runs of Unicode letters/marks/digits only if they contain a letter. It does not remove stopwords or apply stemming, translation or lemmatization. The `unicode-nfc-root-v1` preparation, the originals and the frequencies stay in RAM: only abstracts are traversed, in file order, keeping the first appearance of terms through a `LinkedHashMap`. There is no TF-IDF, frequency sorting or additional analysis path.

The behavior tests use **synthetic** data in isolated temporary state; they do not prove integration with the real corpus. Coverage includes the three sorts' worked examples, stability, permutations, unchanged inputs, cross-validation against an independent oracle, Unicode preparation, HTTP 200/400/404/503, allowed/rejected/closed-by-default CORS and health without a corpus.

## Ranking measurement

The [ranking report](docs/Ranking%20de%20T%C3%A9rminos%20Mediante%20Algoritmos%20de%20Ordenamiento%20no%20Comparativos.docx) records the
three independent JVM runs, rotated method order, 20 warmups and 100 measured
runs per method/process, exact complete-output agreement, real top 15 and timing
limitations. `RankingBenchmark` is a standalone offline entry point; it does not
start the HTTP server. Its arguments are the `corpus.json` path and the
comma-separated method order, for example `counting,radix,bucket`. It prints
`source_commit` from the JVM property `-Dbenchmark.sourceCommit` as given,
without validating it. The corpus file must remain externally mounted read-only.

## Repository contents

Version sources, tests, wrapper, build configuration, Dockerfile and technical documentation. Do not version credentials, caches, build outputs or private instructions/notes. `.env.example`, if added, must contain only fictitious values. The data directories `/corpus/` and `/models/` are excluded: usage and publication permissions are checked before any transfer. Ignoring a file does not remove earlier versions or replace the review of staged files.
