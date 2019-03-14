import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * A simple word generator that can generate random words based on editable patterns.
 * Based on Awkwords 1.2 by Petr Mejzlik (http://akana.conlang.org/tools/awkwords/)
 */
public class WordGenerator {
    
    /* Private Variables */
    private String mainPattern;     //the main blueprint
    private String[] patterns;      //subpatterns that hold shortcuts to smaller patterns

    /* Constructors */
    //WordGenerator with 6 subpatterns
    public WordGenerator(String mainPattern, String patternC, String patternV, String patternN, String patternA, String patternB, String patternD) {
        this.mainPattern = mainPattern;
        this.patterns = new String[26];

        //insert patterns by letter
        patterns[2] = patternC;
        patterns[21] = patternV;
        patterns[13] = patternN;

        patterns[0] = patternA;
        patterns[1] = patternB;
        patterns[3] = patternD;
    }

    //WordGenerator with Consonant, Verb, and Noun subpatterns
    public WordGenerator(String mainPattern, String patternC, String patternV, String patternN) {
        this(mainPattern, patternC, patternV, patternN, "", "", "");
    }
    
    //basic WordGenerator with no subpatterns
    public WordGenerator(String mainPattern) {
        this( mainPattern, "", "", "");
    }

    /* User Methods */

    //assigns a pattern to a specific shortcut (from 'A' to 'Z')
    //returns whether it was successful or not
    public boolean set(char shortcut, String pattern) {
        if(isShortcut(shortcut)) {
            patterns[shortcut - 65] = pattern;
            return true;
        }

        return false;
    }

    //generates a specified number of words from the generator
    public String[] generate(int amount) {
        String[] result = new String[amount];
        for(int i = 0; i < amount; i++) {
            result[i] = render(mainPattern);
        }

        return result;
    }

    /* Calculations */

    //generates a single word from the pattern
    private String render(String pattern) {
        String[][] fragments = fragments(choose(pattern));
        String finalStr = "";

        for(int i = 0; exists(fragments, i ,0); i++) {
            String fragStr = "";

            if(fragments[i][0].isEmpty()) {
                continue;
            }

            switch(fragments[i][0].charAt(0)) {
                case '(':
                    //same as '[' but 50% chance to skip entirely
                    Random randomizer = new Random();
                    if(randomizer.nextBoolean()) {      
						int fragLength = fragments[i][0].length();
						fragStr = render(fragments[i][0].substring(1, fragLength - 1));
                    }
					break;
                case '[':
                    //recursively renders parts of the word
                    int fragLength = fragments[i][0].length();
                    fragStr = render(fragments[i][0].substring(1, fragLength - 1));
                    break;
                default:
                    fragStr = fragments[i][0];
            }

            for(int filterIndex = 0; exists(fragments, i, 1 + filterIndex); filterIndex++) {
                if(fragStr.equals(fragments[i][1 + filterIndex])) {
                    //abort rendering
                    return "RENDERING_ERROR";
                }
            }
			
			finalStr += fragStr;
        }

        //uncover dummy brackets
        return finalStr.replaceAll("12", "[(");
        
    }

    //parses a list of slash-delimited options
    private String choose(String str) {
        int strLength = str.length();
        List<String> options = new ArrayList<String>();
        List<String> target = new ArrayList<String>();

        for(int i = 0; i < strLength; i++) {
            String optionStr = "";
            String weightStr = "";

            for(int level = 0; !(i == strLength || (level == 0 && str.charAt(i) == '/')); i++) {
                //process option's characters
                if(str.charAt(i) == '"') {
                    //escaped characters
                    optionStr += str.charAt(i);
                    i++;
                    while(i < strLength) {
                        optionStr += str.charAt(i);
                        if(str.charAt(i) == '"') {
                            break;
                        }
                        i++;
                    }
                } else if(str.charAt(i) == '*' && level == 0) {
                    //weight specification
                    i++;
                    while(Character.isDigit(str.charAt(i)) && i < strLength) {
                        weightStr += str.charAt(i);
                        i++;
                    }
                    i--;
                } else {
                    char current = str.charAt(i);
                    optionStr += current;
                    if(current == '[' || current == '(') {
                        level++;
                    } else if(current == ']' || current == ')') {
                        level--;
                    }
                }
            }
            options.add(optionStr);

            //check weight range - capped at 128
            if(weightStr.isEmpty()) {
                weightStr = "1";
            }
            int weight = Integer.parseInt(weightStr);
            if(weight < 1) {
                weight = 1;
            }
            if(weight > 128) {
                weight = 128;
            }

            //insert number of references into list according to weight
            String option = options.get(options.size() - 1);
            for(int j = 0; j < weight; j++) {
                target.add(option);
            }
        }

        Random randomizer = new Random();
        return target.get(randomizer.nextInt(target.size()));
    }
    
    //divides a pattern into top-level fragments, returning the substrings as an array.
    private String[][] fragments(String pattern) {
        char current;
        int fragIndex = 0;
        int filterIndex = 0;
        int patternLength = pattern.length();
		String[][] fragments = new String[patternLength][2];

        for(int i = 0; i < patternLength; i++) {
            current = pattern.charAt(i);
            if(isShortcut(current)) {
                //shortcut letter
                int lastIndex = lastIndex();
                fragments[fragIndex][0] = "";

                for(int j = 0; j < lastIndex; j++) {
                    if(current - 65 == j) {
                        fragments[fragIndex][0] = "[" + patterns[j] + "]";
                    }
                }

                fragIndex++;
                filterIndex = 0;
            } else if(current == '^') {
                //recursively filters an open fragment
                i++;
                int length = 0;
                char next = pattern.charAt(i);
                boolean esc = false;            //note: quote escaping works inside filters

                while(esc || (!isSpecial(next) && (i + length < patternLength))) {
                    if(next == '"') {
                        esc = !esc;
                    }
                    length++;
                    next = pattern.charAt(i + length);
                }

                if(length > 0) {
                    String filter = fragments(pattern.substring(i, length))[0][0];
                    fragments[fragIndex - 1][1 + filterIndex] = filter;
                    filterIndex++;
                    i += length;
                    if(pattern.charAt(i) == '^') {
                        i--;
                    }
                }
            } else {
				fragments[fragIndex][0] = "";
				
                if(current == '[' || current == '(') {
                    //brackets
                    int level = -1;
                    do {
                        char next = pattern.charAt(i);
                        if(next == '[' || next == '(') {
                            level++;
                        } else if(next == ']' || next == ')') {
                            level--;
                        }

                        fragments[fragIndex][0] += next;
						i++;

                    } while(level >= 0 && i < patternLength);
                    i--;
                    fragIndex++;
                    filterIndex = 0;
                } else {
                    //read characters
                    char next;
                    while(i < patternLength && !isSpecial(pattern.charAt(i))) {
                        next = pattern.charAt(i);
                        if(next == '"') {
                            //escaping time!
                            i++;
                            if(pattern.charAt(i) == '"') {
                                //insert a single " in the fragment
                                fragments[fragIndex][0] += '"';
                            }
                            while(pattern.charAt(i) != '"' && i < patternLength) {
                                //read escaped characters
                                char escNext = pattern.charAt(i);
                                char addFrag = escNext;

                                //dummy characters for fragment-initial brackets to get around their detection in render()
                                if(escNext == '[') {
                                    addFrag = '1';
                                } else if(escNext == '(') {
                                    addFrag = '2';
                                }

                                fragments[fragIndex][0] += addFrag;
                                i++;
                            }
                        } else if(next != ' ') {
                            //note: spaces do not interrupt the fragment
                            fragments[fragIndex][0] += next;
                        }
                        
                        i++;
                    }
                    i--;
                    if(fragments[fragIndex] != null) {
                        fragIndex++;
                        filterIndex = 0;
                    }
                }
            }
        }
        return fragments;
    }

    /* Helper Methods */

    private boolean isShortcut(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private boolean isSpecial(char c) {
        return c == '[' || c == '(' || c == '^' || isShortcut(c);
    }
    
    //returns if index exists in 2D array and is not null
	private boolean exists(String[][] array, int row, int col) {
		return row < array.length && col < array[0].length && array[row][col] != null;
	}

    //gets last filled index for optimized looping
    private int lastIndex() {
        int lastIndex = 0;
        for(int i = 0; i <= 25; i++) {
            if(patterns[i] != null) {
                lastIndex = i + 1;
            }
        }
        return lastIndex;
    }
}