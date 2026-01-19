#!/usr/bin/env python3
import argparse
import os
import math
import csv
from collections import defaultdict

import matplotlib.pyplot as plt

def plot_score_histogram_pos_neg(rows, out_png, title):
    """
    rows: list of tuples (label, p1, p2, score)
    Produces the 'pos vs neg' overlay histogram.
    """
    import matplotlib.pyplot as plt

    pos = [score for (label, _, __, score) in rows if label == "pos"]
    neg = [score for (label, _, __, score) in rows if label == "neg"]

    all_scores = pos + neg
    if not all_scores:
        return

    bins = 60

    plt.figure()
    plt.hist(pos, bins=bins, alpha=0.6, label="pos")
    plt.hist(neg, bins=bins, alpha=0.6, label="neg")
    plt.title(title)
    plt.xlabel("Similarity score")
    plt.ylabel("Count")
    plt.legend()
    plt.yscale("log")
    plt.tight_layout()
    plt.savefig(out_png, dpi=200)
    plt.close()


def read_scores_tsv(path):
    """
    Expected TSV header: label\tp1\tp2\tscore
    label in {pos,neg} (case-insensitive).
    """
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        rdr = csv.reader(f, delimiter="\t")
        header = next(rdr, None)
        if header is None:
            raise ValueError("Empty scores file")
        # tolerate missing header by checking first cell
        if header and header[0].lower() not in ("label", "pos", "neg"):
            # header is actually a row
            # treat it as a data row and continue
            first = header
            header = ["label", "p1", "p2", "score"]
            rdr = [first] + list(rdr)

        for r in rdr:
            if not r or len(r) < 4:
                continue
            label = (r[0] or "").strip().lower()
            if label not in ("pos", "neg"):
                continue
            try:
                score = float((r[3] or "0").strip())
            except Exception:
                continue
            rows.append((label, r[1], r[2], score))
    return rows


def confusion_at_threshold(labels_scores, thr, eps=0.0):
    # predict pos if score >= thr
    tp = fp = tn = fn = 0
    for y, s in labels_scores:
        pred_pos = (s >= thr - eps)
        if y == 1 and pred_pos:
            tp += 1
        elif y == 0 and pred_pos:
            fp += 1
        elif y == 0 and not pred_pos:
            tn += 1
        elif y == 1 and not pred_pos:
            fn += 1
    return tp, fp, tn, fn


def pr_at_threshold(tp, fp, tn, fn):
    prec = tp / (tp + fp) if (tp + fp) > 0 else 1.0
    rec = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    return prec, rec


def f1(prec, rec):
    return (2 * prec * rec / (prec + rec)) if (prec + rec) > 0 else 0.0


