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

The build runs the tests and produces the JAR inside the container. The final stage contains the JRE, the JAR and the `data/` directory, without Gradle or sources, and runs Java with the unprivileged UID/GID `10001:10001`. Both base images are pinned by digest.

The application listens on `0.0.0.0` inside the container. `PORT` changes the internal port and defaults to `8080`; the `-p` mapping must match. Local publishing is limited to the host's loopback interface.

## Data

`data/` holds everything the server needs to run:

| File | Content |
| --- | --- |
| `data/corpus.json` | The 20 papers: `id`, `title`, `authors`, `originalAbstract` and `sourceFile`. |
| `data/gemini.json` | One `gemini-embedding-2` vector per paper (3072 dimensions). |
| `data/local.json` | One `intfloat/multilingual-e5-small` vector per paper (384 dimensions). |

The image copies `data/` and sets `CORPUS_MANIFEST`, `GEMINI_EMBEDDINGS` and `LOCAL_EMBEDDINGS` to those files, so it starts with the corpus ready and both embedding methods available. Override any of the three variables to use another file. Outside the image the variables are empty by default. The server loads the files once at startup and keeps them in memory; there is no database.

## Contract and scope

`GET /api/health` reports the health of the process, not the availability of the data. Without a loaded corpus file, the contractual response is HTTP 200 with:

```json
{"status":"up","corpusState":"unavailable"}
```

The HTTP contract is defined in an OpenAPI specification maintained outside this repository. The catalog `GET /api/papers`, the detail `GET /api/papers/{id}` and the ranking `POST /api/ranking` are implemented. The ranking receives an empty JSON object, sorts the same frequency list of the 20 abstracts with Counting, Radix and Bucket, checks that the three complete lists match and returns the top 15 and the time of each method. The explanations are in [ranking report](docs/Ranking%20de%20T%C3%A9rminos%20Mediante%20Algoritmos%20de%20Ordenamiento%20no%20Comparativos.docx). `POST /api/similarity` compares 2–20 distinct papers using Levenshtein, longest common subsequence (LCS), TF-IDF cosine and Jaccard. It accepts `paperIds` and `methods`, returns every selected pair in manifest order and preserves the requested method order. TF-IDF is fitted once over all twenty abstracts. The response contains only `paperIds`, `methods` and `pairs`; each result includes its score and explanatory data. `gemini-embeddings` compares pre-generated Gemini Embedding 2 vectors using explicit Java cosine in [-1,1]; embedding results identify the `model` and `dimension`. Requests never call the provider. `local-embeddings` does the same with pre-generated vectors from `intfloat/multilingual-e5-small`. Without a valid vector file, the requested embedding method returns HTTP 503 `PROVIDER_UNAVAILABLE`. No partial results are returned. Clustering and search are not implemented, and the corpus is not downloaded.

### Gemini vector generation

`GeminiEmbeddingGenerator` is a standalone command, separate from the HTTP server and from the server jar: it lives in the `generator` source set. It produced `data/gemini.json` and is only needed to regenerate it. It reads `GEMINI_API_KEY` only from the environment, never from a file. Each complete NFC abstract is prefixed with `task: sentence similarity | query: `, counted with the model's `countTokens` endpoint (maximum 8192), then embedded with `gemini-embedding-2` (3072 dimensions). It stops on errors without retries; it never truncates or substitutes a provider.

The vector file is `{"model": "gemini-embedding-2", "papers": [{"id": "paper-01", "values": [...]}, ...]}`, one record per corpus paper in corpus order; the dimension is the length of `values`. Run from this directory:

```sh
GEMINI_API_KEY=... ./gradlew generateGeminiEmbeddings --args="data/corpus.json data/gemini.json"
```

The server trusts the file's content (one record per corpus paper, in corpus order) as it does with the corpus file. An unreadable file leaves Gemini unavailable without blocking classical methods. Regenerate the vectors whenever the corpus or embedding configuration changes. The API key is not needed by the HTTP server.

### Local vector generation

