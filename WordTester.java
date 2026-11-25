import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Testing driver for the Word class. Run main() and read the report:
 * every check prints PASS or FAIL, followed by a summary at the end.
 *
 * words.txt must be in the working folder (the same folder as the
 * .java files in jGRASP) for the dictionary tests to pass.
 */
public class WordTester {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        testFeedback();
        testGuessValidation();
        testSecretValidation();
        testGameState();
        testBoardDisplay();
        testDictionaryFile();
        testMessyDictionary();
        testBadDictionaries();
        testRandomWord();

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        if (failed == 0) {
            System.out.println("All tests passed.");
        }
    }

    // ---------------------------------------------------------------
    // Feedback, including repeated-letter cases
    // ---------------------------------------------------------------
    private static void testFeedback() {
        System.out.println("== Feedback ==");
        checkFeedback("QI", "QI", "[Q] [I]");    // exact match
        checkFeedback("QI", "qi", "[Q] [I]");    // lower case guess
        checkFeedback("QI", "XU", "-X- -U-");    // nothing in common
        checkFeedback("AB", "BA", "(B) (A)");    // both letters swapped
        checkFeedback("AB", "AX", "[A] -X-");    // one correct
        checkFeedback("AB", "XA", "-X- (A)");    // one present
        checkFeedback("MA", "AM", "(A) (M)");
        // Repeated letters: these break a naive one-pass algorithm.
        checkFeedback("BA", "AA", "-A- [A]");    // only one A in secret
        checkFeedback("AB", "AA", "[A] -A-");
        checkFeedback("MO", "MM", "[M] -M-");
        checkFeedback("OM", "MM", "-M- [M]");
        checkFeedback("HM", "MM", "-M- [M]");
        checkFeedback("AA", "AB", "[A] -B-");
        checkFeedback("AA", "BA", "-B- [A]");
        checkFeedback("MM", "MM", "[M] [M]");
        checkFeedback("MM", "MO", "[M] -O-");
        checkFeedback("AA", "BB", "-B- -B-");
    }

    // ---------------------------------------------------------------
    // Guess input rules: exactly two letters A-Z, case insensitive
    // ---------------------------------------------------------------
    private static void testGuessValidation() {
        System.out.println("== Guess validation ==");
        check("\"qi\" becomes \"QI\"", Word.normalizeGuess("qi").equals("QI"));
        check("\"Qi\" becomes \"QI\"", Word.normalizeGuess("Qi").equals("QI"));
        check("\"QI\" stays \"QI\"", Word.normalizeGuess("QI").equals("QI"));
        check("\"zz\" accepted (not a word, but allowed)",
                Word.normalizeGuess("zz").equals("ZZ"));

        checkGuessRejected("");          // empty line
        checkGuessRejected("Q");         // too short
        checkGuessRejected("QIS");       // too long
        checkGuessRejected(" QI");       // leading space
        checkGuessRejected("QI ");       // trailing space
        checkGuessRejected("Q I");       // space inside
        checkGuessRejected("  ");        // two spaces
        checkGuessRejected("Q1");        // digit
        checkGuessRejected("!?");        // punctuation
        checkGuessRejected("\t\t");      // tabs
        checkGuessRejected("éa");   // accented letter (e with accent)
        checkGuessRejected("ß");    // German sharp s: upper-cases to "SS"
        checkGuessRejected(null);
        checkGuessRejected("STOP");      // STOP is handled by the client, not Word
    }

    // ---------------------------------------------------------------
    // Specific-word constructor
    // ---------------------------------------------------------------
    private static void testSecretValidation() {
        System.out.println("== Secret word constructor ==");
        check("new Word(\"qi\") has secret \"QI\"",
                new Word("qi").getSecret().equals("QI"));
        checkSecretRejected("Q");
        checkSecretRejected("QIS");
        checkSecretRejected("Q1");
        checkSecretRejected(" QI");
        checkSecretRejected("");
        checkSecretRejected(null);
    }

    // ---------------------------------------------------------------
    // Guess counting, winning, losing, game over
    // ---------------------------------------------------------------
    private static void testGameState() {
        System.out.println("== Game state ==");
        Word w = new Word("QI");
        check("new game: 0 guesses made", w.getGuessCount() == 0);
        check("new game: 6 guesses remaining", w.getGuessesRemaining() == 6);
        check("new game: not solved", !w.isSolved());
        check("new game: not over", !w.isGameOver());

        w.makeGuess("ab");
        check("after 1 guess: count is 1", w.getGuessCount() == 1);
        check("after 1 guess: 5 remaining", w.getGuessesRemaining() == 5);

        try {
            w.makeGuess("abc");
            check("invalid guess throws", false);
        } catch (IllegalArgumentException e) {
            check("invalid guess throws", true);
        }
        check("invalid guess does not use a turn", w.getGuessCount() == 1);

        w.makeGuess("qi");
        check("correct guess: solved", w.isSolved());
        check("correct guess: game over", w.isGameOver());
        check("correct guess: count is 2", w.getGuessCount() == 2);
        checkGuessAfterGameOverThrows(w);

        // Lose: six wrong guesses.
        Word lose = new Word("QI");
        String[] wrong = {"aa", "bb", "cc", "dd", "ee", "ff"};
        for (int i = 0; i < wrong.length; i++) {
            check("before wrong guess " + (i + 1) + ": not over", !lose.isGameOver());
            lose.makeGuess(wrong[i]);
        }
        check("6 wrong guesses: game over", lose.isGameOver());
        check("6 wrong guesses: not solved", !lose.isSolved());
        check("6 wrong guesses: 0 remaining", lose.getGuessesRemaining() == 0);
        checkGuessAfterGameOverThrows(lose);

        // Win on the last possible guess.
        Word lastChance = new Word("QI");
        for (int i = 0; i < 5; i++) {
            lastChance.makeGuess(wrong[i]);
        }
        lastChance.makeGuess("QI");
        check("win on 6th guess: solved", lastChance.isSolved());

        // A guess after an earlier correct-looking letter does not solve it.
        Word partial = new Word("QI");
        partial.makeGuess("QA");
        check("partly correct guess: not solved", !partial.isSolved());

        // evaluate() alone does not record a guess.
        Word peek = new Word("QI");
        peek.evaluate("QI");
        check("evaluate() does not use a turn", peek.getGuessCount() == 0);
        check("evaluate() does not solve the game", !peek.isSolved());
    }

    // ---------------------------------------------------------------
    // toString board
    // ---------------------------------------------------------------
    private static void testBoardDisplay() {
        System.out.println("== Board display ==");
        Word w = new Word("QI");
        check("empty board message", w.toString().equals("(no guesses yet)"));
        w.makeGuess("iq");
        w.makeGuess("qi");
        String expected = "1. (I) (Q)\n2. [Q] [I]";
        boolean ok = w.toString().equals(expected);
        check("board after two guesses", ok);
        if (!ok) {
            System.out.println("      expected:\n" + expected);
            System.out.println("      actual:\n" + w);
        }
    }

    // ---------------------------------------------------------------
    // The real words.txt file
    // ---------------------------------------------------------------
    private static void testDictionaryFile() {
        System.out.println("== words.txt ==");
        Set<String> words;
        try {
            words = Word.loadDictionary(new File("words.txt"));
        } catch (FileNotFoundException e) {
            check("words.txt found in working folder", false);
            return;
        }
        check("words.txt has 107 words (found " + words.size() + ")",
                words.size() == 107);

        String[] mustHave = {"AA", "MM", "OK", "EW", "QI", "ZA", "XU", "HM"};
        for (String word : mustHave) {
            check("words.txt contains " + word, words.contains(word));
        }
        // Collins-only words must NOT be in the NWL list.
        String[] mustNotHave = {"ZE", "OO", "CH", "KY", "EE", "JA"};
        for (String word : mustNotHave) {
            check("words.txt does not contain " + word + " (Collins only)",
                    !words.contains(word));
        }
        boolean allValid = true;
        for (String word : words) {
            if (!word.matches("[A-Z]{2}")) {
                allValid = false;
            }
        }
        check("every entry is two upper-case letters", allValid);
    }

    // ---------------------------------------------------------------
    // Loader cleans spaces, blank lines, case, and duplicates
    // ---------------------------------------------------------------
    private static void testMessyDictionary() throws IOException {
        System.out.println("== Messy dictionary file ==");
        File messy = writeTempFile(
                "  qi  \n\nQI\nza\n   \n\tZa \nqI\n");
        Set<String> words = Word.loadDictionary(messy);
        check("messy file loads 2 unique words (found " + words.size() + ")",
                words.size() == 2);
        check("messy file contains QI", words.contains("QI"));
        check("messy file contains ZA", words.contains("ZA"));
        messy.delete();
    }

    // ---------------------------------------------------------------
    // Bad dictionary files fail loudly
    // ---------------------------------------------------------------
    private static void testBadDictionaries() throws IOException {
        System.out.println("== Bad dictionary files ==");
        File threeLetters = writeTempFile("QI\nCAT\nZA\n");
        checkLoadRejected("file with a 3-letter word", threeLetters);
        threeLetters.delete();

        File digit = writeTempFile("QI\nA1\n");
        checkLoadRejected("file with a digit", digit);
        digit.delete();

        File empty = writeTempFile("\n  \n\n");
        checkLoadRejected("file with no words", empty);
        empty.delete();

        try {
            Word.loadDictionary(new File("this_file_does_not_exist.txt"));
            check("missing file throws FileNotFoundException", false);
        } catch (FileNotFoundException e) {
            check("missing file throws FileNotFoundException", true);
        }
    }

    // ---------------------------------------------------------------
    // Random secret word
    // ---------------------------------------------------------------
    private static void testRandomWord() throws FileNotFoundException {
        System.out.println("== Random secret word ==");
        Set<String> words = Word.loadDictionary(new File("words.txt"));

        boolean allFromDictionary = true;
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 200; i++) {
            String secret = new Word(new File("words.txt")).getSecret();
            if (!words.contains(secret)) {
                allFromDictionary = false;
            }
            seen.add(secret);
        }
        check("200 random secrets all come from words.txt", allFromDictionary);
        check("random secrets vary (" + seen.size() + " different in 200 games)",
                seen.size() > 20);

        // Same seed gives the same word, so a bug can be reproduced.
        String first = Word.pickRandomWord(words, new Random(42));
        String second = Word.pickRandomWord(words, new Random(42));
        check("same seed gives same word", first.equals(second));
    }

    // ===============================================================
    // Helper methods
    // ===============================================================

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + description);
        } else {
            failed++;
            System.out.println("  FAIL  " + description);
        }
    }

    private static void checkFeedback(String secret, String guess, String expected) {
        Word w = new Word(secret);
        List<Word.LetterResult> result = w.evaluate(guess);
        String actual = Word.formatResult(guess, result);
        String description = "secret " + secret + ", guess " + guess
                + " -> " + expected;
        if (!actual.equals(expected)) {
            description += "   (got " + actual + ")";
        }
        check(description, actual.equals(expected));
    }

    private static void checkGuessRejected(String guess) {
        String shown = (guess == null) ? "null" : "\"" + guess + "\"";
        try {
            Word.normalizeGuess(guess);
            check("guess " + shown + " rejected", false);
        } catch (IllegalArgumentException e) {
            check("guess " + shown + " rejected with correct message",
                    e.getMessage().equals(Word.IMPROPER_INPUT_MESSAGE));
        }
    }

    private static void checkSecretRejected(String secret) {
        String shown = (secret == null) ? "null" : "\"" + secret + "\"";
        try {
            new Word(secret);
            check("secret " + shown + " rejected", false);
        } catch (IllegalArgumentException e) {
            check("secret " + shown + " rejected", true);
        }
    }

    private static void checkGuessAfterGameOverThrows(Word w) {
        try {
            w.makeGuess("ZZ");
            check("guess after game over throws IllegalStateException", false);
        } catch (IllegalStateException e) {
            check("guess after game over throws IllegalStateException", true);
        }
    }

    private static void checkLoadRejected(String description, File file)
            throws FileNotFoundException {
        try {
            Word.loadDictionary(file);
            check(description + " rejected", false);
        } catch (IllegalArgumentException e) {
            check(description + " rejected (" + e.getMessage() + ")", true);
        }
    }

    private static File writeTempFile(String contents) throws IOException {
        File file = File.createTempFile("wordtest", ".txt");
        PrintStream out = new PrintStream(file);
        out.print(contents);
        out.close();
        return file;
    }
}
