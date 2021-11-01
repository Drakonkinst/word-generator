import java.util.HashMap;
import java.util.Map;

public class PatternTokenLiteral implements PatternToken {
    private static final Map<String, PatternTokenLiteral> CACHE = new HashMap<>();

    public static PatternTokenLiteral EMPTY = new PatternTokenLiteral("");
    private final String str;

    public static PatternTokenLiteral of(final String str) {
        if(CACHE.containsKey(str)) {
            return CACHE.get(str);
        }
        final PatternTokenLiteral literal = new PatternTokenLiteral(str);
        CACHE.put(str, literal);
        return literal;
    }

    private PatternTokenLiteral(final String str) {
        this.str = str;
    }

    @Override
    public String evaluate() {
        return str;
    }

    @Override
    public String toString() {
        return "\"" + str + "\"";
    }
}