`LocalEmbeddingGenerator` is a standalone command in the `generator` source set, separate from the HTTP server and the server jar (ONNX Runtime and the tokenizer library are generator-only dependencies). It uses `intfloat/multilingual-e5-small` at revision `614241f622f53c4eeff9890bdc4f31cfecc418b3` (MIT license, 384 dimensions, 512 token limit), run with ONNX Runtime and the model's own tokenizer. Each complete NFC abstract is prefixed with `query: `, tokenized without truncation (more than 512 tokens including special tokens stops the command), then token states are mean-pooled over the attention mask and L2-normalized. It produced `data/local.json` and needs no network once the model files are available: `model.onnx` and `tokenizer.json` from the model repository (not included in this repository). The output uses the same file format as above, with `"model": "intfloat/multilingual-e5-small"`.

```sh
./gradlew generateLocalEmbeddings --args="data/corpus.json /path/to/model.onnx /path/to/tokenizer.json data/local.json"
```

The generator guarantees inputs of at most 512 tokens. Requests never run the model.

## Configuration and text preparation

| Setting | Behavior |
| --- | --- |
| `CORPUS_MANIFEST` | Path of the `corpus.json` file. The image sets it to `/app/data/corpus.json`; empty by default outside the image. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins applied as configured, for example `http://127.0.0.1:4174`. Whitespace is stripped and empty entries are ignored. Empty by default: CORS stays closed. GET and POST are allowed without credentials. |

The `sourceFile` values are provenance references: **they are not opened, resolved or interpreted as input paths**. The application does not read any PDF either, and corpus paths are not accepted over HTTP.

`corpus.json` is a plain JSON document with one `papers` array; each paper has `id`, `title`, `authors`, `originalAbstract` and `sourceFile`:

```json
{"papers":[{"id":"paper-01","title":"…","authors":["…"],"originalAbstract":"…","sourceFile":"…"}]}
```

The application reads it once at startup and maps those fields as provided, without runtime integrity validation; the team that curates the file is responsible for the content, completeness and order of the twenty records.

Missing or empty configuration, an unreadable corpus file or a loading failure leaves the corpus unavailable: catalog, detail, ranking and similarity return HTTP 503 `CORPUS_UNAVAILABLE`, even for an unknown ID, while health reports `up`/`unavailable`. An incomplete but readable file is not rejected. With the corpus ready, health returns `up`/`ready`, the catalog includes twenty summaries, the detail keeps the exact original abstract, and an unknown ID returns HTTP 404 `PAPER_NOT_FOUND`. Responses do not include private references, review flags or loading failure details.

`TextPreparation` computes in Java the NFC `baseText` and the tokens NFC → `Locale.ROOT` lowercase → NFC; it keeps runs of Unicode letters/marks/digits only if they contain a letter. It does not remove stopwords or apply stemming, translation or lemmatization. The `unicode-nfc-root-v1` preparation, the originals and the frequencies stay in RAM: only abstracts are traversed, in file order, keeping the first appearance of terms through a `LinkedHashMap`. Similarity also prepares joined tokens and a shared TF-IDF matrix once in RAM; ranking sorts the prepared frequencies on request.

The behavior tests use **synthetic** data in isolated temporary state; they do not prove integration with the real corpus. Coverage includes the three sorts' worked examples, stability, permutations, unchanged inputs, cross-validation against an independent oracle, Unicode preparation, HTTP 200/400/404/503, allowed/rejected/closed-by-default CORS and health without a corpus.

## Ranking measurement

The [ranking report](docs/Ranking%20de%20T%C3%A9rminos%20Mediante%20Algoritmos%20de%20Ordenamiento%20no%20Comparativos.docx) records the
three independent JVM runs, rotated method order, 20 warmups and 100 measured
runs per method/process, exact complete-output agreement, real top 15 and timing
limitations. `RankingBenchmark` is a standalone offline entry point; it does not
start the HTTP server. Its arguments are the `corpus.json` path and the
comma-separated method order, for example `counting,radix,bucket`. It prints
`source_commit` from the JVM property `-Dbenchmark.sourceCommit` as given,
without validating it.

## Repository contents

Version sources, tests, wrapper, build configuration, Dockerfile, `data/` and technical documentation. Do not version credentials, caches, build outputs, model files or private instructions/notes. `.env.example`, if added, must contain only fictitious values.
