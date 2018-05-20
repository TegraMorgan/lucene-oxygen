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
package oxygen;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import org.apache.lucene.analysis.Analyzer;


import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.*;


import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static oxygen.Utils.format;

import utils.CmdParser;

public class HandsOnDemo {

    private static final String PATH_TO_JSON = "../nfL6.json";
    private static final String PATH_TO_INDEX = "./tmp/ir-class/demo";
    private static final String BODY_FIELD = "body";
    private static final FieldType termVector_t;
    private static long startTime;
    private static long beginQuery;
    private static long endQuery;
    private static long endRun;

    private static IndexWriter indexWriter = null;

    private static Similarity[] similarities;
    private static MultiSimilarity similarity;

    static {
        //TODO check
        //similarities = new Similarity[] {new BooleanSimilarity()};
        //similarities = new Similarity[] {new OxygenCustomSimilarity()};
        //similarities = new Similarity[] {new LMJelinekMercerSimilarity(0.7f)};

        //similarities = new Similarity[] {new BooleanSimilarity(), new OxygenCustomSimilarity()};
        //similarities = new Similarity[] {new BooleanSimilarity(), new LMJelinekMercerSimilarity(0.7f)};

        similarities = new Similarity[]{new OxygenCustomSimilarity(), new LMJelinekMercerSimilarity(0.7f)};


        //similarities = new Similarity[] {new BooleanSimilarity(), new OxygenCustomSimilarity(), new LMJelinekMercerSimilarity(0.7f)};

        similarity = new MultiSimilarity(similarities);

        termVector_t = new FieldType(TextField.TYPE_STORED);
        termVector_t.setStoreTermVectors(true);
        termVector_t.setStoreTermVectorPositions(true);
        termVector_t.setStoreTermVectorOffsets(true);
        termVector_t.freeze();
    }

    public static void main(String[] args) throws Exception {

        startTime = System.currentTimeMillis();

        CmdParser parser = new CmdParser();
        try {
            parser.extract(args);
        } catch (Exception e) {
            System.exit(1);
        }

        try (Directory dir = newDirectory(); Analyzer analyzer = newAnalyzer()) {

            if (parser.hasIndexingOption()) {
                indexCorpus(dir, PATH_TO_JSON, analyzer, similarity);
            }
            beginQuery = System.currentTimeMillis();
            System.out.printf("Index ready.\nTime elapsed : %d seconds\n", (beginQuery - startTime) / 1000);
            // Search
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                //logIndexInfo(reader);

                String queryString = "us US U.S.A U.S. u.s";      // String to search
                queryString = OxygenCustomAnalyzer.symbolRemoval(queryString);      // Making string lucene friendly
                final QueryParser qp = new QueryParser(BODY_FIELD, analyzer);       // Basic Query Parser creates
                final Query q = qp.parse(queryString);                              // Boolean Query

                // PhraseQuery should be added perhaps?
                /* Viable classes are as follows:

                PhraseQuery
                TermQuery
                BooleanQuery

                */
                endQuery = System.currentTimeMillis();
                System.out.println("Query: " + q);
                System.out.println();
                System.out.printf("Time elapsed : %d seconds\n", (endQuery - startTime) / 1000);

                final IndexSearcher searcher = new IndexSearcher(reader);
                /* There is also a PassageSearcher */
                searcher.setSimilarity(similarity);
                final TopDocs td = searcher.search(q, 10);

                final FastVectorHighlighter highlighter = new FastVectorHighlighter();
                final FieldQuery fieldQuery = highlighter.getFieldQuery(q, reader);
                for (final ScoreDoc sd : td.scoreDocs) {
                    final String[] snippets =
                            highlighter.getBestFragments(fieldQuery, reader, sd.doc, BODY_FIELD, 100, 3);
                    final Document doc = searcher.doc(sd.doc);
                    System.out.println(format("doc=%d, score=%.4f, text=%s snippet=%s", sd.doc, sd.score,
                            doc.get(BODY_FIELD), Arrays.stream(snippets).collect(Collectors.joining(" "))));
                }
            }
        }
        endRun = System.currentTimeMillis();
        System.out.printf("Time elapsed : %d seconds\n", (endRun - startTime) / 1000);
    }

    private static Directory newDirectory() throws IOException {
        return FSDirectory.open(new File(PATH_TO_INDEX).toPath());
    }

    private static Analyzer newAnalyzer() {
        return new oxygen.OxygenCustomAnalyzer();
    }

    private static IndexWriterConfig newIndexWriterConfig(Analyzer analyzer, Similarity similarity) {
        return new IndexWriterConfig(analyzer)
                .setSimilarity(similarity)
                .setOpenMode(OpenMode.CREATE)
                .setCodec(new SimpleTextCodec())
                .setCommitOnClose(true);
    }

    private static void indexCorpus(Directory dir, String jsonPath, Analyzer analyzer, Similarity similarity) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(jsonPath));
            List<Question> questions = Arrays.asList(new Gson().fromJson(br, Question[].class));

            List<String> allAnswers = new ArrayList<String>();
            for (Integer i = 0; i < questions.size(); ++i) {
                allAnswers.addAll(questions.get(i).nbestanswers);
            }

            // Index
            try (IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig(analyzer, similarity))) {
                for (Integer i = 0; i < allAnswers.size(); ++i) {
                    final Document doc = new Document();
                    doc.add(new StringField("id", i.toString(), Store.YES));
                    doc.add(new TextField(BODY_FIELD, allAnswers.get(i), Store.YES));
                    // doc.add(new Field(BODY_FIELD, docData[1], termVector_t));gin dev-Tegra
                    writer.addDocument(doc);
                }
            }
            //printCorpus(allAnswers);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private static void printCorpus(List<String> corpus) {
        for (String answer : corpus) {
            System.out.println(answer);
        }
    }

    @SuppressWarnings("unused")
    private static void logIndexInfo(IndexReader reader) throws IOException {
        System.out.println("Index info:");
        System.out.println("----------");
        System.out.println("Docs: " + reader.numDocs());
        final Fields fields = MultiFields.getFields(reader);
        System.out.println("Fields: " + Iterables.toString(fields));
        System.out.println("Terms:");
        for (final String field : fields) {
            final Terms terms = MultiFields.getTerms(reader, field);
            System.out.println(format("  %s (sumTTF=%d sumDF=%d)", field, terms.getSumTotalTermFreq(),
                    terms.getSumDocFreq()));
            final TermsEnum termsEnum = terms.iterator();
            final StringBuilder sb = new StringBuilder();
            while (termsEnum.next() != null) {
                sb.append("    ").append(termsEnum.term().utf8ToString());
                sb.append(" (").append(termsEnum.docFreq()).append(")");
                sb.append("  docs=");
                final PostingsEnum postings = termsEnum.postings(null, PostingsEnum.ALL);
                while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    sb.append("{id=").append(postings.docID()).append(',');
                    sb.append("freq=").append(postings.freq());
                    sb.append(",pos=[");
                    for (int i = 0; i < postings.freq(); i++) {
                        final int pos = postings.nextPosition();
                        sb.append(pos).append(',');
                    }
                    sb.append(']');
                    sb.append("} ");
                }
                System.out.println(sb);
                sb.setLength(0);
            }
        }
        System.out.println();
    }
}
