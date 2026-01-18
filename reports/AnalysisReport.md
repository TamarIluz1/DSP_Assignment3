# Assignment 3 — DIRT: Extraction of Lexico-syntactic Similarities

## Analysis Report

## 1. Experimental Setup

### 1.1 Runs
We ran the system on:
- **Small**: 10 Biarcs files  
- **Medium**: 20 Biarcs files (10 original + 10 additional: 14,27,34,46,50,52,67,73,85,95)  
- **Large (placeholder)**: 100 files (budget permitting)

### 1.2 Outputs
For each run we produce:
- `mi.tsv` — MI feature table
- `out_scores.tsv` — similarity per test pair (`label, p1, p2, score`)

### 1.3 Evaluation Protocol
Each row in `out_scores.tsv` is labeled:
- `pos` = entailment (positive pair)
- `neg` = non-entailment (negative pair)

We define a classifier by thresholding:
- predict **pos** if `score >= threshold`, else **neg**

We report:
- Precision, Recall, F1
- Precision–Recall curve
- Error analysis examples (TP/FP/TN/FN)

---

## 2. Small Run (10 Files)

### 2.1 Basic Counts (Fill)
- Positive pairs: ________  
- Negative pairs: ________  
- Total pairs: ________  

(If using your known run, you can fill: Pos=2481, Neg=99, Total=2580.)

### 2.2 Score Distribution (Describe + Figure)
Observations (fill):
- % of scores that are 0.0: ________
- % of positive pairs with score > 0: ________
- % of negative pairs with score > 0: ________

Insert figure:
- `figures/score_hist_small_10.png`

