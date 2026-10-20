# Full-corpus ranking measurements

## Scope and protocol

Measured on 2026-10-20 (UTC), using the three implemented sorts over the same
20-abstract corpus. This is an offline kernel experiment, not HTTP latency or an
end-to-end preparation benchmark. No PDF, title or author text enters the counts.

`RankingBenchmark` loads and validates the supplied manifest with `CorpusLoader`,
then prepares `TextPreparation.frequencies` once, in manifest/first-appearance
order. Preparation, common input copies, statistics and top selection are outside
the clock. For each method, it executes **20 warmups followed by 100 measured
invocations**. Every invocation receives a new `ArrayList` copied before the
clock. Only `sortDescending` lies between the two `System.nanoTime()` calls;
the difference is divided by `1_000_000.0`. Validation, internal allocations,
Counting accumulation, every Radix pass, and Bucket distribution/local Counting/
concatenation/immutable output all remain inside that interval.

Every complete output (including warmups) is compared with the first complete
output outside timing. A disagreement fails the process rather than printing a
successful comparison. All three processes ended with `complete_outputs_equal=true`.
Statistics use a sorted copy of durations, and the even-size median is the mean
of the two central observations. No library sort orders terms in production.

Three sequential, independent JVMs ran in disposable containers, with orders:

1. counting, radix, bucket
2. radix, bucket, counting
3. bucket, counting, radix

Each process measured 100 samples per method; overall statistics below pool all
300 measured samples for each method, **not** the three process medians. Across
the experiment each method had 60 warmups and 300 measured runs. All 900 measured
sort outputs and 180 warmup outputs were checked in full.

## Identification

- `corpusVersion`:
  `abstracts-sha256-062c5fd325318e59d4b505d40cb36a5330a91d280dba16739c3f0aae36d5dd11`.
- Measured source commit: `0ed46727b3ba5d241a3c0f067d49b40d46c21d67`.

`RankingBenchmark` prints both identifiers as `corpus_version` and `source_commit`
in every JVM run. It reads the corpus version from the validated manifest and
refuses to run without a valid hexadecimal source-commit identifier supplied
through `benchmark.sourceCommit`. The JAR used here was built from the clean
source commit identified above; the harness validates the identifier's format,
not the JAR's correspondence to a Git checkout.

## Environment and reproduction

- CPU: 12th Gen Intel Core i9-12900H, one socket, 14 physical cores,
  20 logical processors visible to the JVM (hybrid processor).
- Architecture: x86_64, Linux Docker host; no CPU pinning or isolation from
  unrelated host activity, no explicit CPU/memory limits or heap tuning.
- Runtime: Java 25.0.4.1; image
  `eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64`.
- Build: Gradle wrapper 9.8.1, `./gradlew bootJar`.
- Measurement containers: no network, read-only root filesystem, unprivileged
  UID/GID 1000:1000, JAR and manifest bind-mounted **read-only**, no corpus copy.
  No server or Spring application context starts.

Run the following once for each order above, supplying local absolute paths via
`JAR` and `MANIFEST` (do not place the manifest in the repository or image).
Build the JAR from the identified commit and set `COMMIT` to that same revision:

```sh
COMMIT=0ed46727b3ba5d241a3c0f067d49b40d46c21d67
docker run --rm --pull=never --network none --read-only --user 1000:1000 \
  --mount "type=bind,src=$JAR,dst=/app.jar,readonly" \
  --mount "type=bind,src=$MANIFEST,dst=/corpus/manifest.json,readonly" \
  eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64 \
  java -Dbenchmark.sourceCommit=$COMMIT \
  -Dloader.main=com.neguentropia.ranking.RankingBenchmark \
  -cp /app.jar org.springframework.boot.loader.launch.PropertiesLauncher \
  /corpus/manifest.json "$ORDER"
```

The harness prints plain key/value lines: configuration, all individual measured
samples, statistics, top 15 and complete-output equality. It never prints abstracts
or host paths. Printed
decimal digits preserve observations, not a claim of equivalent timer accuracy.

## Input and configuration

| Quantity | Observed value |
| --- | ---: |
| Abstracts | 20 |
| Total tokens | 4002 |
| Distinct terms V | 1079 |
| Maximum frequency Fmax | 221 |
| Counting range Fmax+1 | 222 |
| Radix base | 10 |
| Radix passes | 3 |
| Bucket B = floor(sqrt(V)) | 32 |
| Bucket width ceil((Fmax+1)/B) | 7 |

Preparation is `unicode-nfc-root-v1`: NFC, root-locale lowercase, NFC, Unicode
letter/mark/digit runs containing a letter. No stopword removal or stemming.
Ties keep first appearance, never alphabetical order. The source is exclusively
the same complete corpus of 20 verified abstracts; this experiment does not
change its provenance or establish additional data-use permissions.

## Measured times (milliseconds)

