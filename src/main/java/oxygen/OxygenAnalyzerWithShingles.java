package oxygen;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;


/**
 * A version of OxygenAnalyzer that uses bigrams
 */
public class OxygenAnalyzerWithShingles extends OxygenAnalyzerBase {

    /**
     * @return Oxygen-analyzer type
     */
    public static String getShingleInfo() {
        return new String("with shingles");
    }

    /**
     * Creates tokens for search
     * @param fieldName String to tokenize
     * @return TokenStreamComponents
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new StandardFilter(source);    // Basic initialization
        result = new EnglishPossessiveFilter(result);       // Removes ' symbol (exmpl: Harry's book -> Harry book)
        result = new LowerCaseFilter(result);               // Self explanatory

        if (!stemExclusionSet.isEmpty()) {
            result = new SetKeywordMarkerFilter(result, stemExclusionSet); // Stemming exclusions
        }

        result = new ShingleFilter(result);                 // min shingle is by default 2
        ((ShingleFilter) result).setOutputUnigrams(false);
        result = new PorterStemFilter(result);              // Common algo, results are as good as any other filter


        return new TokenStreamComponents(source, result);
    }
}
