import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * A Word object holds the secret word for one game of Two-Letter Wordle,
 * the guesses made so far, and the methods that check a guess against
 * the secret word.
 *
 * Each letter of a guess gets one of three results:
 *   CORRECT - right letter, right position        shown as [Q]
 *   PRESENT - letter is in the word, wrong spot   shown as (Q)
 *   ABSENT  - letter is not in the word           shown as -Q-
 *
 * Guesses are case insensitive and do not have to be real words.
 * Any combination of two letters A-Z is accepted.
 */
public class Word {

    /** The possible results for one letter of a guess. */
    public enum LetterResult { CORRECT, PRESENT, ABSENT }

    /** Number of letters in the secret word and in every guess. */
    public static final int WORD_LENGTH = 2;

    /** Number of guesses the player gets per game. */
    public static final int MAX_GUESSES = 6;

    /** Message used when a guess is not exactly two letters A-Z. */
    public static final String IMPROPER_INPUT_MESSAGE =
            "Improper input. Please enter a two letter guess";

    private final String secret;                    // always upper case
    private final List<String> guesses;             // guesses made, in order
    private final List<List<LetterResult>> results; // results for each guess

    /**
     * Creates a game with a specific secret word. Useful for testing
     * and debugging. The word is converted to upper case.
     *
     * @param secret the secret word, exactly WORD_LENGTH letters A-Z
     * @throws IllegalArgumentException if secret is null or not
     *         exactly WORD_LENGTH letters A-Z
     */
    public Word(String secret) {
        if (!isValidLetters(secret)) {
            throw new IllegalArgumentException(
                    "Secret word must be exactly " + WORD_LENGTH
                    + " letters A-Z, but was: " + secret);
        }
        this.secret = secret.toUpperCase(Locale.ROOT);
        this.guesses = new ArrayList<String>();
        this.results = new ArrayList<List<LetterResult>>();
    }

    /**
     * Creates a game whose secret word is chosen at random from a
     * dictionary file (one word per line).
     *
     * @param dictionaryFile the dictionary file to read
     * @throws FileNotFoundException if the file does not exist
     * @throws IllegalArgumentException if the file has a bad entry
     *         or contains no words
     */
    public Word(File dictionaryFile) throws FileNotFoundException {
        this(pickRandomWord(loadDictionary(dictionaryFile), new Random()));
    }

    /**
     * Reads a dictionary file and returns its words in a sorted set.
     * Leading and trailing spaces are removed, blank lines are skipped,
     * words are converted to upper case, and duplicates are dropped
     * (a Set cannot hold the same word twice).
     *
     * @param dictionaryFile the file to read, one word per line
     * @return the words in the file, upper case, sorted
     * @throws FileNotFoundException if the file does not exist
     * @throws IllegalArgumentException if a line is not exactly
     *         WORD_LENGTH letters A-Z, or the file contains no words
     */
    public static Set<String> loadDictionary(File dictionaryFile)
            throws FileNotFoundException {
        Set<String> words = new TreeSet<String>();
        Scanner input = new Scanner(dictionaryFile);
        try {
            int lineNumber = 0;
            while (input.hasNextLine()) {
                lineNumber++;
                String line = input.nextLine().trim();
                if (!line.isEmpty()) {
                    if (!isValidLetters(line)) {
                        throw new IllegalArgumentException(
                                "Bad entry on line " + lineNumber + " of "
                                + dictionaryFile.getName() + ": \"" + line + "\"");
                    }
                    words.add(line.toUpperCase(Locale.ROOT));
                }
            }
        } finally {
            input.close();
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException(
                    dictionaryFile.getName() + " contains no words");
        }
        return words;
    }

    /**
     * Picks one word at random from a set of words.
     *
     * @param words the words to choose from (must not be empty)
     * @param rand the random number generator to use
     * @return a randomly chosen word from the set
     * @throws IllegalArgumentException if words is empty
     */
    public static String pickRandomWord(Set<String> words, Random rand) {
        if (words.isEmpty()) {
            throw new IllegalArgumentException("No words to choose from");
        }
        // A Set has no index, so copy it into a list to pick by position.
        List<String> wordList = new ArrayList<String>(words);
        return wordList.get(rand.nextInt(wordList.size()));
    }

    /**
     * Checks that a guess is exactly WORD_LENGTH letters A-Z (either case)
     * and returns it in upper case.
     *
     * @param guess the player's input
     * @return the guess in upper case
     * @throws IllegalArgumentException if guess is null, the wrong length,
     *         or contains anything other than letters A-Z (spaces included)
     */
    public static String normalizeGuess(String guess) {
        if (!isValidLetters(guess)) {
            throw new IllegalArgumentException(IMPROPER_INPUT_MESSAGE);
        }
        return guess.toUpperCase(Locale.ROOT);
    }

