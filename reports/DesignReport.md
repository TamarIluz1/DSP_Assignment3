# Assignment 3 — DIRT: Extraction of Lexico-syntactic Similarities  
## Design Report

### 1. Goal
Implement and evaluate the DIRT pipeline (Lin & Pantel) on the Google Syntactic N-grams (Biarcs) corpus:
1. Extract **lexico-syntactic predicates** (dependency paths) with argument slots **X/Y**.
2. Compute MI feature values **MI(p, slot, w)** for all discovered triples.
3. Compute similarity scores for provided test predicate pairs.

---

### 2. Inputs and Outputs

#### 2.1 Input corpus
- Dataset: **Biarcs** (Google Syntactic N-grams dependency fragments)
- Each record contains tokens (index, word, POS, head) and an aggregated count.

#### 2.2 Test set
- `positive-preds.txt` — tab-separated predicate pairs (entails).
- `negative-preds.txt` — tab-separated predicate pairs (not-entails).

#### 2.3 System outputs
1. **MI feature table** (`mi.tsv`):  
   Format: `predicate<TAB>slot<TAB>filler<TAB>mi`
2. **Similarity scores** (`out_scores.tsv`):  
   Format: `label<TAB>p1<TAB>p2<TAB>score`

---

### 3. Core Extraction Logic (Per record)
Component: `com.dsp.dirt.extract.PathExtractor`

#### 3.1 Token reconstruction
- Rebuild an array `tok[index]` from the Biarcs record.
- Construct parent pointers `parent[i] = headIndex` (root = 0).

#### 3.2 Candidate selection
- **Slot fillers:** only nouns (`NN*`) are allowed to fill **X** and **Y**.
- **Path head constraint:** only extract paths where the **head token is a verb** (`VB*`).
- **Auxiliary filtering:** skip auxiliary verbs as heads (e.g., `is, are, was, have, do, modals`).

#### 3.3 Stanford preposition constraint (assignment requirement)
Unlike MiniPar, Stanford dependencies retain prepositions as nodes. Therefore, when building predicates we include:
- content words: `VB*`, `JJ*`, `RB*`
- and also **preposition connectors**: `IN`, `TO`
- (optionally `RP` if present)

#### 3.4 Multiple paths per record
A Biarcs record may contain multiple noun pairs ⇒ multiple dependency paths.  
For each pair of nouns `(a,b)`:
- Compute `l = LCA(a,b)` in the dependency tree.
- If `tok[l]` is a verb and not auxiliary: build predicate for:
  - direction `a→b` (X=a, Y=b)
  - direction `b→a` (X=b, Y=a)

#### 3.5 Stemming
Requirement: test predicates are in base form; corpus tokens are not stemmed.  
We apply a Porter stemmer to:
- predicate internal tokens (except X/Y)
- noun fillers (slot words)

#### 3.6 Extracted instance format
Each extracted instance is:
- `PathInstance(pathKey, slot, filler, count)`
where:
- `pathKey` is a space-separated template, e.g. `X associ with Y`
- `slot ∈ {X,Y}`
- `filler` is a stemmed noun
- `count` is the record’s aggregated frequency

---

### 4. MapReduce Design

#### 4.1 Job A — Triple Counts
Component: `com.dsp.dirt.job.TripleCountsJob`

**Mapper input:** raw Biarcs lines  
**Mapper output:** emits 4 count keys per `PathInstance`, weighted by its `count`:

Key formats (tab-separated):
1. `T<TAB>p<TAB>slot<TAB>w` meaning `|p,slot,w|`
2. `PS<TAB>p<TAB>slot` meaning `|p,slot,*|`
3. `SW<TAB>slot<TAB>w` meaning `|*,slot,w|`
4. `S<TAB>slot<TAB>*` meaning `|*,slot,*|`

**Reducer/Combiner:** sums counts per identical key.

**Global total needed for MI:**  
We compute `|*,*,*|` over all triples (T-events), using a Hadoop counter:
- counter group/name: `DIRT:TOTAL_T`

This total is passed to Job B via configuration key:
- `DIRT_TOTAL_T`

#### 4.2 Job B — MI Computation
Component: `com.dsp.dirt.job.ComputeMIJob`

**Input:** Job A output lines: `KEY<TAB>COUNT`

**Mapper strategy: partition by slot**  
Each line is routed to reducer key = `slot`, and the value is tagged:
- `S<TAB>count`
- `SW<TAB>w<TAB>count`
- `PS<TAB>p<TAB>count`
- `T<TAB>p<TAB>w<TAB>count`

**Reducer logic per slot**
- Build maps:
  - `c_ps[p] = |p,slot,*|`
  - `c_sw[w] = |*,slot,w|`
- For each triple `|p,slot,w|` compute MI using:

\[
MI(p,slot,w) = \log \frac{|p,slot,w|\cdot |*,*,*|}{|p,slot,*|\cdot |*,slot,w|}
\]

Output key: `p<TAB>slot<TAB>w`  
Output value: `mi` (double)

---

### 5. Local Similarity Scoring
Components: `com.dsp.dirt.local.EvalPairs`, `com.dsp.dirt.local.SimilarityScorer`

#### 5.1 Feature loading
- Load MI entries from `mi.tsv`.
- Keep only predicates needed by the test set (optimization).

#### 5.2 Similarity formula (DIRT)
For predicates p and q:
- Slot similarity computed by MI-weighted overlap (implemented in code).
- Final similarity:

\[
sim(p,q) = \sqrt{sim_X(p,q)\cdot sim_Y(p,q)}
\]

Output file:
- `out_scores.tsv`: `label<TAB>p1<TAB>p2<TAB>score`

---

### 6. Experimental Runs

#### 6.1 Small run (10 files)
- Input: **10 Biarcs files** (bucket/prefix: ____________________)
- Output base: ____________________
- Cluster/instance type: ____________________
- Notes: ____________________

Run statistics (fill):
- MI rows: ________
- Unique predicates: ________
- Unique fillers: ________
- Needed predicates in test set: ________
- Predicates covered in MI: ________

#### 6.2 Medium run (20 files)
Input composition:
- 10 original files: `0–10` from bucket/prefix: ____________________
- 10 additional files: indices **14, 27, 34, 46, 50, 52, 67, 73, 85, 95**
  - bucket/prefix for each: ____________________
  - (list exact S3 URIs used in `steps.json` or your run script)

Run statistics (fill):
- MI rows: ________
- Unique predicates: ________
- Unique fillers: ________
- Needed predicates in test set: ________
- Predicates covered in MI: ________

#### 6.3 Large run (100 files) — placeholder
- Planned input: up to 100 files (budget permitting)
- Expected differences vs small/medium:
  - higher predicate coverage
  - fewer missing test predicates
  - more non-zero similarities
  - potentially more false positives due to more overlap

---

### 7. Memory and Scale Considerations (fill with your estimates)
Job A:
- KV pairs per `PathInstance`: 4
- Expected mapper output volume: ____________________
- Reducer memory: streaming sum, low per-key memory

Job B:
- Reducer per slot stores:
  - `c_ps` size ≈ number of predicates in slot
  - `c_sw` size ≈ number of fillers in slot
- Expected peak memory per reducer: ____________________

Cost control measures:
- Output compression: ____________________
- Number of reducers: ____________________
- Input sampling strategy: ____________________
