#!/usr/bin/env python3
"""
make_images_100.py
Generate report figures from out_scores.tsv produced by EvalPairs.

Usage:
  python3 make_images_100.py out_scores.tsv figures_100
"""

import sys
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 make_images_100.py <out_scores.tsv> <out_dir>")
        sys.exit(1)

    scores_path = Path(sys.argv[1])
    out_dir = Path(sys.argv[2])
    out_dir.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(scores_path, sep="\t")
    df["score"] = pd.to_numeric(df["score"], errors="coerce").fillna(0.0)
    df["y"] = (df["label"].astype(str).str.lower() == "pos").astype(int)

    P = int(df["y"].sum())
    N = int(len(df) - P)

    s = df["score"].values
    y = df["y"].values

    # Ranking curves
    order = np.argsort(-s)
    y_sorted = y[order]
    tps = np.cumsum(y_sorted)
    fps = np.cumsum(1 - y_sorted)

    prec = tps / (tps + fps)
    rec = tps / P if P > 0 else np.zeros_like(tps, dtype=float)

    tpr = tps / P if P > 0 else np.zeros_like(tps, dtype=float)
    fpr = fps / N if N > 0 else np.zeros_like(fps, dtype=float)

    roc_auc = float(np.trapz(tpr, fpr))
    ap = float((prec[y_sorted == 1].sum() / P) if P > 0 else 0.0)

    # 1) Score distribution by label
    uniq_scores = np.sort(df["score"].unique())
    pos_counts = [(df[(df["score"] == v) & (df["y"] == 1)].shape[0]) for v in uniq_scores]
    neg_counts = [(df[(df["score"] == v) & (df["y"] == 0)].shape[0]) for v in uniq_scores]

    x = np.arange(len(uniq_scores))
    plt.figure()
    plt.bar(x - 0.2, pos_counts, width=0.4, label="pos")
    plt.bar(x + 0.2, neg_counts, width=0.4, label="neg")
    plt.xticks(x, [f"{v:.3g}" for v in uniq_scores], rotation=45, ha="right")
    plt.ylabel("count")
    plt.xlabel("score")
    plt.title("Score distribution (100-file run)")
    plt.legend()
    plt.tight_layout()
    plt.savefig(out_dir / "score_distribution_100.png", dpi=200)
    plt.close()

    # 2) Non-zero scores by label
    nonzero_pos = int(((df["score"] > 0) & (df["y"] == 1)).sum())
    nonzero_neg = int(((df["score"] > 0) & (df["y"] == 0)).sum())

    plt.figure()
    plt.bar(["pos", "neg"], [nonzero_pos, nonzero_neg])
    plt.ylabel("count with score>0")
    plt.title("Pairs with non-zero similarity score")
    plt.tight_layout()
    plt.savefig(out_dir / "nonzero_by_label_100.png", dpi=200)
    plt.close()

    # 3) Precision-Recall curve
    plt.figure()
    plt.plot(rec, prec)
    plt.xlabel("recall")
    plt.ylabel("precision")
    plt.title(f"Precision-Recall (AP={ap:.3f})")
    plt.tight_layout()
    plt.savefig(out_dir / "pr_curve_100.png", dpi=200)
    plt.close()

    # 4) ROC curve
    plt.figure()
    plt.plot(fpr, tpr)
    plt.plot([0, 1], [0, 1], linestyle="--")
    plt.xlabel("false positive rate")
    plt.ylabel("true positive rate")
    plt.title(f"ROC (AUC={roc_auc:.3f})")
    plt.tight_layout()
    plt.savefig(out_dir / "roc_curve_100.png", dpi=200)
    plt.close()

    print("Wrote:")
    for p in sorted(out_dir.glob("*.png")):
        print(" -", p)

if __name__ == "__main__":
    main()