    /**
     * Compares a guess to the secret word without recording it.
     *
     * Uses two passes so repeated letters are handled correctly:
     *   1. Mark every letter in the right position CORRECT, and count
     *      the secret's letters that were NOT matched.
     *   2. For each remaining guess letter, mark it PRESENT only if an
     *      unmatched copy of that letter is still left in the count;
     *      otherwise mark it ABSENT.
     * Example: secret BA, guess AA gives ABSENT, CORRECT. The secret has
     * only one A, and it is already used by the CORRECT match.
     *
     * @param guess the guess to check (case insensitive)
     * @return one LetterResult per letter of the guess, in order
     * @throws IllegalArgumentException if guess is not valid input
     */
    public List<LetterResult> evaluate(String guess) {
        String g = normalizeGuess(guess);
        LetterResult[] marks = new LetterResult[WORD_LENGTH];
        Map<Character, Integer> unmatched = new HashMap<Character, Integer>();

        // Pass 1: exact matches, and count the secret letters left over.
        for (int i = 0; i < WORD_LENGTH; i++) {
            char secretLetter = secret.charAt(i);
            if (g.charAt(i) == secretLetter) {
                marks[i] = LetterResult.CORRECT;
            } else {
                int count = unmatched.getOrDefault(secretLetter, 0);
                unmatched.put(secretLetter, count + 1);
            }
        }

        // Pass 2: letters in the word but in the wrong position.
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (marks[i] == null) {
                char guessLetter = g.charAt(i);
                int count = unmatched.getOrDefault(guessLetter, 0);
                if (count > 0) {
                    marks[i] = LetterResult.PRESENT;
                    unmatched.put(guessLetter, count - 1);
                } else {
                    marks[i] = LetterResult.ABSENT;
                }
            }
        }

        List<LetterResult> resultList = new ArrayList<LetterResult>();
        for (LetterResult mark : marks) {
            resultList.add(mark);
        }
        return resultList;
    }

    /**
     * Records a guess and returns its results. An invalid guess throws
     * an exception and does NOT use up one of the player's guesses.
     *
     * @param guess the player's guess (case insensitive)
     * @return one LetterResult per letter of the guess
     * @throws IllegalStateException if the game is already over
     * @throws IllegalArgumentException if guess is not valid input
     */
    public List<LetterResult> makeGuess(String guess) {
        if (isGameOver()) {
            throw new IllegalStateException("The game is already over");
        }
        String g = normalizeGuess(guess);
        List<LetterResult> result = evaluate(g);
        guesses.add(g);
        results.add(result);
        return result;
    }

    /** @return true if the most recent guess matched the secret word */
    public boolean isSolved() {
        return !guesses.isEmpty()
                && guesses.get(guesses.size() - 1).equals(secret);
    }

    /** @return true if the word was guessed or no guesses remain */
    public boolean isGameOver() {
        return isSolved() || guesses.size() >= MAX_GUESSES;
    }

    /** @return the number of valid guesses made so far */
    public int getGuessCount() {
        return guesses.size();
    }

    /** @return the number of guesses the player has left */
    public int getGuessesRemaining() {
        return MAX_GUESSES - guesses.size();
    }

    /** @return the secret word, in upper case */
    public String getSecret() {
        return secret;
    }

    /**
     * Formats a guess and its results as plain text,
     * e.g. guess QI with CORRECT, ABSENT becomes "[Q] -I-".
     *
     * @param guess the guess (upper or lower case)
     * @param result the results for each letter of the guess
     * @return the formatted text
     */
    public static String formatResult(String guess, List<LetterResult> result) {
        String g = guess.toUpperCase(Locale.ROOT);
        String text = "";
        for (int i = 0; i < result.size(); i++) {
            if (i > 0) {
                text += " ";
            }
            char letter = g.charAt(i);
            LetterResult mark = result.get(i);
            if (mark == LetterResult.CORRECT) {
                text += "[" + letter + "]";
            } else if (mark == LetterResult.PRESENT) {
                text += "(" + letter + ")";
            } else {
                text += "-" + letter + "-";
            }
        }
        return text;
    }

    /**
     * Returns the board: every guess so far with its results,
     * one numbered line per guess.
     */
    @Override
    public String toString() {
        if (guesses.isEmpty()) {
            return "(no guesses yet)";
        }
        String board = "";
        for (int i = 0; i < guesses.size(); i++) {
            board += (i + 1) + ". " + formatResult(guesses.get(i), results.get(i));
            if (i < guesses.size() - 1) {
                board += "\n";
            }
        }
        return board;
    }

    /**
     * Returns true if text is non-null and is exactly WORD_LENGTH
     * characters, each one a letter a-z or A-Z.
     *
     * The characters are checked BEFORE converting to upper case, because
     * upper-casing can change a string's length (the German letter "ß"
     * becomes "SS"), which would let a one-character input slip through.
     */
    private static boolean isValidLetters(String text) {
        if (text == null || text.length() != WORD_LENGTH) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isLetter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            if (!isLetter) {
                return false;
            }
        }
        return true;
    }
}