def ensure_dir(d):
    os.makedirs(d, exist_ok=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--scores", required=True, help="TSV: label p1 p2 score")
    ap.add_argument("--tag", default="run", help="tag for filenames")
    ap.add_argument("--out_dir", default="figures", help="where to write pngs")
    ap.add_argument("--grid", type=int, default=0,
                    help="If >0, also evaluate a uniform grid of thresholds (for smoother curves)")
    ap.add_argument("--round", type=int, default=6,
                help="Round scores to N decimals before threshold sweep (reduces #unique thresholds).")
    args = ap.parse_args()

    rows = read_scores_tsv(args.scores)
    if not rows:
        raise SystemExit("No valid rows found in scores TSV")

    labels_scores = []
    for (lab, _, __, s) in rows:
        s = round(s, args.round)
        y = 1 if lab == "pos" else 0
        labels_scores.append((y, s))

    n_pos = sum(1 for y, _ in labels_scores if y == 1)
    n_neg = sum(1 for y, _ in labels_scores if y == 0)
    n_total = len(labels_scores)

    scores = [s for _, s in labels_scores]
    uniq_scores = sorted(set(scores))

    # --- Threshold candidates (LIMITED) ---
    scores_sorted = sorted(scores)
    thr_set = {0.0, 1e-9, max(scores_sorted)}

    # add quantiles to get a small, representative sweep
    qs = [0.001, 0.01, 0.05, 0.10, 0.25, 0.50, 0.75, 0.90, 0.95, 0.99, 0.999]
    n = len(scores_sorted)
    for q in qs:
        idx = int(q * (n - 1))
        thr_set.add(scores_sorted[idx])

    thresholds = sorted(thr_set)
    print("DEBUG: #thresholds_used =", len(thresholds))

    
    print("DEBUG: grid =", args.grid)
    print("DEBUG: #rows =", len(rows))
    print("DEBUG: #unique_scores =", len(uniq_scores))
    print("DEBUG: min,max =", min(scores), max(scores))
    thresholds = sorted(thr_set)
    
    # Evaluate
    f1_points = []
    pr_points = []
    best = None  # (f1, thr, tp, fp, tn, fn, prec, rec)

    for thr in thresholds:
        tp, fp, tn, fn = confusion_at_threshold(labels_scores, thr)
        prec, rec = pr_at_threshold(tp, fp, tn, fn)
        f = f1(prec, rec)
        f1_points.append((thr, f))
        pr_points.append((rec, prec))
        if best is None or f > best[0] or (math.isclose(f, best[0]) and thr < best[1]):
            best = (f, thr, tp, fp, tn, fn, prec, rec)

    ensure_dir(args.out_dir)

    # --- Plot 1: Score histogram ---
    out_hist = os.path.join(args.out_dir, f"score_hist_{args.tag}.png")
    plot_score_histogram_pos_neg(
        rows,
        out_hist,
        title=f"Score distribution ({args.tag.replace('_', ', ')})"
    )

    # --- Plot 2: F1 vs threshold ---
    plt.figure()
    xs = [t for (t, _) in f1_points]
    ys = [v for (_, v) in f1_points]
    plt.scatter(xs, ys, s=25)
    plt.plot(xs, ys, linewidth=1)
    plt.title(f"F1 vs threshold ({args.tag})")
    plt.xlabel("threshold")
    plt.ylabel("F1")
    out_f1 = os.path.join(args.out_dir, f"f1_vs_threshold_{args.tag}.png")
    plt.savefig(out_f1, dpi=200, bbox_inches="tight")
    plt.close()

    # --- Plot 3: Precision–Recall curve ---
    # Sort by recall for nicer plotting
    pr_points_sorted = sorted(pr_points, key=lambda x: x[0])
    plt.figure()
    plt.plot([r for r, _ in pr_points_sorted], [p for _, p in pr_points_sorted])
    plt.title(f"Precision–Recall curve ({args.tag})")
    plt.xlabel("recall")
    plt.ylabel("precision")
    out_pr = os.path.join(args.out_dir, f"pr_curve_{args.tag}.png")
    plt.savefig(out_pr, dpi=200, bbox_inches="tight")
    plt.close()

    # --- Summary text (for filling the report) ---
    # Diagnostic strict threshold: score > 0
    tp0, fp0, tn0, fn0 = confusion_at_threshold(labels_scores, 1e-9)
    prec0, rec0 = pr_at_threshold(tp0, fp0, tn0, fn0)
    f0 = f1(prec0, rec0)

    # Percentages
    zeros = sum(1 for _, s in labels_scores if s == 0.0)
    pos_nonzero = sum(1 for y, s in labels_scores if y == 1 and s > 0.0)
    neg_nonzero = sum(1 for y, s in labels_scores if y == 0 and s > 0.0)

    pct_zero = 100.0 * zeros / n_total
    pct_pos_nonzero = 100.0 * pos_nonzero / n_pos if n_pos else 0.0
    pct_neg_nonzero = 100.0 * neg_nonzero / n_neg if n_neg else 0.0

    out_summary = os.path.join(args.out_dir, f"summary_{args.tag}.txt")
    with open(out_summary, "w", encoding="utf-8") as f:
        f.write(f"Counts: pos={n_pos}, neg={n_neg}, total={n_total}\n")
        f.write(f"Zero scores: {zeros}/{n_total} ({pct_zero:.2f}%)\n")
        f.write(f"Pos with score>0: {pos_nonzero}/{n_pos} ({pct_pos_nonzero:.2f}%)\n")
        f.write(f"Neg with score>0: {neg_nonzero}/{n_neg} ({pct_neg_nonzero:.2f}%)\n\n")

        f_best, thr_best, tp, fp, tn, fn, prec, rec = best
        f.write("Best-by-sweep:\n")
        f.write(f"  threshold={thr_best}\n")
        f.write(f"  TP={tp}, FP={fp}, TN={tn}, FN={fn}\n")
        f.write(f"  Precision={prec:.6f}, Recall={rec:.6f}, F1={f_best:.6f}\n\n")

        f.write("Diagnostic threshold (score>0):\n")
        f.write("  threshold=1e-9\n")
        f.write(f"  TP={tp0}, FP={fp0}, TN={tn0}, FN={fn0}\n")
        f.write(f"  Precision={prec0:.6f}, Recall={rec0:.6f}, F1={f0:.6f}\n")

    print("Wrote:")
    print(" ", out_hist)
    print(" ", out_f1)
    print(" ", out_pr)
    print(" ", out_summary)
    print("\nOpen summary file and copy values into your report.")


if __name__ == "__main__":
    main()
