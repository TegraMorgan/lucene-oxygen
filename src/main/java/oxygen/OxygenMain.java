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
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import org.apache.lucene.analysis.Analyzer;


import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.*;


import java.io.*;
import java.util.*;


import static oxygen.Utils.format;
import static utils.QueryToLucene.symbolRemoval;

import utils.CmdParser;

public class OxygenMain {

    private static final String PATH_TO_JSON = "../nfL6.json";
    private static final String PATH_TO_INDEX1 = "./indexes/index";
    private static final String PATH_TO_INDEX2 = "./indexes/shingle_index";
    private static final String PATH_TO_QUESTIONS = "./test/exampleTestQuestions.txt";
    private static final String PATH_TO_ANSWERS_OUTPUT = "./test/out/answers.json";

    private static final String BODY_FIELD = "body";
    private static final String CATEGORY_FIELD = "main_category";
    private static final FieldType termVector_t;

    private static long overallTime = 0;
    private static long startIndexing;
    private static long endIndexing;
    private static long startQueryParse;
    private static long endQueryParse;
    private static long startSearch;
    private static long endSearch;

    private static Similarity[] similarities;
    private static MultiSimilarity similarity;

    static {
        //TODO check
        //similarities = new Similarity[] {new BooleanSimilarity()};
        //similarities = new Similarity[] {new OxygenCustomSimilarity()};
        //similarities = new Similarity[] {new LMJelinekMercerSimilarity(0.7f)};
        //similarities = new Similarity[] {new BooleanSimilarity(), new OxygenCustomSimilarity()};
        //similarities = new Similarity[] {new BooleanSimilarity(), new LMJelinekMercerSimilarity(0.7f)};
        //similarities = new Similarity[] {new BooleanSimilarity(), new OxygenCustomSimilarity(), new LMJelinekMercerSimilarity(0.7f)};


        similarities = new Similarity[]{new OxygenCustomSimilarity(), new LMJelinekMercerSimilarity(0.7f)};
        similarity = new MultiSimilarity(similarities);
        termVector_t = new FieldType(TextField.TYPE_STORED);
        termVector_t.setStoreTermVectors(true);
        termVector_t.setStoreTermVectorPositions(true);
        termVector_t.setStoreTermVectorOffsets(true);
        termVector_t.freeze();
    }

    public static void main(String[] args) throws Exception {
        CmdParser parser = new CmdParser();
        try {
            parser.extract(args);
        } catch (Exception e) {
            System.exit(1);
        }
        try (Directory dirShingle = FSDirectory.open(new File(PATH_TO_INDEX1).toPath());
             Directory dirNoShingle = FSDirectory.open(new File(PATH_TO_INDEX2).toPath());
             OxygenAnalyzerWithShingles analyzerShingle = new OxygenAnalyzerWithShingles();
             OxygenAnalyzerBase analyzerNoShingle = new OxygenAnalyzerBase()) {
            if (parser.hasIndexingOption()) {
                startIndexing = System.currentTimeMillis();
                indexCorpus(dirShingle, PATH_TO_JSON, analyzerShingle, similarity);
                indexCorpus(dirNoShingle, PATH_TO_JSON, analyzerNoShingle, similarity);
                endIndexing = System.currentTimeMillis();
                overallTime += endIndexing - startIndexing;
                System.out.printf("Index ready.\nTime elapsed : %d seconds\n", (endIndexing - startIndexing) / 1000);
                // Search
            }
            try (BufferedReader br = new BufferedReader(new FileReader(PATH_TO_QUESTIONS))) {
                List<QuestionAnswered> allQuestionsAnswered = new ArrayList<>();
                for (String line; (line = br.readLine()) != null; ) {
                    String[] parts = line.split(" ");
                    String queryId = parts[0];
                    List<String> list = Arrays.asList(parts);
                    list.remove(0);
                    String queryString = String.join(" ", list);

                    queryString = symbolRemoval(queryString);      // Making string lucene friendly
                    String preFilteredQuery = OxygenPreFilter.filter(queryString, Constants.getStopWords());

                    List<Answer> answers = new ArrayList<>(0);
                    try (DirectoryReader reader = DirectoryReader.open(dirShingle)) {

                        answers = search(reader, queryString, preFilteredQuery, analyzerShingle);

                    } catch (OxygenNotFound withShinglesException) {
                        try (DirectoryReader reader = DirectoryReader.open(dirNoShingle)) {

                            answers = search(reader, queryString, preFilteredQuery, analyzerNoShingle);

                        } catch (OxygenNotFound withoutShinglesException) {

                        }
                    }
                    QuestionAnswered q = new QuestionAnswered(Long.parseLong(queryId), answers);
                    allQuestionsAnswered.add(q);
                }

            }
        }
    }

