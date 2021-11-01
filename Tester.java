public class Tester {
    public static void main(String[] args) {
        WordGenerator wordGenerator = WordGenerator.builder("CV(CV)(N)")
            .setPattern('V', "a/i/u")
            .setPattern('C', "p/t/k/s/m/n")
            .setPattern('N', "m/n")
            .build();
        
        System.out.println();
        for (int i = 0; i < 10; ++i) {
            System.out.println(wordGenerator.generate());
        }
    }
}
