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
    public static final CharArraySet OXYGEN_STOP_SET;
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

    static {
        final List<String> stopWords = Arrays.asList(
                "a", "able", "about", "above", "abst", "accordance", "according", "accordingly", "across", "act",
                "actually", "added", "adj", "affected", "affecting", "affects", "after", "afterwards", "again",
                "against", "ah", "all", "almost", "alone", "along", "already", "also", "although", "always", "am",
                "among", "amongst", "an", "and", "announce", "another", "any", "anybody", "anyhow", "anymore",
                "anyone", "anything", "anyway", "anyways", "anywhere", "apparently", "approximately", "are", "aren",
                "arent", "arise", "around", "as", "aside", "ask", "asking", "at", "auth", "available", "away",
                "awfully", "b", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before",
                "beforehand", "begin", "beginning", "beginnings", "begins", "behind", "being", "believe", "below",
                "beside", "besides", "between", "beyond", "biol", "both", "brief", "briefly", "but", "by", "c",
                "ca", "came", "can", "cannot", "can't", "cause", "causes", "certain", "certainly", "co", "com",
                "come", "comes", "contain", "containing", "contains", "could", "couldnt", "d", "date", "did",
                "didn't", "different", "do", "does", "doesn't", "doing", "done", "don't", "down", "downwards",
                "due", "during", "e", "each", "ed", "edu", "effect", "eg", "eight", "eighty", "either", "else",
                "elsewhere", "end", "ending", "enough", "especially", "et", "et-al", "etc", "even", "ever",
                "every", "everybody", "everyone", "everything", "everywhere", "ex", "except", "f", "far", "few",
                "ff", "fifth", "first", "five", "fix", "followed", "following", "follows", "for", "former",
                "formerly", "forth", "found", "four", "from", "further", "furthermore", "g", "gave", "get",
                "gets", "getting", "give", "given", "gives", "giving", "go", "goes", "gone", "got", "gotten",
                "h", "had", "happens", "hardly", "has", "hasn't", "have", "haven't", "having", "he", "hed",
                "hence", "her", "here", "hereafter", "hereby", "herein", "heres", "hereupon", "hers",
                "herself", "hes", "hi", "hid", "him", "himself", "his", "hither", "home", "how", "howbeit",
                "however", "hundred", "i", "id", "ie", "if", "i'll", "im", "immediate", "immediately",
                "importance", "important", "in", "inc", "indeed", "index", "information", "instead", "into",
                "invention", "inward", "is", "isn't", "it", "itd", "it'll", "its", "itself", "i've",
                "j", "just", "k", "keep	keeps", "kept", "kg", "km", "know", "known", "knows", "l", "largely",
                "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets",
                "like", "liked", "likely", "line", "little", "'ll", "look", "looking", "looks", "ltd", "m",
                "made", "mainly", "make", "makes", "many", "may", "maybe", "me", "mean", "means", "meantime",
                "meanwhile", "merely", "mg", "might", "million", "miss", "ml", "more", "moreover", "most",
                "mostly", "mr", "mrs", "much", "mug", "must", "my", "myself", "n", "na", "name", "namely",
                "nay", "nd", "near", "nearly", "necessarily", "necessary", "need", "needs", "neither", "never",
                "nevertheless", "new", "next", "nine", "ninety", "no", "nobody", "non", "none", "nonetheless",
                "noone", "nor", "normally", "nos", "not", "noted", "nothing", "now", "nowhere", "o", "obtain",
                "obtained", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "omitted", "on",
                "once", "one", "ones", "only", "onto", "or", "ord", "other", "others", "otherwise", "ought",
                "our", "ours", "ourselves", "out", "outside", "over", "overall", "owing", "own", "p", "page",
                "pages", "part", "particular", "particularly", "past", "per", "perhaps", "placed", "please",
                "plus", "poorly", "possible", "possibly", "potentially", "pp", "predominantly", "present",
                "previously", "primarily", "probably", "promptly", "proud", "provides", "put", "q", "que",
                "quickly", "quite", "qv", "r", "ran", "rather", "rd", "re", "readily", "really", "recent",
                "recently", "ref", "refs", "regarding", "regardless", "regards", "related", "relatively",
                "respectively", "resulted", "resulting", "results", "right", "run", "s", "said", "say",
                "same", "saw", "say", "saying", "says", "sec", "section", "see", "seeing", "seem", "seemed",
                "seeming", "seems", "seen", "self", "selves", "sent", "seven", "several", "shall", "she",
                "shed", "she'll", "shes", "should", "shouldn't", "show", "showed", "shown", "showns", "shows",
                "significant", "significantly", "similar", "similarly", "since", "six", "slightly", "so",
                "some", "somebody", "somehow", "someone", "somethan", "something", "sometime", "sometimes",
                "somewhat", "somewhere", "soon", "sorry", "specifically", "specified", "specify", "specifying",
                "still", "stop", "strongly", "sub", "substantially", "successfully", "such", "sufficiently",
                "suggest", "sup", "sure"
        );
        final CharArraySet stopSet = new CharArraySet(stopWords, false);
        OXYGEN_STOP_SET = CharArraySet.unmodifiableSet(stopSet);
    }

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

    /**
     * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class
     * accesses the static final set the first time.;
     */
    private static class DefaultSetHolder {
        static final CharArraySet DEFAULT_STOP_SET = OXYGEN_STOP_SET;
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
