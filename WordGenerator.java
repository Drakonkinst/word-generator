import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A word generator that can generate random words based on editable patterns.
 * Based on Awkwords 1.2 by Petr Mejzlik (http://akana.conlang.org/tools/awkwords/)
 */
public class WordGenerator {
    public static final int DEFAULT_WEIGHT = 1;

    private static final int CHAR_A = 65;
    private static final int CHAR_Z = 90;
    private static final int MIN_WEIGHT = 1;
    private static final int MAX_WEIGHT = 128;
    private static final Pattern CONTAINS_SHORTCUT_CHARS = Pattern.compile("[A-Z](?=[^\"]*(?:\"[^\"]*\"[^\"]*)*$)");

    private final PatternToken token;

    public static Builder builder(final String mainPattern) {
        return new Builder(mainPattern);
    }

    private WordGenerator(final PatternToken token) {
        this.token = token;
    }

    public String generate() {
        return token.evaluate();
    }

    public static class Builder {
        private static boolean isShortcut(char c) {
            return c >= CHAR_A && c <= CHAR_Z;
        }

        private static int extractWeight(final String weightStr) {
            if(weightStr.length() > 0) {
                final int weight = Integer.parseInt(weightStr);
                if(weight < MIN_WEIGHT) {
                    return MIN_WEIGHT;
                } else if(weight > MAX_WEIGHT) {
                    return MAX_WEIGHT;
                }
                return weight;
            }
            return DEFAULT_WEIGHT;
        }

        private final String mainPattern;
        private final Map<Character, String> primitive = new HashMap<>();
        private final Map<Character, String> complex = new HashMap<>();
        private final Map<Character, PatternToken> subpatterns = new HashMap<>();

        public Builder(final String mainPattern) {
            this.mainPattern = mainPattern;
        }

        public Builder setPattern(final char shortcut, final String pattern) {
            if(!isShortcut(shortcut)) {
                throw new IllegalArgumentException("Shortcut letters must be between A and Z");
            }
            final Matcher matcher = CONTAINS_SHORTCUT_CHARS.matcher(pattern);
            final boolean matches = matcher.find();
            if(matches) {
                // Complex
                complex.put(shortcut, pattern);
            } else {
                // Primitive
                primitive.put(shortcut, pattern);
            }
            return this;
        }

        // Parse top-level lists
        public PatternToken parseFragments(final String pattern, final boolean optional) {
            final List<String> fragments = new ArrayList<>();
            final List<Integer> weights = new ArrayList<>();
            final int patternLength = pattern.length();
            StringBuilder fragStr = new StringBuilder();
            boolean isWeighted = false;

            for (int i = 0; i < patternLength; ++i) {
                char currentChar = pattern.charAt(i);
                if (currentChar == '/') {
                    if (!fragStr.isEmpty()) {
                        fragments.add(fragStr.toString());
                        weights.add(DEFAULT_WEIGHT);
                    }
                    fragStr = new StringBuilder();
                } else if (currentChar == '[' || currentChar == '(') {
                    int level = -1;
                    do {
                        currentChar = pattern.charAt(i);
                        if (currentChar == '[' || currentChar == '(') {
                            ++level;
                        } else if (currentChar == ']' || currentChar == ')') {
                            --level;
                        }
                        fragStr.append(currentChar);
                        ++i;
                    } while (level >= 0 && i < patternLength);
                    --i;
                } else if (currentChar == '"') {
                    fragStr.append('"');
                    do {
                        ++i;
                        currentChar = pattern.charAt(i);
                        fragStr.append(currentChar);
                    } while (currentChar != '"' && i + 1 < patternLength);
                } else if (currentChar == '*') {
                    isWeighted = true;
                    final String frag = fragStr.toString();

                    final StringBuilder weightStr = new StringBuilder();
                    ++i;
                    while (i < patternLength) {
                        currentChar = pattern.charAt(i);
                        if (Character.isDigit(currentChar)) {
                            weightStr.append(currentChar);
                        } else {
                            --i;
                            break;
                        }
                        ++i;
                    }
                    int weight = extractWeight(weightStr.toString());
                    fragments.add(frag);
                    weights.add(weight);
                    fragStr = new StringBuilder();
                } else {
                    fragStr.append(currentChar);
                }
            }
            if(!fragStr.isEmpty()) {
                fragments.add(fragStr.toString());
                weights.add(DEFAULT_WEIGHT);
            }

            if(fragments.size() > 1) {
                if(isWeighted) {
                    final RandomCollection<PatternToken> tokens = new RandomCollection<>();
                    for(int i = 0; i < fragments.size(); ++i) {
                        tokens.add(weights.get(i), parseFragment(fragments.get(i)));
                    }
                    return new PatternTokenWeighted(optional, tokens);
                } else {
                    final List<PatternToken> tokens = new ArrayList<>();
                    for(int i = 0; i < fragments.size(); ++i) {
                        tokens.add(parseFragment(fragments.get(i)));
                    }
                    return new PatternTokenUniform(optional, tokens);
                }
            }
            
            if(optional) {
                final List<PatternToken> singletonToken = new ArrayList<>();
                singletonToken.add(parseFragment(fragments.get(0)));
                return new PatternTokenUniform(true, singletonToken);
            }
            return parseFragment(pattern);
        }

        private PatternToken parseFragment(final String pattern) {
            final int patternLength = pattern.length();

            final List<PatternToken> tokens = new ArrayList<>();
            for(int i = 0; i < patternLength; ++i) {
                char currentChar = pattern.charAt(i);

                if(currentChar == '"') {
                    final StringBuilder escapedStr = new StringBuilder();
                    ++i;
                    while(i < patternLength) {
                        currentChar = pattern.charAt(i);
                        ++i;
                        if(currentChar != '"') {
                            escapedStr.append(currentChar);
                        } else {
                            break;
                        }
                    }
                    --i;
                    tokens.add(PatternTokenLiteral.of(escapedStr.toString()));
                } else if(currentChar == '[' || currentChar == '(') {
                    int level = 0;
                    final boolean optional = currentChar == '(';
                    final StringBuilder listStr = new StringBuilder();
                    ++i;

                    while(i < patternLength) {
                        currentChar = pattern.charAt(i);
                        if (currentChar == '[' || currentChar == '(') {
                            ++level;
                        } else if (currentChar == ']' || currentChar == ')') {
                            --level;
                            if(level < 0) {
                                break;
                            }
                        }
                        listStr.append(currentChar);
                        ++i;
                    }

                    tokens.add(parseFragments(listStr.toString(), optional));
                } else if(isShortcut(currentChar)) {
                    final PatternToken subpatternToken = subpatterns.get(currentChar);
                    if(subpatternToken == null) {
                        // This is a bit unreliable
                        throw new IllegalArgumentException("Error: Subpattern " + currentChar + " has not been loaded"
                            + " yet. Complex subpatterns cannot reference other complex subpatterns.");
                    }
                    tokens.add(subpatternToken);
                } else {
                    final StringBuilder literalStr = new StringBuilder();
                    while(i < patternLength) {
                        currentChar = pattern.charAt(i);
                        if(currentChar == '[' || currentChar == '(' || currentChar == '"' || isShortcut(currentChar)) {
                            break;
                        }
                        ++i;
                        literalStr.append(currentChar);
                    }
                    --i;
                    tokens.add(PatternTokenLiteral.of(literalStr.toString()));
                }
            }

            if(tokens.size() <= 0) {
                return PatternTokenLiteral.EMPTY;
            }
            if(tokens.size() == 1) {
                return tokens.get(0);
            }
            return new PatternTokenTuple(tokens);
        }

        public WordGenerator build() {
            // Compile all primitive tokens
            for(Map.Entry<Character, String> entry : primitive.entrySet()) {
                subpatterns.put(entry.getKey(), parseFragments(entry.getValue(), false));
                System.out.println(entry.getKey() + " = " + entry.getValue() + " -> " + subpatterns.get(entry.getKey()));
            }

            // Compile all complex tokens
            for(Map.Entry<Character, String> entry : complex.entrySet()) {
                subpatterns.put(entry.getKey(), parseFragments(entry.getValue(), false));
                System.out.println(entry.getKey() + " = " + entry.getValue() + " -> " + subpatterns.get(entry.getKey()));
            }

            // Compile main pattern
            final PatternToken mainPatternToken = parseFragments(mainPattern, false);
            System.out.println("MAIN = " + mainPattern + " -> " + mainPatternToken);
            return new WordGenerator(mainPatternToken);
        }
    }
}