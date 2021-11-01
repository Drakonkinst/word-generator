import java.util.List;

public class PatternTokenTuple implements PatternToken {
    private final List<PatternToken> items;

    public PatternTokenTuple(final List<PatternToken> items) {
        this.items = items;
    }

    @Override
    public String evaluate() {
        final StringBuilder result = new StringBuilder();
        for(final PatternToken item : items) {
            result.append(item.evaluate());
        }
        return result.toString();
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        for(int i = 0; i < items.size(); ++i) {
            s.append(items.get(i).toString());
            if(i < items.size() - 1) {
                s.append(" + ");
            }
        }
        return s.toString();
    }
}
