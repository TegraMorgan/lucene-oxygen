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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.util.List;

import static com.shaie.irclass.Utils.format;

public class PassageSearchDemo {

    private static final String BODY_FIELD = "body";

    private static final FieldType TERM_VECTOR_TYPE;
    private static final String[][] DATA = new String[][]{
            new String[]{"doc0", "As one critic put it, \"this is a satire whose aim is so unsure that\n" +
                    "its principal casuality is itself.\" That may well be true, but it does\n" +
                    "bring out one good point: throughout the entire movie, Jimmy Hollywood\n" +
                    "is pursued relentlessly by the cops even though he is doing good for\n" +
                    "the entire community. As he points out, instead of pursuing drug\n" +
                    "dealers and thieves out on the streets, they choose to persecute\n" +
                    "someone who is actually doing good. This reflects my mentality towards\n" +
                    "cops in general who intimidate the average citizen for whatever cheap\n" +
                    "thrills they can get instead of being fair about it. Regardless of\n" +
                    "what the law is, cops behave as though you are guilty until proven\n" +
                    "innocent (even though it is never explicitly stated, they surely imply\n" +
                    "it). I say this also because of a recent experience a friend of mine\n" +
                    "had."},
            new String[]{"doc1", "Last weekend, I went to see Jimmy Hollywood starring Joe Pesci (Jimmy\n" +
                    "Alto) and Christian Slater (William). It was quite funny, though I'd\n" +
                    "wait for it to come on video. Jimmy Alto is this aspiring actor\n" +
                    "who befriends William who has lost his memory. Following the loss of\n" +
                    "his car radio, Jimmy decides to \"act\" a vigilante (he calls it his\n" +
                    "greatest role ever) in order to \"rescue\" Hollywood from what it has\n" +
                    "degenerated to."}
    };

    static {
        TERM_VECTOR_TYPE = new FieldType(TextField.TYPE_STORED);
        TERM_VECTOR_TYPE.setStoreTermVectors(true);
        TERM_VECTOR_TYPE.setStoreTermVectorPositions(true);
        TERM_VECTOR_TYPE.setStoreTermVectorOffsets(true);
        TERM_VECTOR_TYPE.freeze();
    }

    public static void main(String[] args) throws Exception {
        try (Directory dir = newDirectory();
             Analyzer analyzer = newAnalyzer()) {
            // Index
            try (IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig(analyzer))) {
                for (final String[] docData : DATA) {
                    final Document doc = new Document();
                    doc.add(new StringField("id", docData[0], Store.YES));
                    doc.add(new Field(BODY_FIELD, docData[1], TERM_VECTOR_TYPE));
                    writer.addDocument(doc);
                }
            }

            // Search
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                final QueryParser qp = new QueryParser(BODY_FIELD, analyzer);
                final Query q = qp.parse("jimmy hollywood");

                final IndexSearcher searcher = new IndexSearcher(reader);
                final TopDocs td = searcher.search(q, 10);
                // final PassageSearcher passageSearcher =
                // new SnippetsPassageSearcher(searcher, BODY_FIELD, 2, PassageScorer.DOC_SCORE_AND_QUERY_TF);
                final PassageSearcher passageSearcher =
                        new TermVectorsPassageSearcher(searcher, BODY_FIELD, 0.1, PassageScorer.DOC_SCORE_AND_QUERY_TF);

                final List<Passage> passages = passageSearcher.search(q, td, 3, 50);
                for (final Passage passage : passages) {
                    System.out.println(
                            format("doc=%s, doc_score=%.4f, psg_score=%.4f, query_terms=%s, offsets=(%d,%d)\n%s\n",
                                    passage.getDocID(), passage.getDocScore(), passage.getScore(),
                                    passage.getQueryTerms(), passage.getStartOffset(), passage.getEndOffset(),
                                    passage.getText()));
                }
            }
        }

    }

    private static Directory newDirectory() {
        return new RAMDirectory();
    }

    private static Analyzer newAnalyzer() {
        return new EnglishAnalyzer();
    }

    private static IndexWriterConfig newIndexWriterConfig(Analyzer analyzer) {
        return new IndexWriterConfig(analyzer)
                .setOpenMode(OpenMode.CREATE)
                .setCommitOnClose(true);
    }

}
