# Stable descending decimal Radix Sort

`RadixSort.sortDescending` orders the complete prepared list of `TermFrequency`
records by descending frequency. Equal frequencies retain input order, which is
first-appearance order when supplied by the shared frequency preparation. The
result is immutable and the input is not modified. Selecting the top 15 belongs
to the caller after the complete ordering.

## Procedure

1. Reject a null list, null records and duplicate terms. Copy the records into
   an array and find the maximum frequency, `Fmax`. `TermFrequency` already
   enforces nonempty terms and positive `long` frequencies.
2. Compute `d = passCount(Fmax)` by repeated integer division by `BASE = 10`.
   For a positive maximum this equals `floor(log10(Fmax)) + 1`, without using
   floating-point logarithms. Zero denotes empty input and requires no passes;
   a negative maximum is invalid.
3. For exponents 1, 10, 100, ..., perform one stable counting pass on the digit
   `(frequency / exponent) % BASE`:
   - Count occurrences in ten slots.
   - Accumulate suffix counts from digit 8 down to 0. Each count now denotes
     the exclusive end position for that digit in descending digit order.
   - Traverse the current array from right to left. Decrement the digit's end
     position and place the whole record there in a new output array.
4. Use that output as the next pass's input. Return an immutable copy after
   the most significant digit. Never reverse the final array: reversing would
   also reverse equal-frequency records.

The exponent is multiplied only when another pass remains. Positive `long`
values require at most 19 decimal passes, so the largest exponent is `10^18`;
`10^19` is never evaluated. This implementation supports the shared `long`
record rather than limiting frequencies to `int` (whose maximum needs ten
passes).

## Worked example: every pass

Input: `[a:12, b:3, c:12, d:21, e:3, f:8]`. Here `Fmax=21` and `d=2`.

### Pass 1: units, exponent 1

Digits in current order: `[2, 3, 2, 1, 3, 8]`.
Nonzero counts are `count[1]=1`, `count[2]=2`, `count[3]=2`, `count[8]=1`.
The descending groups occupy these output index ranges:

| Digit | Output indices | Stable records |
| --- | --- | --- |
| 8 | 0 | f:8 |
| 3 | 1–2 | b:3, e:3 |
| 2 | 3–4 | a:12, c:12 |
| 1 | 5 | d:21 |

Right-to-left placement writes f at 0, e at 2, d at 5, c at 4, b at 1,
and a at 3. The first output is
`[f:8, b:3, e:3, a:12, c:12, d:21]`.

### Pass 2: tens, exponent 10

Digits in the new current order: `[0, 0, 0, 1, 1, 2]`.
Counts are `count[0]=3`, `count[1]=2`, `count[2]=1`.
Right-to-left placement writes d at 0, c at 2, a at 1, e at 5, b at 4,
and f at 3. The final output is
`[d:21, a:12, c:12, f:8, b:3, e:3]`.

The zero-tens group f/b/e retains its units order. The ties a/c and b/e retain
first appearance, not alphabetical order. The tests check both intermediate
arrays through the package-visible counting-pass helper used by the public
method, as well as the final public result.

## Correctness and absence of comparison ordering

After pass r, the records are descending by their r least significant digits.
A stable pass groups the next digit in descending order while retaining the
previous lower-digit order inside each group. Induction therefore gives full
descending frequency order after d passes. Equal full frequencies have equal
digits on every pass, so they never change relative order.

No pair of records is compared to choose its sorted position. Positions come
from digit extraction, counts and suffix sums. The maximum scan compares each
frequency to a scalar maximum only to determine the number of passes. The hash
set checks term uniqueness; it does not order records. There is no comparator,
comparison-sort call, alphabetical tie-break or fallback ordering.

## Complexity and assumptions

Let V be the number of distinct records, b=10, and d the decimal digit count
of Fmax. Copying, validation and finding Fmax require expected O(V) work under
constant-time hash operations and bounded-cost term hashing/equality. String
hashing/equality costs and hash collisions can affect validation time; the
expected bound is not an unconditional worst-case guarantee for arbitrary
term strings.

Each counting pass costs O(V+b), including its new output array. The ordering
kernel therefore costs O(d(V+b)); immutable result construction costs O(V).
For fixed-width Java `long` arithmetic, d<=19 and b=10, giving a linear kernel
bound in V. For unbounded integers, digit count and arithmetic cost cannot be
assumed constant: there is no unconditional O(V) claim in that setting.

Peak live auxiliary space is O(V+b), including the validation set, record
arrays and immutable result. Across passes, O(dV) output slots are allocated
in total, but obsolete pass arrays are not all simultaneously live.

## Limits and verification scope

- Empty input returns an immutable empty list; all other valid inputs return
  every record, including lists shorter than 15. No truncation occurs here.
- Null lists/elements throw `NullPointerException`; duplicate terms and array
  lengths above the conservative `Integer.MAX_VALUE - 8` limit throw
  `IllegalArgumentException`. Actual heap exhaustion remains an explicit JVM
  `OutOfMemoryError`; it never triggers another sorting algorithm.
- Storage depends on V, not Fmax. Even `Long.MAX_VALUE` needs only ten count
  slots per pass; no array indexed by the entire frequency range is allocated.
- The caller must supply a list that is not concurrently modified and already
  represents the intended aggregate. This kernel does not extract abstracts,
  tokenize, count terms, access HTTP, or verify corpus provenance.
- Tests cover both worked-example passes, ties, all-equal frequencies, empty
  and singleton lists, integer/long boundaries, pass counts, invalid input,
  allocation-length rejection, immutability, unchanged input and exact
  record-level permutation with descending stable order.
- Synthetic correctness tests do not establish full-corpus timing or equality
  with the other two methods. Those require integration on the same prepared
  corpus input. Preparation and the common input copy stay outside the timing
  interval; internal validation, structures, passes and output construction
  belong inside it. Top selection and result checks occur afterwards.