    private static Directory newDirectory() throws IOException {
        return FSDirectory.open(new File(PATH_TO_INDEX1).toPath());
    }

    private static Analyzer newAnalyzer() {
        return new OxygenAnalyzerWithShingles();
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
            List<BestAnswersCollection> questions = Arrays.asList(new Gson().fromJson(br, BestAnswersCollection[].class));

            List<String> allAnswers = new ArrayList<>();
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

    private static List<Answer> search(DirectoryReader reader, String queryString, String preFilteredQuery,
                                       OxygenAnalyzerBase analyzer) throws IOException, ParseException, OxygenNotFound {
        final QueryParser qp = new QueryParser(BODY_FIELD, analyzer);     // Basic Query Parser creates
        BooleanQuery.setMaxClauseCount(65536);
        startQueryParse = System.currentTimeMillis();
        final Query q = qp.parse(preFilteredQuery);                       // Boolean Query
        endQueryParse = System.currentTimeMillis();

        overallTime += endQueryParse - startQueryParse;

        printQueryInfo(queryString, preFilteredQuery, q, endQueryParse, startQueryParse);

        final IndexSearcher searcher = new IndexSearcher(reader);
        /* There is also a PassageSearcher */
        searcher.setSimilarity(similarity);

        startSearch = System.currentTimeMillis();

        final TopDocs topDocs = searcher.search(q, 10);
        endSearch = System.currentTimeMillis();

        overallTime += endSearch - startSearch;

        if (topDocs.scoreDocs.length > 0) {
            printSearchResults(topDocs, q, reader, searcher, endSearch - startSearch, overallTime);
            List<Answer> answers = createAnswersArray(searcher, q, topDocs);
            return answers;
        } else {
            System.out.printf("Search: %s not succeeded.\n", analyzer.getShingleInfo());
            throw new OxygenNotFound();
        }
    }

    private static List<Answer> createAnswersArray(IndexSearcher searcher, Query q, TopDocs topDocs)
            throws IOException {

        List<Answer> list = new ArrayList<>();

        for (final ScoreDoc sd : topDocs.scoreDocs) {
            list.add(new Answer(searcher.doc(sd.doc).get(BODY_FIELD), sd.score));
        }
        return list;
    }

    private static void createAnswersJson(List<QuestionAnswered> qa) throws IOException {
        //TODO to check
        JsonArray json = (JsonArray) new Gson().toJsonTree(qa, new TypeToken<List<QuestionAnswered>>(){}.getType());

        try {
            File file=new File(PATH_TO_ANSWERS_OUTPUT);
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);

            fileWriter.write(json.toString());
            fileWriter.flush();
            fileWriter.close();

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


    private static void printQueryInfo(String originalQuery, String preFilteredQuery, Query parsedQuery,
                                       long endQueryParseMillis, long startQueryParseMillis) {
        System.out.println();
        System.out.println("Original query: " + originalQuery);
        System.out.println("Pre-filtered query: " + preFilteredQuery);
        System.out.println("Indexed query: " + parsedQuery);
        System.out.println();
        System.out.printf("Query parsed.\nTime elapsed : %d seconds\n", (endQueryParseMillis - startQueryParseMillis) / 1000);
    }

    private static void printSearchResults(TopDocs td, Query q, DirectoryReader reader, IndexSearcher searcher,
                                           long searchTimeMillis, long overallTimeMillis) throws IOException {

        System.out.printf("Search finished.\nTime elapsed : %d seconds\n", searchTimeMillis / 1000);
        System.out.printf("Total time on indexing, parsing query and searching : %d seconds\n", overallTimeMillis / 1000);

        System.out.print("\nSearch results:\n");
        final FastVectorHighlighter highlighter = new FastVectorHighlighter();
        final FieldQuery fieldQuery = highlighter.getFieldQuery(q, reader);

        for (final ScoreDoc sd : td.scoreDocs) {
            final String[] snippets =
                    highlighter.getBestFragments(fieldQuery, reader, sd.doc, BODY_FIELD, 100, 3);
            final Document doc = searcher.doc(sd.doc);
            System.out.println(format("doc=%d, score=%.4f, text=%s", sd.doc, sd.score, doc.get(BODY_FIELD)));
        }
        System.out.println();
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
