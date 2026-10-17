# Counting Sort for the term ranking

Implementation: `src/main/java/com/neguentropia/ranking/CountingSort.java`. Tests: `src/test/java/com/neguentropia/ranking/CountingSortTest.java`.

## Input and output

The input is the prepared list of `V` distinct terms with positive frequencies, aggregated from the 20 verified abstracts in first-appearance order. The output contains every record, ordered by frequency from highest to lowest. Terms with the same frequency keep their input order, so ties resolve by first appearance and never alphabetically. The top 15 is taken from this complete output afterwards.

Empty input returns an empty list. A `null` list or element, a duplicated term, or a frequency range that cannot be allocated raises an explicit exception; the method never falls back to another ordering.

## Procedure

Let `Fmax` be the largest frequency.

1. **Count.** Allocate `positions[0..Fmax]` and add one for each term at the index of its frequency.
2. **Accumulate from the top.** Walk the frequencies from `Fmax - 1` down to `0` and add the next cell: `positions[f]` becomes the number of terms with frequency `>= f`, which is the exclusive end of the block reserved for frequency `f` in a descending list.
3. **Place from right to left.** Walk the input backwards and write each record at `--positions[frequency]`. Walking backwards fills each block from its end, so tied terms keep their input order.

No step compares two records with each other. A record's position comes only from the counts, which are indexed directly by its frequency.

## Worked example

Input: `[a:12, b:3, c:12, d:21, e:3, f:8]`, so `Fmax = 21`.

| Frequency | Count | Cumulative from the top |
| ---: | ---: | ---: |
| 21 | 1 | 1 |
| 12 | 2 | 3 |
| 8 | 1 | 4 |
| 3 | 2 | 6 |

All other frequencies have count zero and simply carry the cumulative value forward.

| Record (read right to left) | `--positions[frequency]` |
| --- | ---: |
| f:8 | 3 |
| e:3 | 5 |
| d:21 | 0 |
| c:12 | 2 |
| b:3 | 4 |
| a:12 | 1 |

Result: `[d:21, a:12, c:12, f:8, b:3, e:3]`. The ties keep their order: `a` before `c`, and `b` before `e`. Sorting ascending and reversing would also reverse the ties, which is why the accumulation runs from the top.

## Complexity

Finding `Fmax` and validating the input take `O(V)`. Counting and placing take `O(V)`. Accumulating takes `O(Fmax)`. The total is **`O(V + Fmax)`** time and **`O(V + Fmax)`** extra space. This is the textbook `O(n + k)` with `n = V` and `k = Fmax + 1`.

It is linear in `V` only when `Fmax` grows at most proportionally to `V`. A single very frequent term increases `k` and therefore both time and memory, independently of `V`. For that reason the implementation rejects ranges that cannot be allocated as an array rather than switching silently to a comparison sort.

## Limitations

- Memory depends on the largest frequency, not on the number of terms.
- The algorithm needs integer keys; it relies on frequencies being positive whole numbers.
- Execution times belong to the shared ranking measurement, not to this note.
