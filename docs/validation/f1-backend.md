# F1 backend validation

## Scope and verdict

Reviewed baseline: `f5978ece095387c827610eb536a4ccc7794db824`.

**No blocking overengineering finding was identified in the implemented corpus foundation.** Keep the concrete controller/service/preparation structure and its validation boundaries. This review adds documentation, not a Java refactor, a new architecture, or an implementation of later-phase algorithms.

The requirement references below are the project's PRD v1.0, TRD v1.0, adopted common design contracts, and OpenAPI 1.0.0. These definition documents remain centralized in the parent workspace; they are not duplicated here. Product requirements determine scope, technical/design documents explain implementation, and OpenAPI determines HTTP fields, routes, and errors. Later-phase designs are not evidence of implemented algorithms.

## Requirement-to-implementation review

Paths in this table are relative to this repository. Test-class references identify the existing executable coverage; they do not imply a new real-corpus integration run.

| Requirement / source | Implementation and executable evidence | Assessment |
| --- | --- | --- |
| PRD QC-02; TRD §4: clear concrete responsibilities, no speculative architecture | [CorpusController](../../src/main/java/com/neguentropia/corpus/CorpusController.java) maps HTTP and explicit DTO records; [CorpusService](../../src/main/java/com/neguentropia/corpus/CorpusService.java) owns the validated snapshot; [CorpusLoader](../../src/main/java/com/neguentropia/corpus/CorpusLoader.java) validates input; [TextPreparation](../../src/main/java/com/neguentropia/corpus/TextPreparation.java) computes without Spring or HTTP. | Conforms for this slice. No strategy hierarchy, repository abstraction, generic execution framework, or plugin system is needed. |
| PRD-01; TRD §5: exactly twenty complete, ordered, usable records | Loader checks cardinality, exact ID order, nonblank metadata, author arrays, useful abstract tokens, well-formed Unicode, unique references/hashes, and content hashes before returning a manifest. [CorpusLoaderTest](../../src/test/java/com/neguentropia/corpus/CorpusLoaderTest.java) exercises rejection and integrity cases. | Necessary validation, not redundant application layering. No partially loaded collection escapes a failed load. |
| PRD-01/02; DD “Texto y orden reproducibles”: original text is separate from derived text | Loader retains original strings; the controller explicitly exposes only the catalog/detail fields; preparation returns derived NFC text and tokens. [CorpusEndpointTest](../../src/test/java/com/neguentropia/corpus/CorpusEndpointTest.java) and [TextPreparationTest](../../src/test/java/com/neguentropia/corpus/TextPreparationTest.java) cover the separate views. | Do not normalize, trim, or rewrite the stored original to simplify rendering or preparation. |
| PRD-02; DD common preparation rules | `baseText` uses NFC. Tokens use NFC → `Locale.ROOT` lowercase → NFC, split by Unicode character category, and keep runs only when they contain a letter. Existing preparation tests cover marks, digits, supplementary letters, punctuation, locale independence, spacing, and invalid Unicode. | Explicit and explainable implementation. No stopword list, stemming, translation, or hidden preprocessing was added. |
| PRD-02/03; TRD §5; DD first-appearance order and abstract-only counting | `TextPreparation.frequencies` traverses only `originalAbstract` in manifest order, accumulating through `LinkedHashMap`; [CorpusServiceTest](../../src/test/java/com/neguentropia/corpus/CorpusServiceTest.java) covers prepared data and frequencies. | Preserves deterministic term order without prematurely implementing sorting. Titles/authors identify papers but do not enter frequency counts. |
| Corpus identity and reproducibility; OpenAPI `corpusVersion` / `abstractSha256` | Loader validates UTF-8 strictly, rejects duplicate/trailing JSON content, checks original-text SHA-256, and hashes ordered canonical tuples for the version. Loader tests exercise integrity/serialization boundaries. | Keep the canonical serializer. It preserves exact escaping, Unicode, tuple membership, and ordering; an arbitrary serializer replacement could invalidate otherwise unchanged corpus identity. Source-file hashes are checked structurally and for uniqueness, not against PDF bytes at runtime. |
| OpenAPI catalog/detail/health and explicit errors; TRD §7 | Controller returns catalog/detail only when ready, `503 CORPUS_UNAVAILABLE` otherwise, and `404 PAPER_NOT_FOUND` for an unknown ID only when ready. [InvalidCorpusEndpointTest](../../src/test/java/com/neguentropia/corpus/InvalidCorpusEndpointTest.java), catalog/detail tests, and [HealthEndpointTest](../../src/test/java/com/neguentropia/HealthEndpointTest.java) cover public HTTP behavior. | Distinguishes process liveness from corpus readiness. Unknown ID must not mask unavailable corpus state. This review does not claim a fresh full OpenAPI schema/fixture integration sweep. |
| PRD QC-06/07; TRD §§5,7: fail closed and do not leak local input details | Service publishes no manifest on load failure; controller projects explicit public records rather than serializing loader records. Private source references and load exception details are absent from these response DTOs. | The broad catch is an intentional fail-closed boundary, not permission to continue with partial data. Operator diagnostics remain limited; adding diagnostics later would require privacy-focused verification, not exposure through HTTP. |
| PRD QC-06; TRD local-origin restrictions | [CorpusCorsConfiguration](../../src/main/java/com/neguentropia/corpus/CorpusCorsConfiguration.java) allows GET for explicit valid loopback origins, disables credentials, and closes the complete allowlist on invalid configuration. [CorpusCorsConfigurationTest](../../src/test/java/com/neguentropia/corpus/CorpusCorsConfigurationTest.java) and endpoint tests cover configuration and response behavior. | Necessary browser integration guard. CORS is not authentication, spending control, or authorization to invoke a model. No security configuration was changed. |
| PRD QC-04/08; TRD §§11,13–14: container execution, exclusions, phase boundaries | Existing [Dockerfile](../../Dockerfile), [build configuration](../../build.gradle), and [README](../../README.md) document container execution and external data. Current controller marks all nine analysis capabilities unavailable. | No additional service, dependency, CI pipeline, external cache, database, model client, or corpus publication was introduced. This unit does not close F1 or start F2. |

