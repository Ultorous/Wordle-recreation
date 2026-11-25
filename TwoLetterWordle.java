import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Client program for Two-Letter Wordle. Creates a new Word object for
 * each game, reads guesses from the console, and shows the results.
 *
 * The player has 6 guesses to find a secret two-letter word chosen at
 * random from words.txt (the NWL Scrabble two-letter words). Typing STOP
 * (any case) at a guess prompt ends the program. After each guess the
 * board is printed, followed by any closeness hints for that guess.
 *
 * words.txt must be in the program's working folder. In jGRASP, that is
 * the same folder as this .java file.
 */
public class TwoLetterWordle {

    public static final String DICTIONARY_FILE = "words.txt";
    public static final String STOP_COMMAND = "STOP";

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);
        printIntro();

        boolean keepPlaying = true;
        while (keepPlaying) {
            Word word;
            try {
                word = new Word(new File(DICTIONARY_FILE));
            } catch (FileNotFoundException e) {
                System.out.println("Could not find " + DICTIONARY_FILE
                        + ". Put it in the same folder as the .java files.");
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Problem with " + DICTIONARY_FILE + ": "
                        + e.getMessage());
                return;
            }

            boolean playerStopped = playOneGame(console, word);
            if (playerStopped) {
                keepPlaying = false;
            } else {
                keepPlaying = askToPlayAgain(console);
            }
        }
        System.out.println("Thanks for playing!");
    }

    /** Prints the rules and how to read the feedback. */
    public static void printIntro() {
        System.out.println("Welcome to Two-Letter Wordle!");
        System.out.println("Guess the secret two-letter Scrabble word in "
                + Word.MAX_GUESSES + " tries.");
        System.out.println("Any two letters A-Z are allowed as a guess.");
        System.out.println();
        System.out.println("How to read the feedback:");
        System.out.println("  [Q]  right letter, right spot");
        System.out.println("  (Q)  letter is in the word, wrong spot");
        System.out.println("  <Q>  not in the word, but the closest of your");
        System.out.println("       letters to a letter in the word (A-Z order).");
        System.out.println("       Only shown when neither letter is in the word.");
        System.out.println("  -Q-  letter is not in the word");
        System.out.println("A message tells you when a <Q> letter is within");
        System.out.println("1 letter (so close!) or 3 letters (almost there.).");
        System.out.println();
    }

    /**
     * Plays one game until the word is guessed, the guesses run out,
     * or the player types STOP.
     *
     * @return true if the player typed STOP (or input ended),
     *         false if the game finished normally
     */
    public static boolean playOneGame(Scanner console, Word word) {
        while (!word.isGameOver()) {
            System.out.println("Type " + STOP_COMMAND + " to quit.");
            if (word.getGuessCount() == 0) {
                System.out.print("Please enter a guess: ");
            } else {
                System.out.print("Please enter another guess: ");
            }

            if (!console.hasNextLine()) {   // input ended (e.g. Ctrl+D)
                System.out.println();
                System.out.println("The word was " + word.getSecret() + ".");
                return true;
            }
            String input = console.nextLine();

            if (input.equalsIgnoreCase(STOP_COMMAND)) {
                System.out.println("The word was " + word.getSecret() + ".");
                return true;
            }

            try {
                word.makeGuess(input);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                System.out.println();
                continue;   // invalid input does not use up a guess
            }

            System.out.println();
            System.out.println(word);
            for (String hint : word.getHints(input)) {
                System.out.println(hint);
            }
            System.out.println("Guesses remaining: " + word.getGuessesRemaining());
            System.out.println();
        }

        if (word.isSolved()) {
            int count = word.getGuessCount();
            System.out.println("You got it in " + count
                    + (count == 1 ? " guess!" : " guesses!")
                    + " The word was " + word.getSecret() + ".");
        } else {
            System.out.println("Out of guesses. The word was "
                    + word.getSecret() + ".");
        }
        return false;
    }

    /**
     * Asks whether to play again until the player answers Y or N.
     * STOP is also accepted as no.
     *
     * @return true for yes, false for no
     */
    public static boolean askToPlayAgain(Scanner console) {
        while (true) {
            System.out.print("Play again? (Y/N): ");
            if (!console.hasNextLine()) {
                System.out.println();
                return false;
            }
            String answer = console.nextLine().trim();
            if (answer.equalsIgnoreCase("Y") || answer.equalsIgnoreCase("YES")) {
                System.out.println();
                return true;
            }
            if (answer.equalsIgnoreCase("N") || answer.equalsIgnoreCase("NO")
                    || answer.equalsIgnoreCase(STOP_COMMAND)) {
                return false;
            }
            System.out.println("Please enter Y or N.");
        }
    }
}