```md
![Score histogram (Small – 10 files)](figures/score_hist_small_10.png)




### 2.3 Choosing a Threshold and F1

We convert similarity scores into a binary decision using a threshold:

* Predict POSITIVE if score >= threshold
* Predict NEGATIVE otherwise

Because the small corpus (10 files) often produces many zero scores, the “best” threshold by F1 may be dominated by class imbalance. Therefore, we report two evaluations:
(1) Best threshold according to a threshold sweep (maximize F1).
(2) A diagnostic threshold slightly above 0 (treat score=0 as negative), which exposes sparsity-driven false negatives.


#### 2.3.1 Best Threshold by F1 Sweep (Small – 10 Files)

FILL:

* Best threshold (max F1) = __________
* Confusion matrix at this threshold:

  * TP = __________
  * FP = __________
  * TN = __________
  * FN = __________
* Metrics:

  * Precision = __________
  * Recall = __________
  * F1 = __________

Interpretation:

* If the sweep selects threshold = 0.0, it means predicting “positive” for all pairs (or almost all pairs). This can yield a high F1 if the dataset is heavily imbalanced toward positives, but it does not reflect real separation between positive and negative pairs.


#### 2.3.2 Diagnostic Threshold: Score Must Be > 0 (Small – 10 Files)

We also evaluate a strict threshold:

* threshold = 1e-9 (equivalent to “score > 0”)

FILL:

* Confusion matrix at threshold 1e-9:

  * TP = __________
  * FP = __________
  * TN = __________
  * FN = __________
* Metrics:

  * Precision = __________
  * Recall = __________
  * F1 = __________

Interpretation:

* With 10 files, many predicates or feature overlaps are missing, so most similarities are exactly 0. Under threshold > 0, recall often becomes very low because most positive pairs remain at score 0 and are classified as negative (false negatives).
* On larger input sizes (20 files, 100 files), we expect more overlaps and therefore a higher recall at meaningful thresholds, but also potentially more false positives due to increased chance of spurious overlap.

(If you include graphs, cite them here:)

* F1 vs threshold (Small – 10 files): figures/f1_vs_threshold_small_10.png

---


# 3. Precision–Recall Curve

We evaluate the ranking quality by sweeping the decision threshold over observed similarity scores and computing precision and recall at each threshold. This yields a Precision–Recall (PR) curve, where:

* Recall is on the x-axis
* Precision is on the y-axis


## 3.1 PR Curve (Small – 10 Files)

Observed behavior in sparse runs (10 files):

* At threshold = 0.0, recall is typically near 1.0 because almost all pairs are predicted positive. Precision at this point equals the base rate of positives in the dataset.
* For any threshold > 0, the number of predicted positives can drop sharply; precision may increase while recall collapses.

FILL (optional summary numbers from your PR computation):

* Number of distinct thresholds evaluated = __________
* Average precision (optional) = __________

(If you include graphs, cite them here:)

* Precision–Recall curve (Small – 10 files): figures/pr_curve_small_10.png


## 3.2 PR Curve Placeholders for 20 Files and 100 Files

20 files (placeholder):

* Expectation: more non-zero similarities, smoother PR curve, and less “all-or-nothing” behavior.
  FILL LATER:
* PR curve path: figures/pr_curve_20.png
* Any key observations: ____________________________

100 files (placeholder):

* Expectation: further reduction in sparsity and improved separation, but potentially more false positives due to broader feature overlap.
  FILL LATER:
* PR curve path: figures/pr_curve_100.png
* Any key observations: ____________________________

---


# 4. Error Analysis

We categorize test pairs into TP/FP/TN/FN using a selected threshold. Because threshold=0.0 often produces no true negatives or false negatives in sparse settings, we use the diagnostic threshold (score > 0) for meaningful error categories.


## 4.1 Threshold Used for Error Categorization

We use:

* threshold = 1e-9 (i.e., score must be strictly greater than 0)

Rationale:

* With threshold=0.0, many runs classify nearly all pairs as positive, eliminating TN/FN and preventing informative error analysis.
* With threshold > 0, we can inspect which pairs achieve non-zero similarity and which remain at 0.


## 4.2 Category Counts (Small – 10 Files)

FILL:

* TP count = __________
* FP count = __________
* TN count = __________
* FN count = __________


## 4.3 Examples (Small – 10 Files)

Instructions:

* Provide 5 examples for each category (TP, FP, TN, FN).
* For each example, include: label, predicate1, predicate2, similarity score.


### 4.3.1 True Positives (5 Examples)
FILL:

1. (pos) p1 = __________ ; p2 = __________ ; score = __________
2. (pos) p1 = __________ ; p2 = __________ ; score = __________
3. (pos) p1 = __________ ; p2 = __________ ; score = __________
4. (pos) p1 = __________ ; p2 = __________ ; score = __________
5. (pos) p1 = __________ ; p2 = __________ ; score = __________


### 4.3.2 False Positives (5 Examples)
Note: In very sparse 10-file runs, it is common to have zero or very few false positives because negatives may all score 0. If FP count is 0, state that explicitly and add a note that this may change in larger runs.

FILL:

* FP count observed = __________
  If FP count > 0:

1. (neg) p1 = __________ ; p2 = __________ ; score = __________
2. (neg) p1 = __________ ; p2 = __________ ; score = __________
3. (neg) p1 = __________ ; p2 = __________ ; score = __________
4. (neg) p1 = __________ ; p2 = __________ ; score = __________
5. (neg) p1 = __________ ; p2 = __________ ; score = __________
   If FP count = 0:

* No false positives observed at threshold > 0 in the 10-file run.


### 4.3.3 True Negatives (5 Examples)
FILL:

1. (neg) p1 = __________ ; p2 = __________ ; score = __________
2. (neg) p1 = __________ ; p2 = __________ ; score = __________
3. (neg) p1 = __________ ; p2 = __________ ; score = __________
4. (neg) p1 = __________ ; p2 = __________ ; score = __________
5. (neg) p1 = __________ ; p2 = __________ ; score = __________


### 4.3.4 False Negatives (5 Examples)
These are positive-labeled pairs that got score 0 (or below threshold).

FILL:

1. (pos) p1 = __________ ; p2 = __________ ; score = __________
2. (pos) p1 = __________ ; p2 = __________ ; score = __________
3. (pos) p1 = __________ ; p2 = __________ ; score = __________
4. (pos) p1 = __________ ; p2 = __________ ; score = __________
5. (pos) p1 = __________ ; p2 = __________ ; score = __________


## 4.4 Comparing Small vs Larger Inputs (Placeholders)

Pick a few of the above examples (at least one from each category if available) and compare the similarity scores between:

* Small (10 files)
* Medium (20 files)
* Large (100 files)

FILL TABLE-LIKE TEXT:
Example A: p1=__________ p2=__________

* score(10) = ________
* score(20) = ________
* score(100) = ________
  Observation: ____________________________

Example B: p1=__________ p2=__________

* score(10) = ________
* score(20) = ________
* score(100) = ________
  Observation: ____________________________


## 4.5 Common Errors and Behaviors (Small – 10 Files)

Typical causes of errors in small corpora:

* Missing predicate coverage: one or both predicates in a test pair do not appear in the corpus (feature vector missing).
* Feature sparsity: predicate exists but has very few high-MI fillers; overlap with the other predicate’s fillers is empty, producing score 0.
* POS/path constraints: enforcing verb head and noun slots can remove many candidate paths, reducing recall on small data.
* Auxiliary filtering: removing auxiliary verbs improves quality but can eliminate frequent shallow patterns; in small data this can further reduce coverage.

Expected change in larger runs:

* More predicates and fillers appear, increasing overlap, raising recall at meaningful thresholds.
* Increased overlap can also create more false positives when unrelated predicates share frequent fillers (especially if MI filtering is not strong enough).

---


# 5. Large Run Sections (Placeholders: 20 Files and 100 Files)


## 5.1 Medium Run (20 Files: 10 Original + Selected Indices)
Input:

* 10 original files from bucket dsp-ass3-first10-biarcs
* Additional 10 files: indices 14, 27, 34, 46, 50, 52, 67, 73, 85, 95

FILL LATER:

* Total pairs scored = __________
* Best threshold (max F1) = __________
* F1 at best threshold = __________
* PR curve path: figures/pr_curve_20.png
* Short observation: ____________________________


## 5.2 Large Run (100 Files) Placeholder
FILL LATER:

* Input size = __________ (if less than 100 due to budget, state exact number)
* Best threshold (max F1) = __________
* F1 at best threshold = __________
* PR curve path: figures/pr_curve_100.png
* Short observation: ____________________________

---


# 6. Summary and Next Steps


## 6.1 Summary of Small Run (10 Files)

FILL:

* Best-F1 threshold (from sweep): __________
* F1 at best threshold: __________
* Diagnostic threshold (score > 0) F1: __________
* Key behavior: scores are (sparse / moderately spread / well separated) = __________


## 6.2 What to Do Next (Practical Checklist)

1. Generate the 20-file run outputs:

* Run MapReduce on the 20 selected files.
* Merge reducer outputs to produce mi.tsv (20).
* Run local EvalPairs to produce out_scores.tsv (20).

2. Recompute evaluation for 20 files:

* Compute F1 vs threshold, pick best threshold.
* Produce PR curve.
* Extract 5 examples each for TP/FP/TN/FN and compare to the 10-file run.

3. If budget allows, do the 100-file run:

* Repeat exactly the same process.
* Discuss how the results changed (coverage, sparsity, errors, threshold stability).


## 6.3 Expected Differences Between 10, 20, and 100 Files

* Coverage increases with corpus size: more predicates from the test set appear in the corpus.
* Score distribution becomes less degenerate: more non-zero similarities.
* Threshold selection becomes more meaningful: F1 peak moves away from 0.0 and better reflects class separation.
* False positives may increase: more opportunities for unrelated predicates to share fillers (especially frequent entities), so PR curve can reveal trade-offs.

