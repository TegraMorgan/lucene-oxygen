/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required byOCP applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shaie.irclass;

import com.shaie.irclass.Passage.Interval;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SnippetsPassageSearcher implements PassageSearcher {

    private final IndexSearcher searcher;
    private final String field;
    private final int numSnippetsPerDoc;
    private final PassageScorer passageScorer;
    private final FastVectorHighlighter highlighter;

    /**
     * Sole constructor.
     *
     * @param searcher          the {@link IndexSearcher} to use for searching documents.
     * @param field             the field from which to extract candidate passages.
     * @param numSnippetsPerDoc the number of snippets to generate for each document.
     * @param passageScorer     the {@link PassageScorer} to use for scoring passages.
     */
    public SnippetsPassageSearcher(IndexSearcher searcher, String field, int numSnippetsPerDoc,
                                   PassageScorer passageScorer) {
        this.searcher = searcher;
        this.field = field;
        this.numSnippetsPerDoc = numSnippetsPerDoc;
        this.passageScorer = passageScorer;
        this.highlighter = new FastVectorHighlighter();
    }

    private static String extractHighlightedTerms(String snippet, Passage passage) {
        final StringBuilder sb = new StringBuilder();
        int idx = snippet.indexOf("<b>");
        int start = 0;
        while (idx != -1) {
            sb.append(snippet, start, idx);
            final int end = snippet.indexOf("</b>", idx + 3);
            if (end == -1) {
                throw new IllegalStateException("Unbalanced highlighting tags: " + snippet);
            }
            sb.append(snippet, idx + 3, end);
            passage.addTerm(snippet.substring(idx + 3, end), new Interval(idx + 3, end));
            idx = snippet.indexOf("<b>", end + 4);
            start = end + 4;
        }
        sb.append(snippet.substring(start));
        return sb.toString();
    }

    @SuppressWarnings("resource")
    @Override
    public List<Passage> search(Query query, TopDocs topDocs, int numPassages, int passageLength) throws IOException {
        // Generate candidate passages.
        final IndexReader reader = searcher.getIndexReader();
        final FieldQuery fieldQuery = highlighter.getFieldQuery(query, reader);
        final List<Passage> candidatePassages = new ArrayList<>();
        for (final ScoreDoc sd : topDocs.scoreDocs) {
            final Document doc = searcher.doc(sd.doc);
            final String docID = doc.get("id");
            final String text = doc.get("body");
            final String[] snippets =
                    highlighter.getBestFragments(fieldQuery, reader, sd.doc, field, passageLength, numSnippetsPerDoc);
            for (final String snippet : snippets) {
                // discount all highlighting tags
                final Passage passage = new Passage()
                        .setDocID(docID)
                        .setDocScore(sd.score)
                        .setText(snippet);
                final String cleanText = extractHighlightedTerms(snippet, passage);
                final int start = text.indexOf(cleanText);
                passage.setStartOffset(start).setEndOffset(start + cleanText.length());
                candidatePassages.add(passage);
            }
        }

        // Score and sort the passages.
        passageScorer.score(candidatePassages);
        Collections.sort(candidatePassages, PassageScorer.PASSAGE_COMPARATOR);

        // Return the top-scoring passages.
        return candidatePassages.stream().limit(numPassages).collect(Collectors.toList());
    }

}
