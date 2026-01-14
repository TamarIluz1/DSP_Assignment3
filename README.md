# Assignment 3 — DIRT Skeleton (Hadoop/Java)

This is a **skeleton** project for Assignment 3 (DIRT-style inference rule discovery).
It includes:
- MapReduce Job A: count triples and totals
- MapReduce Job B: compute MI per (p,slot,w)
- Local scorer: compute similarity for provided positive/negative pairs

## Files you must implement
- `src/main/java/com/dsp/dirt/parse/BiarcsParser.java` — parse your corpus line format
- `src/main/java/com/dsp/dirt/extract/PathExtractor.java` — extract dependency paths and apply constraints
- Decide/verify MI + similarity formulas (keep consistent with your report)

## Where the test sets are
- `data/testsets/positive-preds.txt`
- `data/testsets/negative-preds.txt`

## Build
```bash
mvn -q -DskipTests package
```

Jar will be in:
`target/dirt-assignment3-1.0.0-shaded.jar`

## Run on Hadoop/EMR
```bash
hadoop jar target/dirt-assignment3-1.0.0-shaded.jar \
  com.dsp.dirt.Driver \
  s3://YOUR-BUCKET/input/biarcs/ \
  s3://YOUR-BUCKET/output/assignment3/
```

Outputs:
- `{outBase}/A_counts/`
- `{outBase}/B_mi/`

## Local scoring (after you download/merge MI output into mi.tsv)
```bash
java -cp target/dirt-assignment3-1.0.0-shaded.jar \
  com.dsp.dirt.local.EvalPairs \
  mi.tsv \
  data/testsets/positive-preds.txt \
  data/testsets/negative-preds.txt \
  scores.tsv
```
