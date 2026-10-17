# Bucket Sort with explicit local Counting

`BucketSort.sortDescending` orders the complete prepared list by decreasing frequency. Equal frequencies keep input order, which represents first appearance. The result is immutable; the input is not modified. This is a hybrid interval Bucket Sort, not the conventional comparison-based insertion-sort variant.

## Procedure

1. Validate the list and every record, reject duplicate terms, and find `Fmax`. Frequencies are positive, as enforced by `TermFrequency`.
2. For vocabulary size `V > 0`, choose `B = max(1, floor(sqrt(V)))` and `w = ceil((Fmax + 1) / B)`. Empty input has `B = w = 0` and returns an empty list. `Parameters.forInput` exposes exactly this calculation.
3. Create `B` lists. Visit records in input order and append each to bucket `q = frequency / w`. Its interval starts at `q*w`; all limit arithmetic uses `long`.
4. Visit buckets from `B-1` down to zero, skipping empty lists. In each nonempty bucket, count local keys `frequency - q*w` in an array of length `w`. Accumulate counts from high key to low key, then place records into a local output array by visiting the bucket backwards and decrementing the corresponding cumulative count.
5. Append that local array to the complete output. Return an immutable copy. Top 15 selection belongs to the caller, after the complete sort.

No pair of records is compared to decide its relative position. The maximum scan compares one frequency against a running bound, not records for ordering. Division chooses intervals and array indices choose local positions; arithmetic counts and reverse placement establish the order. A hash set validates uniqueness but never determines ordering. No generic sort or comparison-based fallback is used.

## Worked example

Take `[a:12,b:3,c:12,d:21,e:3,f:8]`. Here `V=6`, `Fmax=21`, so the actual parameter rule gives `B=2`, `w=ceil(22/2)=11`.

| Bucket | Interval | Stable distribution | Local descending output |
| --- | --- | --- | --- |
| 0 | 0..10 | b:3, e:3, f:8 | f:8, b:3, e:3 |
| 1 | 11..21 | a:12, c:12, d:21 | d:21, a:12, c:12 |

For bucket 1, local keys are `[1,1,10]`. Raw counts are key 1 → 2 and key 10 → 1. Descending cumulative counts are key 10 → 1, keys 9 through 2 → 1, keys 1 and 0 → 3. Reverse placement puts d at index 0, c at index 2, then a at index 1. For bucket 0, keys are `[3,3,8]`; cumulative counts give key 8 → 1 and key 3 → 3. Reverse placement puts f at index 0, e at index 2, then b at index 1.

Concatenating bucket 1 then bucket 0 yields exactly `[d:21,a:12,c:12,f:8,b:3,e:3]`. The two ties preserve a before c and b before e.

### Three-interval illustration

For illustration only, distributing the same six records with `B=3` would give `w=8`:

| Bucket | Interval | Stable distribution | Local descending output |
| --- | --- | --- | --- |
| 0 | 0..7 | b:3, e:3 | b:3, e:3 |
| 1 | 8..15 | a:12, c:12, f:8 | a:12, c:12, f:8 |
| 2 | 16..23 | d:21 | d:21 |

Bucket 1 has local keys `[4,4,0]`. Raw counts are key 4 → 2 and key 0 → 1. Descending cumulative counts are keys 7 through 5 → 0, keys 4 through 1 → 2, key 0 → 3. Reverse placement puts f at index 2, c at index 1, a at index 0. Concatenation 2,1,0 gives the same final list. The public method never overrides its parameter rule: the tests exercise actual `B=3,w=8` by extending the input with g:7, h:16 and i:15, making `V=9` while leaving `Fmax=21`.

## Complexity and assumptions

Let `r` be the number of nonempty buckets, `vq` the size of bucket q, and `w` the common width. Distribution and bucket creation cost O(V+B). Local Counting costs O(vq+w) per nonempty bucket, including cumulative counts and placement. Concatenation and immutable copying cost O(V). Thus the sorting work is O(V+B+r*w), bounded by O(V+B+B*w).

Because `B*w < Fmax+1+B`, the overall bound is O(V+B+Fmax). It becomes O(V) only under an assumption such as `Fmax=O(V)` (and this rule has `B=O(sqrt(V))`). It is not unconditionally linear, nor does it require uniformly distributed frequencies. Empty buckets save local passes, but a large sparse frequency range can still dominate.

Uniqueness validation uses a hash set with expected O(V) operations; hashing terms also depends on their string lengths. That assumption is separate from the integer sorting bound. Live auxiliary storage is O(V+B+w): bucket lists, validation set, accumulated output, immutable copy, and one local count/output pair at a time. Total temporary allocation over the run may be O(V+r*w); local arrays are not retained for all buckets simultaneously.

## Limits

- Null list or element throws `NullPointerException`; duplicate terms throw `IllegalArgumentException`.
- `Fmax=Long.MAX_VALUE` cannot represent `Fmax+1` in `long` and is explicitly rejected. Ceiling division uses quotient and remainder to avoid addition overflow.
- A width greater than `Integer.MAX_VALUE-8` is explicitly rejected before narrowing or allocation. Smaller arrays can still exceed the JVM heap and raise `OutOfMemoryError`; there is no silent fallback. Parameter calculation itself does not allocate local count arrays and can describe a width that sorting cannot allocate.
- This hybrid is the selected technical implementation. Academic acceptance of the hybrid variant remains unconfirmed.
