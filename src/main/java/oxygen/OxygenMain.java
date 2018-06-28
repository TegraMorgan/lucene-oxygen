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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import utils.CmdParser;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static oxygen.Utils.format;
import static utils.QueryToLucene.symbolRemoval;

public class OxygenMain {

    private static final String PATH_TO_JSON = "../nfL6.json";
    private static final String PATH_TO_INDEX1 = "./indexes/index";
    private static final String PATH_TO_INDEX2 = "./indexes/shingle_index";
    private static final String PATH_TO_QUESTIONS = "./test/finalEval.txt";
    private static final String PATH_TO_QUESTIONS_MAIN = "./test/questions.txt";
    private static final String PATH_TO_ANSWERS_OUTPUT = "./test/out/answers.json";
    private static final float DEFAULT_LAMBDA;

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
    private static float lambda;

    private static Similarity[] similarities;
    private static MultiSimilarity similarity;

    static {

        DEFAULT_LAMBDA = 0.8888889f;
        lambda = DEFAULT_LAMBDA;
        similarities = new Similarity[]{new OxygenCustomSimilarity(), new LMJelinekMercerSimilarity(lambda)};
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
        try {
            if (parser.hasCreateQuiestionsOption()) {
                createQuestionsJson(PATH_TO_JSON);
                //System.exit(0);
            }
            if (parser.hasCreateLambdaOption()) {
                lambda = calculateLambda();
            }
        } catch (Exception e) {
            System.out.println("Exception caught:" + e.getMessage());
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

                File file = new File(PATH_TO_ANSWERS_OUTPUT);
                file.createNewFile();
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write("[    \n");

                for (String line; (line = br.readLine()) != null; ) {

                    System.out.println("The line that was read from file: " + line);

                    String[] parts = line.split("\\s+");

                    String queryId = new String(parts[0]);

                    List<String> list = new ArrayList(Arrays.asList(parts));

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

                    QuestionAnswered q = new QuestionAnswered(queryId, answers);
                    allQuestionsAnswered.add(q);
                    br.mark(3000);
                    if (br.readLine() != null) {
                        appendCommaToJson(q, fileWriter);
                    }
                    br.reset();

                }
                fileWriter.write("\n]\n");
                fileWriter.flush();
                fileWriter.close();
            }
        }
    }

    /**
     * Creates index writer with provided analyzer and similarity
     *
     * @param analyzer   Analyzer used
     * @param similarity Similarity used
     * @return new IndexWriterConfing
     */
    private static IndexWriterConfig newIndexWriterConfig(Analyzer analyzer, Similarity similarity) {
        return new IndexWriterConfig(analyzer)
                .setSimilarity(similarity)
                .setOpenMode(OpenMode.CREATE)
                .setCodec(new SimpleTextCodec())
                .setCommitOnClose(true);
    }

    /**
     * Writes the corpus to given directory from given json file using given analyzer and similarity
     * @param dir
     * @param jsonPath
     * @param analyzer
     * @param similarity
     */
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

        final TopDocs topDocs = searcher.search(q, 5);
        endSearch = System.currentTimeMillis();

        overallTime += endSearch - startSearch;

        if (topDocs.scoreDocs.length > 0) {
            //printSearchResults(topDocs, q, reader, searcher, endSearch - startSearch, overallTime);
            List<Answer> answers = createAnswersArray(searcher, q, topDocs);
            return answers;
        } else {
            System.out.printf("Search: %s not succeeded.\n", OxygenAnalyzerBase.getShingleInfo());
            throw new OxygenNotFound();
        }
    }

    private static List<Answer> createAnswersArray(IndexSearcher searcher, Query q, TopDocs topDocs)
            throws IOException {

        List<Answer> list = new ArrayList<>();

        for (final ScoreDoc sd : topDocs.scoreDocs) {
            list.add(new Answer(searcher.doc(sd.doc).get(BODY_FIELD), new Float(sd.score).toString()));
        }
        return list;
    }


    /**
     * Appends comma to Json file
     *
     * @param q
     * @param writer
     */
    private static void appendCommaToJson(QuestionAnswered q, FileWriter writer) {

        try {
            writer.write(q.toString());
            writer.write(",");
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createQuestionsJson(String filename) throws IOException {

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(filename));
        List<OriginalQuiestionObject> data = gson.fromJson(reader, new TypeToken<List<OriginalQuiestionObject>>() {
        }.getType()); // contains the whole reviews list

        try {
            File file = new File(PATH_TO_QUESTIONS);
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            for (OriginalQuiestionObject q : data) {
                fileWriter.write(q.id + "\t" + q.question + "\n");
            }
            fileWriter.flush();
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static float calculateLambda() {

        try (BufferedReader br = new BufferedReader(new FileReader(PATH_TO_QUESTIONS_MAIN))) {

            List<Integer> sizes = new ArrayList<>();

            for (String line; (line = br.readLine()) != null; ) {
                String[] parts = line.split("\\s+");
                sizes.add(parts.length - 1);
            }

            Collections.sort(sizes);

            if (sizes.size() % 2 == 1) {
                lambda = 1.0f - (1.0f / sizes.get(sizes.size() / 2));
            } else {
                lambda = 1.0f - (1.0f / ((sizes.get(sizes.size() / 2 - 1) + sizes.get(sizes.size() / 2)) / 2));
            }

        } catch (Exception e) {
            System.out.println("Cannot open " + PATH_TO_QUESTIONS + ". " + e.getMessage());
        }

        System.out.println("Threshold is: " + lambda);

        return lambda;
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
}