| Process | Method | Median | Minimum | Maximum |
| --- | --- | ---: | ---: | ---: |
| 1 | counting | 0.06379 | 0.036959 | 0.355036 |
| 1 | radix | 0.0833035 | 0.040714 | 0.364776 |
| 1 | bucket | 0.09157 | 0.061934 | 0.397255 |
| 2 | counting | 0.0604785 | 0.033191 | 0.294857 |
| 2 | radix | 0.0626755 | 0.042502 | 0.360446 |
| 2 | bucket | 0.1561865 | 0.06312 | 4.955768 |
| 3 | counting | 0.060062 | 0.032206 | 5.320922 |
| 3 | radix | 0.115189 | 0.044968 | 0.234162 |
| 3 | bucket | 0.20235999999999998 | 0.10581 | 0.526716 |
| Overall (300 samples) | counting | 0.0632715 | 0.032206 | 5.320922 |
| Overall (300 samples) | radix | 0.1009755 | 0.040714 | 0.364776 |
| Overall (300 samples) | bucket | 0.176704 | 0.061934 | 4.955768 |

## Top 15 from each implemented method

Each cell below comes from the real harness output. The three processes yielded
the same table; equality was checked on all 1079 records, not only these 15.

| Rank | Counting term | Frequency | Radix term | Frequency | Bucket term | Frequency |
| ---: | --- | ---: | --- | ---: | --- | ---: |
| 1 | and | 221 | and | 221 | and | 221 |
| 2 | the | 197 | the | 197 | the | 197 |
| 3 | of | 170 | of | 170 | of | 170 |
| 4 | in | 125 | in | 125 | in | 125 |
| 5 | to | 95 | to | 95 | to | 95 |
| 6 | ai | 86 | ai | 86 | ai | 86 |
| 7 | a | 61 | a | 61 | a | 61 |
| 8 | education | 57 | education | 57 | education | 57 |
| 9 | for | 54 | for | 54 | for | 54 |
| 10 | research | 46 | research | 46 | research | 46 |
| 11 | learning | 40 | learning | 40 | learning | 40 |
| 12 | aied | 38 | aied | 38 | aied | 38 |
| 13 | this | 37 | this | 37 | this | 37 |
| 14 | review | 36 | review | 36 | review | 36 |
| 15 | educational | 33 | educational | 33 | educational | 33 |

## Cross-validation

`RankingCrossValidationTest` uses seed `0x5EEDB0C`, sizes
0, 1, 2, 3, 9, 15, 16, 31, 100, 999, 2048 and 3000, and five shapes:
all equal, all distinct, many ties, one frequency of 1,000,000, and random
frequencies from 1 through 1,000,000. It shuffles each input deterministically.
For all 60 size/shape combinations, each complete method output equals an
independent stable comparator oracle; the original input is unchanged.
An additional synthetic-abstract case exercises the actual text preparation,
Unicode normalization, exclusion of metadata and nonalphabetical ties.

At Integer.MAX_VALUE and Long.MAX_VALUE-1, Counting's unsupported range is
explicitly rejected and Radix equals the oracle. Bucket rejects unsupported
local widths before allocation; a representable but enormous width is not
allocated deliberately in the tests. Radix also agrees at Long.MAX_VALUE;
Bucket explicitly rejects that unrepresentable Fmax+1. No claim is made that
Counting or Bucket can allocate arbitrary long ranges. Radix supports positive
long values, with at most 19 passes.

## Interpretation and complexity assumptions

The absolute times are small because only 1079 already-prepared records are
sorted in memory. There is no disk I/O, text preparation, HTTP or network inside
the clock. Counting visits just 222 range slots; Radix uses three decimal passes;
Bucket uses 32 intervals of width 7. Internal validation and allocation costs
remain measurable and can dominate the integer ordering work at this scale.

Counting has O(V+Fmax) time and space, not unconditional O(V). Here Fmax=221 is
smaller than V, so its range cost is modest. Radix has O(d(V+10)) kernel time
and O(V+10) live auxiliary space; here d=3, while fixed-width long limits d to
19. Bucket is the explicit hybrid interval/Counting implementation, with
O(V+B+r*w) work for r nonempty buckets, bounded by O(V+B+Fmax), and
O(V+B+w) live auxiliary space. Here B*w=224; its range assumptions are favorable.
Hash-based duplicate validation has expected linear operations and term-string
costs in addition to those integer-kernel bounds. None of these measurements
proves asymptotic complexity or uniform frequency distributions.

Counting has the lowest pooled median in this run, but medians vary across
processes with different method orders and JVM states. Counting and Bucket
maxima include millisecond-scale outliers,
consistent with scheduling, allocation/GC and compilation effects; their exact
causes were not profiled. Twenty warmups do not establish steady-state JIT.
The timer reports nanoseconds but its effective resolution/overhead was not
calibrated, and sub-millisecond readings have substantial noise relative to the
work. The reported precision is retained raw evidence, not significant-digit
certainty. Rotating order reduces a fixed-order bias; three JVMs do not remove
it or establish statistical significance. No winner, speedup guarantee,
production latency prediction or extrapolation to larger corpora is justified.
If stronger performance claims are needed, revisit warmup/sample counts,
calibrate timer overhead and control host load before drawing them.

The hybrid Bucket variant remains subject to academic acceptance; measurements
and equality do not resolve that separate requirement.
