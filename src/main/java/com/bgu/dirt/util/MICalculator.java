package com.bgu.dirt.util;

/**
 * Computes mutual information for (path, slot, word) triples.
 * MI(p, slot, w) = log( freq(p, slot, w) / (freq(p, slot) * freq(slot, w)) )
 */
public class MICalculator {
    
    public static double computeMI(long tripleFreq, long pathSlotFreq, long slotWordFreq, long totalTriples) {
        if (tripleFreq == 0 || pathSlotFreq == 0 || slotWordFreq == 0) {
            return 0.0;
        }
        
        double p_triple = (double) tripleFreq / totalTriples;
        double p_pathSlot = (double) pathSlotFreq / totalTriples;
        double p_slotWord = (double) slotWordFreq / totalTriples;
        
        double mi = Math.log(p_triple / (p_pathSlot * p_slotWord));
        return mi;
    }
}
