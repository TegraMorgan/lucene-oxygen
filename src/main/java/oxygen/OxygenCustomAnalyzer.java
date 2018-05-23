package oxygen;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.util.Arrays;
import java.util.List;

public class OxygenCustomAnalyzer extends StopwordAnalyzerBase {
    private final CharArraySet stemExclusionSet;
    public static final CharArraySet OXYGEN_STOP_SET;
    public static final CharArraySet OXYGEN_EXCLUSION_SET;
    protected final CharArraySet stopwords;


    public OxygenCustomAnalyzer() {
        this(getDefaultStopSet());
    }


    //Abbigious abbr. removed
    //TODO special quotation filter: (Eddisson) etc.

    static {
        final List<String> stopWords = Arrays.asList(
                "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at", "be",
                "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't", "cannot", "could",
                "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for",
                "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's",
                "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm",
                "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't",
                "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "out",
                "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
                "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these",
                "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until",
                "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when",
                "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't",
                "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"
        );
        final CharArraySet stopSet = new CharArraySet(stopWords, false);
        OXYGEN_STOP_SET = CharArraySet.unmodifiableSet(stopSet);
    }
// US
    static {
        final List<String> exclusionSet = Arrays.asList(
                "u.s.a", "u.s.a.", "u.s", "u.s."
        );
        final CharArraySet stopSet = new CharArraySet(exclusionSet, false);
        OXYGEN_EXCLUSION_SET = CharArraySet.unmodifiableSet(stopSet);
    }

    /**
     * Builds an analyzer with the given stop words. If a non-empty stem exclusion set is
     * provided this analyzer will add a {@link SetKeywordMarkerFilter} before
     * stemming.
     *
     * @param stopWords        a stopword set
     * @param stemExclusionSet a set of terms not to be stemmed
     */
    public OxygenCustomAnalyzer(CharArraySet stopWords, CharArraySet stemExclusionSet) {
        super(stopWords);
        this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
        this.stopwords = CharArraySet.unmodifiableSet(CharArraySet.copy(stopWords));
    }

    /**
     * Builds an analyzer with the given stop words.
     *
     * @param stopWords a stopword set
     */
    public OxygenCustomAnalyzer(CharArraySet stopWords) {
        this(stopWords, OXYGEN_EXCLUSION_SET);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        TokenStream result = new StandardFilter(in);
        result = new LowerCaseFilter(result);
        return result;
    }

    /**
     * Returns an unmodifiable instance of the default stop words set.
     *
     * @return default stop words set.
     */
    public static CharArraySet getDefaultStopSet() {
        return OxygenCustomAnalyzer.DefaultSetHolder.DEFAULT_STOP_SET;
    }

    public static String symbolRemoval(String q) {
        int l = q.length();
        char[] res = new char[l * 2];
        char[] qu = q.toCharArray();

        /* Part 1 - Query deWildcardization
        We expect regular user input, so all potential wildcards or lucene special symbols have to be preceded by '\' symbol
        + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
        */

        /* Part 2 - Stemmer has a problem with periods and stops ',' '.'
        So, we will put spaces before them
        This is disabled for now because of exclusion set
         */
        int i = 0, j = 0;
        for (; i < l; i++) {
            // Part 1
            if (isAwildcard(qu[i])) {
                res[j++] = '\\';
            }
            if (i + 1 < l && isDoubleWC(qu[i], qu[i + 1])) {
                res[j++] = '\\';
            }
            // Part 2
            /*
            if (toSpace(qu[i])) res[j++] = ' ';
            */
            res[j++] = qu[i];

        }
        return String.valueOf(res).trim();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        /* This is the main point of the analyzer - Tegra */
        //TODO Change the analyzer
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new StandardFilter(source);    // Basic initialization
        result = new EnglishPossessiveFilter(result);       // Removes ' symbol (exmpl: Harry's book -> Harry book)
        result = new LowerCaseFilter(result);         // Self explanatory
        result = new StopFilter(result, stopwords);         // Stop words
        if (!stemExclusionSet.isEmpty())
            result = new SetKeywordMarkerFilter(result, stemExclusionSet); // Stemming exclusions
        result = new PorterStemFilter(result);              // Common algo, results are as good as any other filter
        return new TokenStreamComponents(source, result);
    }

    private static boolean isDoubleWC(char c, char c1) {
        if (c == c1)
            switch (c) {
                case '&':
                case '|':
                    return true;
                default:
                    return false;
            }
        else return false;
    }

    private static boolean isAwildcard(char c) {
        switch (c) {
            case '+':
            case '-':
            case '!':
            case '(':
            case ')':
            case '^':
            case '{':
            case '}':
            case '[':
            case ']':
            case '~':
            case '?':
            case '*':
            case ':':
            case '\\':
            case '/':
            case '"':
                return true;
            default:
                return false;
        }
    }

    private static boolean toSpace(char c) {
        switch (c) {
            case '.':
            case ',':
                return true;
            default:
                return false;
        }
    }

    /**
     * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class
     * accesses the static final set the first time.;
     */
    private static class DefaultSetHolder {
        static final CharArraySet DEFAULT_STOP_SET = OXYGEN_STOP_SET;
    }

}