## Complexity decisions and tradeoffs

### Keep validation and explicit records

The loader's validation steps protect different invariants: strict decoding protects bytes, Unicode validation protects strings, field checks protect shape, cardinality/order protect corpus identity, and hashes protect provenance. Removing one because another exists would weaken a different boundary. Immutable list/map snapshots are appropriate for data loaded once at startup.

The HTTP records intentionally differ from loader records. Their short explicit mappings are easier to audit for private-field leakage than a generic mapper. Adding interfaces, MapStruct, a repository layer, or dependency injection for the pure preparation functions has no demonstrated benefit for this implementation.

### Accept repeated preparation for this fixed corpus

The loader tokenizes to reject unusable abstracts, the service builds prepared papers, and frequency aggregation tokenizes originals again. This repeats work during initialization; it is not a different normalization policy or an observed performance failure. The corpus is fixed at twenty abstracts and startup runs once.

A possible later optimization would aggregate the already prepared tokens. It would couple loader validation, preparation lifecycle, and frequency construction more tightly and require tests for identical counts and first-appearance ordering. No benchmark justifies that change now, so the straightforward implementation is retained. No timing or scalability improvement is claimed.

### Do not implement future design sheets early

TF-IDF, sorting, similarity, searching, clustering, model inference, and their timing protocols belong to subsequent authorized work. Their absence from the foundation is not a reason to add factories, empty interfaces, placeholder services, or algorithm dependencies. The capability response makes that absence explicit.

## Observed verification

Executed inside the assigned Docker development environment, not with host Java/Gradle:

```sh
./gradlew --no-daemon test
```

- Observed process exit: **0**; Gradle reported `BUILD SUCCESSFUL` and four executed tasks.
- Process duration reported by the runner: 52 seconds; Gradle reported 51 seconds. These are suite/build durations, **not algorithm benchmarks**.
- Existing JUnit XML reports were read after that execution: **7 suites, 61 tests, 0 failures, 0 errors, 0 skipped**.
- Suites: `HealthEndpointTest`, `CorpusCorsConfigurationTest`, `CorpusEndpointTest`, `CorpusLoaderTest`, `CorpusServiceTest`, `InvalidCorpusEndpointTest`, and `TextPreparationTest`.
- No deterministic behavior was changed, so a new RED test would not meaningfully validate this documentation-only unit. No tests were duplicated or rewritten to manufacture a failure.

The suite uses temporary **synthetic** manifests. It does not establish PDF extraction accuracy, complete real-corpus ingestion, redistribution rights, visual acceptance, production deployment, or teacher acceptance. Those remain separate evidence and authorization boundaries. No original corpus text or private manifest is included in this report.

## Outcome

Retain the existing Java implementation. The actionable outcome is a traceable explanation of necessary validation versus optional optimization, and a reproducible successful foundation test run in the assigned environment. No blocking code defect was confirmed; no speculative refactor is justified by this review.
