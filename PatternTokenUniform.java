import java.util.List;

public class PatternTokenUniform implements PatternToken {
    private final List<PatternToken> choices;

    public PatternTokenUniform(final boolean optional, final List<PatternToken> choices) {
        this.choices = choices;

        if(optional) {
            choices.add(PatternTokenLiteral.EMPTY);
        }
    }

    @Override
    public String evaluate() {
        return choices.get((int) (Math.random() * choices.size())).evaluate();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append('[');
        for(int i = 0; i < choices.size(); ++i) {
            s.append(choices.get(i).toString());
            if(i < choices.size() - 1) {
                s.append("/");
            }
        }
        s.append(']');
        return s.toString();
    }
}
