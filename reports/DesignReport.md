# Assignment 3 — DIRT: Extraction of Lexico-syntactic Similarities  
## Design Report (Small implemented; Medium/Large placeholders)

### 1. Goal
Implement and evaluate the DIRT pipeline (Lin & Pantel) on the Google Syntactic N-grams (Biarcs) corpus:
1. Extract lexico-syntactic **predicate templates** (dependency paths) with **X/Y** argument slots.
2. Compute MI feature values **MI(p, slot, w)** for all discovered `(predicate, slot, filler)` triples.
3. Score similarity for provided test predicate pairs.

---

### 2. Inputs and Outputs

#### 2.1 Input corpus
- Dataset: **Biarcs** (Google Syntactic N-grams dependency fragments).
- Each record contains tokens (index, word, POS, head) and an aggregated count (frequency).

#### 2.2 Test set
- `positive-preds.txt` — tab-separated predicate pairs labeled entailment (pos).
- `negative-preds.txt` — tab-separated predicate pairs labeled non-entailment (neg).

#### 2.3 Outputs
1. `mi.tsv` — MI feature table:  
   `predicate<TAB>slot<TAB>filler<TAB>mi`
2. `out_scores.tsv` — similarity scores for test pairs:  
   `label<TAB>p1<TAB>p2<TAB>score`

---

### 3. Core Extraction Logic (Per Record)
Component: `com.dsp.dirt.extract.PathExtractor`

#### 3.1 Token reconstruction
For each parsed record:
- Rebuild token array `tok[index]` from the record’s token list.
- Build `parent[i] = headIndex` for dependency navigation (root = 0).

#### 3.2 Constraints (assignment requirements)
We only extract predicates that satisfy:
- **Slots X/Y are nouns**: fillers must be `NN*`.
- **Head is a verb**: the dependency-path head must be `VB*`.
- **Auxiliary filtering**: filter auxiliary verbs (e.g., *is/are/was/have/do/modals*) as heads and also remove auxiliary tokens from predicate paths.

#### 3.3 Stanford parser preposition handling (assignment requirement)
Unlike MiniPar, Stanford dependencies retain prepositions as nodes. Therefore, predicate templates **include prepositions**:
- Include connectors `IN`, `TO` (and optionally `RP`)  
- In addition to content POS: `VB*`, `JJ*`, `RB*`

#### 3.4 Multiple paths per record
A record may yield multiple noun pairs and thus multiple predicates.
For every noun pair `(a,b)`:
- Compute `l = LCA(a,b)` in the dependency tree.
- If `tok[l]` is a non-auxiliary verb, build the predicate template in both directions:
  - `a → b` assigns `X=a`, `Y=b`
  - `b → a` assigns `X=b`, `Y=a`

#### 3.5 Stemming (assignment requirement)
Because the corpus is not stemmed but test predicates are in base form:
- Apply Porter stemming to predicate tokens (excluding `X`/`Y`).
- Stem noun fillers in X/Y slots.

#### 3.6 Extracted instance format
Each extracted instance is represented as:
- `PathInstance(pathKey, slot, filler, count)`

where:
- `pathKey` is a space-separated predicate template (e.g., `X associ with Y`)
- `slot ∈ {X,Y}`
- `filler` is the stemmed noun occupying that slot
- `count` is the record’s aggregated frequency

---

### 4. MapReduce Pipeline

#### 4.1 Job A — Triple Counts
Component: `com.dsp.dirt.job.TripleCountsJob`

**Mapper input**: raw Biarcs lines  
**Mapper output**: emits 4 count keys per extracted instance, weighted by `count`.

Key formats (tab-separated):
1. `T<TAB>p<TAB>slot<TAB>w`  for `|p,slot,w|`
2. `PS<TAB>p<TAB>slot`      for `|p,slot,*|`
3. `SW<TAB>slot<TAB>w`      for `|*,slot,w|`
4. `S<TAB>slot<TAB>*`       for `|*,slot,*|`

**Combiner/Reducer**: sum counts per key.

**Global total for MI**
- Maintain Hadoop counter `DIRT:TOTAL_T` that accumulates the total mass of all `T` events:
  - `TOTAL_T = |*,*,*|` over all `(p,slot,w)` triples.
