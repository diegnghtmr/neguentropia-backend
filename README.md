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

`GET /api/health` reports the health of the process, not the availability of the data. Without a configured and validated manifest, the contractual response is HTTP 200 with:

```json
{"status":"up","corpusState":"unavailable"}
```

The HTTP contract is defined in an OpenAPI specification maintained outside this repository. The catalog `GET /api/papers`, the detail `GET /api/papers/{id}` and the ranking `POST /api/ranking` are implemented. The ranking receives an empty JSON object, sorts the same frequency list of the 20 abstracts with Counting, Radix and Bucket, checks that the three complete lists match and returns the top 15 and the time of each method. The explanations are in [docs/seguimiento-ranking-terminos.docx](docs/seguimiento-ranking-terminos.docx). Similarity, clustering, search and models are not implemented: their capabilities declare `available: false`, `blockedReason: NOT_IMPLEMENTED`. No external providers are enabled and the corpus is not downloaded.

## External corpus and local preparation

| Setting | Behavior |
| --- | --- |
| `CORPUS_MANIFEST` | Internal path of the single private manifest file; empty by default. Mount it externally read-only; never copy it into the repository, resources or image. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of explicit local origins, for example `http://127.0.0.1:4174`. Empty by default; wildcards, credentials, paths and external hosts are not accepted. An invalid entry closes the whole list. |

The local runtime must use the owner's UID/GID if the manifest keeps `0600` permissions; do not widen permissions to make it readable. The manifest's `sourceFile` paths are provenance references: **they are not opened, resolved or interpreted as input paths**. The application does not read any PDF either, and corpus paths are not accepted over HTTP.

Loading happens once at startup. It requires the private format `schemaVersion: 1`, review `agent-reviewed`, `humanApproved: false` and exactly twenty complete records in the order `paper-01`…`paper-20`. It checks fields/types, well-formed Unicode, abstract usefulness, uniqueness of sources/texts/IDs, the SHA-256 of the UTF-8 original and the `abstracts-sha256-` version computed over the ordered JSON tuples `[id,title,authors,originalAbstract,sourceSha256]`. The source hash is validated structurally and for uniqueness; checking its bytes belongs to the private curation, not to the runtime, which does not open PDFs.

A missing, unreadable, invalid or partial configuration leaves the whole corpus unavailable: catalog and detail return HTTP 503 `CORPUS_UNAVAILABLE`, even for an unknown ID. With the corpus ready, health returns `up`/`ready`, the catalog includes twenty summaries and capabilities, the detail keeps the exact original and its hash, and an unknown ID returns HTTP 404 `PAPER_NOT_FOUND`. Responses do not include private references, review flags or loading failure details.

`TextPreparation` computes in Java the NFC `baseText` and the tokens NFC → `Locale.ROOT` lowercase → NFC; it keeps runs of Unicode letters/marks/digits only if they contain a letter. It does not remove stopwords or apply stemming, translation or lemmatization. The `unicode-nfc-root-v1` preparation, the originals and the frequencies stay in RAM: only abstracts are traversed, in manifest order, keeping the first appearance of terms through a `LinkedHashMap`. There is no TF-IDF, frequency sorting or additional analysis path.

The tests use manifests of twenty **synthetic** records in isolated temporary state; they do not prove the integration with the real data. They include fail-closed validation, Unicode preparation, HTTP, allowed/rejected/closed-by-default CORS and the health regression without a corpus.

## Ranking measurement

The [ranking report](docs/seguimiento-ranking-terminos.docx) records the
three independent JVM runs, rotated method order, 20 warmups and 100 measured
runs per method/process, exact complete-output agreement, real top 15 and timing
limitations. `RankingBenchmark` is a standalone offline entry point; it does not
start the HTTP server. Its manifest must remain externally mounted read-only.

## Repository contents

Version sources, tests, wrapper, build configuration, Dockerfile and technical documentation. Do not version credentials, caches, build outputs or private instructions/notes. `.env.example`, if added, must contain only fictitious values. The data directories `/corpus/` and `/models/` are excluded: usage and publication permissions are checked before any transfer. Ignoring a file does not remove earlier versions or replace the review of staged files.
