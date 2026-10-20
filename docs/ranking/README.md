# Term ranking with non-comparison sorts

This document answers the follow-up requirement: rank the most frequent terms of the 20 abstracts with Counting Sort, Radix Sort and Bucket Sort, explain their complexity, measure them on the real corpus and validate their top 15.

| Requirement | Where it is answered |
| --- | --- |
| Theoretical foundation: why the methods reach O(n+k) or O(n) | [Complexity](#complexity) and the three method notes |
| Empirical measurement in milliseconds on the full corpus | [Measurements](#measurements) and [measurements.md](measurements.md) |
| Top 15 terms obtained with each method | [Validation](#validation-top-15) |

Method notes, each written by the member who implemented the method:

- [Counting Sort](counting-sort.md): `CountingSort`.
- [Radix Sort](radix-sort.md): `RadixSort`.
- [Bucket Sort](bucket-sort.md): `BucketSort`.

## Shared input and output

All three methods receive the same list: every distinct term of the 20 verified abstracts with its frequency, in first-appearance order. Only abstracts are counted; titles, authors and other sections are excluded. The preparation applies NFC normalization and root-locale lowercase. Tokens are Unicode letter, mark and digit runs that contain at least one letter. There is no stopword removal and no stemming.

Each method returns the complete list ordered by frequency from highest to lowest. Ties keep first appearance, never alphabetical order. The top 15 is taken afterwards. `POST /api/ranking` runs the three methods, checks that their complete outputs are identical and returns each top 15 with its sort time.

None of the three methods compares two records to decide their order. Positions come from counts indexed by the frequency itself (Counting), by one decimal digit at a time (Radix), or by the frequency interval followed by a local count (Bucket).

## Complexity

Let `V` be the number of distinct terms (the `n` of the requirement) and `Fmax` the largest frequency.

| Method | Time | Extra space | Why |
| --- | --- | --- | --- |
| Counting | O(V + Fmax) | O(V + Fmax) | One pass to count, one pass over the `Fmax + 1` counters to accumulate, one pass to place. This is O(n + k) with `k = Fmax + 1`. |
| Radix, base 10 | O(d · (V + 10)) | O(V + 10) | `d` stable counting passes, one per decimal digit of `Fmax`, each over ten digit slots. With fixed-width numbers `d` is bounded (at most 19 for a Java `long`), so the time is O(V). |
| Bucket, intervals + local Counting | O(V + B + Fmax) | O(V + B + w) | Distribution into `B = floor(sqrt(V))` intervals of width `w = ceil((Fmax + 1) / B)` is O(V + B); the local Counting in each non-empty bucket is O(v_q + w), and `B · w < Fmax + 1 + B`. |

None of the three is linear unconditionally. Counting and Bucket are linear in `V` when `Fmax` grows at most proportionally to `V`. Radix is linear when the number of digits is bounded. A single extremely frequent term increases the cost of Counting and Bucket regardless of `V`. Bucket is a declared hybrid: intervals plus an explicit Counting Sort inside each interval, never a comparison sort.

On the real corpus, `V = 1079` and `Fmax = 221`. That gives Counting 222 counters, Radix 3 passes and Bucket `B = 32` intervals of width `w = 7`. In this corpus `Fmax < V`, so all three bounds are linear in practice.

## Measurements

The protocol has 20 warm-up runs and 100 measured runs per method, in three independent JVMs with the method order rotated. Each run receives a fresh copy of the same input. Only the sort is timed, using `System.nanoTime()` and reporting milliseconds. The complete outputs were compared in every run. Full details, environment and per-process tables: [measurements.md](measurements.md).

| Method | Median of 300 runs (ms) | Minimum (ms) | Maximum (ms) |
| --- | ---: | ---: | ---: |
| Counting | 0.0632715 | 0.032206 | 5.320922 |
| Radix | 0.1009755 | 0.040714 | 0.364776 |
| Bucket | 0.176704 | 0.061934 | 4.955768 |

These measurements use `corpusVersion=abstracts-sha256-062c5fd325318e59d4b505d40cb36a5330a91d280dba16739c3f0aae36d5dd11` and source commit `0ed46727b3ba5d241a3c0f067d49b40d46c21d67`.

The times are a fraction of a millisecond because only 1079 records are sorted. At this scale validation, allocation, JIT and scheduling effects are comparable to the work itself. The ordering of the medians is an observation of this run, not a general ranking of the algorithms, and it must not be extrapolated to larger inputs.

## Validation: top 15

The three methods returned the same complete list of 1079 records in every measured run, so their top 15 is identical:

| Rank | Term | Frequency | Counting | Radix | Bucket |
| ---: | --- | ---: | :---: | :---: | :---: |
| 1 | and | 221 | ✓ | ✓ | ✓ |
| 2 | the | 197 | ✓ | ✓ | ✓ |
| 3 | of | 170 | ✓ | ✓ | ✓ |
| 4 | in | 125 | ✓ | ✓ | ✓ |
| 5 | to | 95 | ✓ | ✓ | ✓ |
| 6 | ai | 86 | ✓ | ✓ | ✓ |
| 7 | a | 61 | ✓ | ✓ | ✓ |
| 8 | education | 57 | ✓ | ✓ | ✓ |
| 9 | for | 54 | ✓ | ✓ | ✓ |
| 10 | research | 46 | ✓ | ✓ | ✓ |
| 11 | learning | 40 | ✓ | ✓ | ✓ |
| 12 | aied | 38 | ✓ | ✓ | ✓ |
| 13 | this | 37 | ✓ | ✓ | ✓ |
| 14 | review | 36 | ✓ | ✓ | ✓ |
| 15 | educational | 33 | ✓ | ✓ | ✓ |

A ✓ means that method placed exactly that term with that frequency at that rank. The per-method columns come from the measurement output in [measurements.md](measurements.md#top-15-from-each-implemented-method). Function words dominate because stopwords are not removed; this follows the adopted preparation rules.

Beyond the corpus, `RankingCrossValidationTest` compares the three complete outputs with an independent oracle on 60 generated size and shape combinations. The combinations include all-equal, all-distinct and many-tie inputs, a single large frequency, and up to 3000 terms.

## Limitations

- Bucket by intervals with local Counting is the selected variant; its academic acceptance as the third method has not been confirmed.
- The measurements come from one machine without CPU isolation. They support the observations above, not performance guarantees.
- The ranking reflects the adopted preparation rules; a different tokenization or stopword policy would change the terms.
