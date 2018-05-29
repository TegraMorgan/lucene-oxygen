package utils;

public class QueryToLucene {
    public static String symbolRemoval(String q) {
        int originalQueryLength = q.length();
        char[] res = new char[originalQueryLength * 2];
        char[] qu = q.toCharArray();

        /* Part 1 - Query deWildcardization
        We expect regular user input, so all potential wildcards or lucene special symbols have to be preceded by '\' symbol
        + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
        */

        /* Part 2 - Stemmer has a problem with periods and stops ',' '.'
        So, we will put spaces before them
        This is disabled for now because of exclusion set
         */
        int originalQueryPosition = 0, newQueryPosition = 0;
        for (; originalQueryPosition < originalQueryLength; originalQueryPosition++) {
            // Part 1
            if (isAwildcard(qu[originalQueryPosition])) {
                res[newQueryPosition++] = '\\';
            }
            if (originalQueryPosition + 1 < originalQueryLength && isDoubleWC(qu[originalQueryPosition], qu[originalQueryPosition + 1])) {
                res[newQueryPosition++] = '\\';
            }
            // Part 2
            /*
            if (toSpace(qu[i])) res[j++] = ' ';
            */
            res[newQueryPosition++] = qu[originalQueryPosition];

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
}
