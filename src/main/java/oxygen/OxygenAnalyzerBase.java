package oxygen;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.shingle.ShingleFilter;

public class OxygenAnalyzerBase extends StopwordAnalyzerBase {
    public static final CharArraySet OXYGEN_EXCLUSION_SET;

    protected final CharArraySet stemExclusionSet;
    protected final CharArraySet stopwords;


    // US
    static {
        final List<String> exclusionSet = Arrays.asList(
                "u.s.a", "u.s.a.", "u.s", "u.s."
        );
        final CharArraySet stopSet = new CharArraySet(exclusionSet, false);
        OXYGEN_EXCLUSION_SET = CharArraySet.unmodifiableSet(stopSet);
    }


    //Abbigious abbr. removed
    //TODO special quotation filter: (Eddisson) etc.



    public OxygenAnalyzerBase() {
        this(getDefaultStopSet());
    }

    /**
     * Builds an analyzer with the given stop words. If a non-empty stem exclusion set is
     * provided this analyzer will add a {@link SetKeywordMarkerFilter} before
     * stemming.
     *
     * @param stopWords        a stopword set
     * @param stemExclusionSet a set of terms not to be stemmed
     */
    public OxygenAnalyzerBase(CharArraySet stopWords, CharArraySet stemExclusionSet) {
        super(stopWords);
        this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
        this.stopwords = CharArraySet.unmodifiableSet(CharArraySet.copy(stopWords));
    }

    /**
     * Builds an analyzer with the given stop words.
     *
     * @param stopWords a stopword set
     */
    public OxygenAnalyzerBase(CharArraySet stopWords) {
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
        return DefaultSetHolder.DEFAULT_STOP_SET;
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
        static final CharArraySet DEFAULT_STOP_SET = Constants.OXYGEN_STOP_SET;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        /* This is the main point of the analyzer - Tegra */
        //TODO Change the analyzer
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new StandardFilter(source);    // Basic initialization
        result = new EnglishPossessiveFilter(result);       // Removes ' symbol (exmpl: Harry's book -> Harry book)
        result = new LowerCaseFilter(result);               // Self explanatory
        result = new StopFilter(result, stopwords);         // Stop words
        if (!stemExclusionSet.isEmpty()) {
            result = new SetKeywordMarkerFilter(result, stemExclusionSet); // Stemming exclusions
        }
        result = new PorterStemFilter(result);              // Common algo, results are as good as any other filter
        return new TokenStreamComponents(source, result);
    }
}
