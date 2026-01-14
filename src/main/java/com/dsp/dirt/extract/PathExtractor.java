package com.dsp.dirt.extract;

import com.dsp.dirt.parse.BiarcsRecord;

import java.util.ArrayList;
import java.util.List;

import static com.dsp.dirt.util.Keys.X;
import static com.dsp.dirt.util.Keys.Y;

/**
 * Core logic:
 * - build dependency graph from record
 * - enumerate candidate X-Y pairs
 * - extract the dependency path between them
 * - apply constraints:
 *   * head is verb
 *   * X/Y fillers are nouns
 *   * include prepositions in path
 *   * filter auxiliaries
 *   * stem/normalize
 */
public class PathExtractor {

    private final Stemmer stemmer = new Stemmer();

    public List<PathInstance> extract(BiarcsRecord r) {
        List<PathInstance> out = new ArrayList<>();

        // TODO:
        // 1) From r, build dependency structure
        // 2) Choose endpoints (wX noun, wY noun)
        // 3) Compute pathKey (canonical string)
        // 4) out.add(new PathInstance(pathKey, X, stemmer.stem(wX), r.count));
        // 5) out.add(new PathInstance(pathKey, Y, stemmer.stem(wY), r.count));

        return out;
    }

    // TODO: helpers: isAuxVerb(token), isNoun(pos), isVerb(pos), canonicalizePath(...)
}