- After Job A finishes, inject this counter value into the configuration for Job B under:
  - `DIRT_TOTAL_T`

**Scale + memory notes**
- Each extracted `PathInstance` produces exactly 4 KV emissions.
- Reducer does streaming summation; memory is O(1) per active key plus Hadoop overhead.

#### 4.2 Job B — MI Computation
Component: `com.dsp.dirt.job.ComputeMIJob`

**Input**: Job A output lines (`KEY<TAB>COUNT`).

**Mapper (partition by slot)**
- Route all statistics needed for MI to reducers keyed by `slot`.
- Emit reducer key = `slot`, value tagged:
  - `S<TAB>count`
  - `SW<TAB>w<TAB>count`
  - `PS<TAB>p<TAB>count`
  - `T<TAB>p<TAB>w<TAB>count`

**Reducer (per slot)**
For each slot:
- Build maps:
  - `c_ps[p] = |p,slot,*|`
  - `c_sw[w] = |*,slot,w|`
- Iterate over each triple `|p,slot,w|` and compute MI using global `totalT` from `DIRT_TOTAL_T`:

\[
MI(p,slot,w) = \log \frac{|p,slot,w|\cdot |*,*,*|}{|p,slot,*|\cdot |*,slot,w|}
\]

Emit:
- key: `p<TAB>slot<TAB>w`
- value: `mi` (double)

**Reducer memory considerations**
- Dominant memory is the two hash maps per slot (`c_ps`, `c_sw`).
- Since `slot ∈ {X,Y}`, the data is naturally partitioned into two reducer partitions.

---

### 5. Local Similarity Scoring
Components: `com.dsp.dirt.local.EvalPairs`, `com.dsp.dirt.local.SimilarityScorer`

Inputs:
- `mi.tsv` (merged from Job B output)
- `positive-preds.txt`, `negative-preds.txt`

Steps:
1. Normalize predicates in the test set using the same stemming logic (lowercase + stem tokens except X/Y).
2. Load MI features only for predicates needed by the test pairs (optimization).
3. Compute DIRT similarity:
\[
sim(p,q) = \sqrt{sim_X(p,q)\cdot sim_Y(p,q)}
\]
where each `sim_slot` is a MI-weighted overlap score across fillers in that slot.

Output:
- `out_scores.tsv` in format `label<TAB>p1<TAB>p2<TAB>score`

---

### 6. Experimental Runs

#### 6.1 Small run (10 files) — completed
Fill the exact run details:
- Input: 10 Biarcs files (S3 prefix/bucket): ____________________
- Output base: ____________________
- Cluster settings (type/count): ____________________
- Reducers (Job A / Job B): ____________________
- Notes (compression, auto-terminate, etc.): ____________________

Observed behavior (from analysis):
- Similarity scores are highly sparse (many zeros), expected for small input.

#### 6.2 Medium run (20 files) — placeholder
Input composition:
- 10 original files: ____________________
- Additional 10 files distributed across lexicographic file indices: ____________________
  - Rationale: spread across index ranges to diversify lexical coverage and reduce sampling bias.
- List exact S3 URIs used: ____________________

Expected differences vs small:
- More predicate coverage, more non-zero similarity.
- Negatives may start getting non-zero scores → possible false positives.

#### 6.3 Large run (100 files) — placeholder
Fill after execution:
- Input size used: ________ (state exact number if < 100)
- Expected differences vs medium:
  - further reduction in sparsity
  - more stable threshold selection
  - clearer PR trade-off; likely more FPs due to increased overlap

---

### 7. Memory, Scale, and Cost Considerations (fill with estimates)

**Job A**
- Emissions: 4 KV pairs per extracted `PathInstance`
- Mapper output volume estimate: ____________________
- Reducer: streaming sum; low per-key memory

**Job B**
- Reducer holds per-slot maps:
  - `c_ps` size ≈ number of predicates seen for that slot
  - `c_sw` size ≈ number of fillers seen for that slot
- Peak memory per reducer estimate: ____________________

**Cost control**
- Output compression: ____________________
- Reducer count: ____________________
- Input selection strategy (distributed indices): ____________________
- Auto-termination + log storage path: ____________________
