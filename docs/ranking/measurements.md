# Full-corpus ranking measurements

## Scope and protocol

Measured on 2026-10-17 (UTC), using the three implemented sorts over the same
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

## Environment and reproduction

- CPU: 12th Gen Intel Core i9-12900H, one socket, 14 physical cores,
  20 logical processors visible to the JVM (hybrid processor).
- Architecture: x86_64, Linux Docker host; no CPU pinning or isolation from
  unrelated host activity, no explicit CPU/memory limits or heap tuning.
- Runtime: Java 25.0.4.1; image
  `eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64`.
- Build: Gradle wrapper 9.8.1 in the contributor's development container,
  `./gradlew --no-daemon --console=plain bootJar` (exit 0).
- Measurement containers: no network, read-only root filesystem, unprivileged
  UID/GID 1000:1000, JAR and manifest bind-mounted **read-only**, no corpus copy.
  All three invocations exited 0. No server or Spring application context starts.

Run the following once for each order above, supplying local absolute paths via
`JAR` and `MANIFEST` (do not place the manifest in the repository or image):

```sh
docker run --rm --pull=never --network none --read-only --user 1000:1000 \
  --mount "type=bind,src=$JAR,dst=/app.jar,readonly" \
  --mount "type=bind,src=$MANIFEST,dst=/corpus/manifest.json,readonly" \
  eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64 \
  java -Dloader.main=com.neguentropia.ranking.RankingBenchmark \
  -cp /app.jar org.springframework.boot.loader.launch.PropertiesLauncher \
  /corpus/manifest.json "$ORDER"
```

The harness prints plain key/value lines: configuration, all individual measured
samples, statistics, top 15 and complete-output equality. It never prints abstracts
or host paths. Raw output, stderr, exact expanded commands, exits and UTC dates
were retained separately as private evidence, not as repository assets. Printed
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
| 1 | counting | 0.06619900000000001 | 0.034503 | 0.274811 |
| 1 | radix | 0.102153 | 0.044434 | 3.892686 |
| 1 | bucket | 0.0795875 | 0.057832 | 0.335151 |
| 2 | counting | 0.047254500000000005 | 0.033887 | 0.366297 |
| 2 | radix | 0.069436 | 0.046429 | 0.377221 |
| 2 | bucket | 0.1244925 | 0.059197 | 5.217948 |
| 3 | counting | 0.041851 | 0.031889 | 4.591844 |
| 3 | radix | 0.11688699999999999 | 0.04216 | 0.297304 |
| 3 | bucket | 0.177821 | 0.063758 | 0.403166 |
| Overall (300 samples) | counting | 0.052687 | 0.031889 | 4.591844 |
| Overall (300 samples) | radix | 0.1009375 | 0.04216 | 3.892686 |
| Overall (300 samples) | bucket | 0.16355199999999998 | 0.057832 | 5.217948 |

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

## Cross-validation and test evidence

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
Counting or Bucket can allocate arbitrary long ranges. The older Radix design
sheet describes int frequencies; the implemented common record and Radix support
positive long values, with at most 19 passes.

The new harness tests first failed compilation because `RankingBenchmark` did
not yet exist (9 missing-symbol errors, exit 1). Existing sorts require no new
implementation, so their added cross-validation tests are regression checks,
not a manufactured failing algorithm test. An initial boundary-test expectation
incorrectly assumed that Bucket would reject an int-range width; it instead
attempted a representable array and exhausted the test heap. The test now only
requires pre-allocation rejection when the actual width exceeds the supported
array length, without changing any sort or hiding a fallback.

Final full-suite command `./gradlew --no-daemon --console=plain test` inside the
development container passed (exit 0). XML totals: **102 tests, 0 failures,
0 errors, 0 skipped**; the six new test methods passed. Statistics tests cover
odd/even/single samples, preservation of the source array, invalid durations,
and valid/invalid method permutations.

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

Counting has the lowest pooled median in this run, but process medians depend
on method position and JVM state. Maxima include millisecond-scale outliers,
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
and equality do not resolve that separate requirement. This report contributes
the empirical comparison, not overall phase closure or integration approval.
