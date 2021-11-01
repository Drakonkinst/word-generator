public class PatternTokenWeighted implements PatternToken {
    private final RandomCollection<PatternToken> weightedChoice;

    public PatternTokenWeighted(final boolean optional, final RandomCollection<PatternToken> weightedChoice) {
        this.weightedChoice = weightedChoice;

        if(optional) {
            weightedChoice.add(WordGenerator.DEFAULT_WEIGHT, PatternTokenLiteral.EMPTY);
        }
    }

    @Override
    public String evaluate() {
        return weightedChoice.next().evaluate();
    }

    @Override
    public String toString() {
        return weightedChoice.toString();
    }
}
