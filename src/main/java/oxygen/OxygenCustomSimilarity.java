/* -------------------------------------------------------------------------- */
/* - lucene-oxygen - custom indexer and searcher                            - */
/* - Copyright (C) 2018  <https://github.com/louiscyphre>,                  - */
/* -                     <https://github.com/TegraMorgan>,                  - */
/* -                     University of Haifa                                - */
/* -                                                                        - */
/* - This program is free software: you can redistribute it and/or modify   - */
/* - it under the terms of the GNU General Public License as published by   - */
/* - the Free Software Foundation, either version 3 of the License, or      - */
/* - (at your option) any later version.                                    - */
/* -                                                                        - */
/* - This program is distributed in the hope that it will be useful,        - */
/* - but WITHOUT ANY WARRANTY; without even the implied warranty of         - */
/* - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          - */
/* - GNU General Public License for more details.                           - */
/* -                                                                        - */
/* - You should have received a copy of the GNU General Public License      - */
/* - along with this program.  If not, see <http://www.gnu.org/licenses/>.  - */
/* -------------------------------------------------------------------------- */
/* - File:                   OxygenCustomSimilarity.java
/* - Created by:             <https://github.com/louiscyphre>               - */
/* - Creation date and time: 22:54 18.05.2018                               - */
/* -------------------------------------------------------------------------- */
package oxygen;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.BM25Similarity;

import java.io.IOException;

/**
 * Oxygen Custom Similarity.
 */
public class OxygenCustomSimilarity extends BM25Similarity {// Similarity for custom functions
    //TODO

    /**
     * Computes the normalization value for a field, given the accumulated
     * state of term processing for this field (see {@link FieldInvertState}).
     *
     * <p>Matches in longer fields are less precise, so implementations of this
     * method usually set smaller values when <code>state.getLength()</code> is large,
     * and larger values when <code>state.getLength()</code> is small.
     *
     * @param state current processing state for this field
     * @return computed norm value
     * @lucene.experimental
     */
    /*@Override
    public long computeNorm(FieldInvertState state) {
        return 0;
    }

    public SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
        BM25Similarity similarity = new BM25Similarity();
        SimWeight weight = similarity.computeWeight(boost, collectionStats, termStats);

        //TODO here
        //weight += weight of term in theme

        return weight;
    }*/

    /**
     * Creates a new {@link SimScorer} to score matching documents from a segment of the inverted index.
     *
     * @param weight  collection information from {@link #computeWeight(float, CollectionStatistics, TermStatistics...)}
     * @param context segment of the inverted index to be scored.
     * @return SloppySimScorer for scoring documents across <code>context</code>
     * @throws IOException if there is a low-level I/O error
     */
    /*@Override
    public SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
        return null;
    }*/


//    /** Collection statistics for the Oxygen model. */
//    private static class OxygenStats extends SimWeight {
//        /** BM25's idf */
//        private final Explanation idf;
//        /** The average document length. */
//        private final float avgdl;
//        /** query boost */
//        private final float boost;
//        /** weight (idf * boost) */
//        private final float weight;
//        /** field name, for pulling norms */
//        private final String field;
//        /** precomputed norm[256] with k1 * ((1 - b) + b * dl / avgdl)
//         *  for both OLD_LENGTH_TABLE and LENGTH_TABLE */
//        private final float[] oldCache, cache;
//
//        OxygenStats(String field, float boost, Explanation idf, float avgdl, float[] oldCache, float[] cache) {
//            this.field = field;
//            this.boost = boost;
//            this.idf = idf;
//            this.avgdl = avgdl;
//            this.weight = idf.getValue() * boost;
//            this.oldCache = oldCache;
//            this.cache = cache;
//        }
//
//    }

    /*private Explanation explainTFNorm(int doc, Explanation freq, OxygenCustomSimilarity.OxygenStats stats, NumericDocValues norms, float[] lengthCache) throws IOException {
        List<Explanation> subs = new ArrayList<>();
        subs.add(freq);
        subs.add(Explanation.match(k1, "parameter k1"));
        if (norms == null) {
            subs.add(Explanation.match(0, "parameter b (norms omitted for field)"));
            return Explanation.match(
                    (freq.getValue() * (k1 + 1)) / (freq.getValue() + k1),
                    "tfNorm, computed as (freq * (k1 + 1)) / (freq + k1) from:", subs);
        } else {
            byte norm;
            if (norms.advanceExact(doc)) {
                norm = (byte) norms.longValue();
            } else {
                norm = 0;
            }
            float doclen = lengthCache[norm & 0xff];
            subs.add(Explanation.match(b, "parameter b"));
            subs.add(Explanation.match(stats.avgdl, "avgFieldLength"));
            subs.add(Explanation.match(doclen, "fieldLength"));
            return Explanation.match(
                    (freq.getValue() * (k1 + 1)) / (freq.getValue() + k1 * (1 - b + b * doclen/stats.avgdl)),
                    "tfNorm, computed as (freq * (k1 + 1)) / (freq + k1 * (1 - b + b * fieldLength / avgFieldLength)) from:", subs);
        }
    }

    private Explanation explainScore(int doc, Explanation freq, OxygenCustomSimilarity.OxygenStats stats, NumericDocValues norms, float[] lengthCache) throws IOException {
        Explanation boostExpl = Explanation.match(stats.boost, "boost");
        List<Explanation> subs = new ArrayList<>();
        if (boostExpl.getValue() != 1.0f)
            subs.add(boostExpl);
        subs.add(stats.idf);
        Explanation tfNormExpl = explainTFNorm(doc, freq, stats, norms, lengthCache);
        subs.add(tfNormExpl);
        return Explanation.match(
                boostExpl.getValue() * stats.idf.getValue() * tfNormExpl.getValue(),
                "score(doc="+doc+",freq="+freq+"), product of:", subs);
    }*/
}
