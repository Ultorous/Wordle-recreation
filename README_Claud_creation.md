# Two-Letter Wordle

A console version of Wordle in Java, where the secret word is one of the 107 legal two-letter Scrabble words from the NASPA Word List (NWL). The player gets 6 guesses. After each guess the program shows which letters are in the right spot, which are in the word but in the wrong spot, and which are not in the word.

This project was built with generative AI (Claude) as a coding collaborator, as part of a design → build → test → learn assignment.

## How to play

- Enter any two letters A–Z. Guesses do not have to be real words, and case doesn't matter (`qi`, `Qi` and `QI` are the same).
- Feedback for each letter:

  | Display | Meaning |
  |---|---|
  | `[Q]` | right letter, right spot |
  | `(Q)` | letter is in the word, wrong spot |
  | `-Q-` | letter is not in the word |

- Type `STOP` (any case) at a guess prompt to quit. The secret word is revealed.
- Invalid input (wrong length, an empty line, spaces, or anything other than A–Z) prints `Improper input. Please enter a two letter guess` and does **not** use up a guess.
- When a game ends with a win or a loss, the secret word is shown and you are asked whether to play again.

## Running in jGRASP

1. Put all of the files in the same folder. `words.txt` must sit next to the `.java` files, because jGRASP runs programs from the source file's folder.
2. Open `TwoLetterWordle.java` and compile it (this also compiles `Word.java`).
3. Run `TwoLetterWordle` to play.
4. To run the tests, open `WordTester.java`, then compile and run it. Every check prints `PASS` or `FAIL`, followed by a summary.

The code uses only Java 8 features, so it compiles on any JDK from 8 onward.

## Files

| File | Purpose |
|---|---|
| `Word.java` | Holds the secret word for one game and all of the game logic: loading the dictionary, picking a random word, validating guesses, scoring guesses, and tracking the game state. |
| `TwoLetterWordle.java` | Client class. Creates a new `Word` for each game, reads input, prints the board, and handles STOP and play-again. |
| `WordTester.java` | Separate testing driver for `Word`. |
| `words.txt` | The 107 NWL two-letter words, one per line, upper case, sorted. |

## Design

### `Word` class

- **Two constructors**
  - `Word(File dictionaryFile)` picks a random secret word from the file.
  - `Word(String secret)` uses a specific secret word, for testing and debugging.
- **`loadDictionary(File)`** reads the file into a `TreeSet<String>`. It trims spaces, skips blank lines, and converts words to upper case. The set drops duplicates automatically. A malformed line (wrong length or a non-letter) throws an `IllegalArgumentException` that names the line number, so a mistake in the file is caught immediately instead of producing an impossible secret word.
- **`pickRandomWord(Set, Random)`** copies the set into an `ArrayList` so a word can be picked by index (a `Set` has no index). Passing in the `Random` object means a test can use a fixed seed to reproduce the same word.
- **`normalizeGuess(String)`** checks that a guess is exactly two letters A–Z and returns it in upper case. Otherwise it throws `IllegalArgumentException` with the "Improper input" message, and the client catches it and prompts again.
- **`evaluate(String)`** scores a guess without recording it. **`makeGuess(String)`** scores a guess and records it.
- **State methods:** `isSolved()`, `isGameOver()`, `getGuessCount()`, `getGuessesRemaining()`, `getSecret()`.
- **`toString()`** returns the board, one numbered line per guess.

### Scoring repeated letters

A naive approach — "green if same position, else yellow if the letter appears anywhere in the secret" — gives wrong answers when a letter repeats. For example, with secret `BA` and guess `AA`, the naive approach marks the first `A` yellow. But the secret's only `A` is already matched by the second (green) `A`, so the first `A` should be gray.

`evaluate` uses two passes and a `Map<Character, Integer>`:

1. Mark the exact matches as correct, and count the secret's letters that were not matched.
2. For each remaining guess letter, mark it "in the word" only if an unmatched copy is still left in the count, and subtract one; otherwise mark it "not in the word".

`WordTester` checks this with the two-letter words that contain repeated letters (`AA`, `MM`) and guesses like `MM` and `AA`. As a check on the tests themselves, swapping in the naive approach makes 5 of the feedback tests fail.

### Data structures used

| Structure | Where | Why |
|---|---|---|
| `TreeSet<String>` | dictionary | removes duplicates and keeps words sorted |
| `ArrayList<String>` | random pick, guess history | indexed access, guesses kept in order |
| `HashMap<Character, Integer>` | `evaluate` | counts unmatched letters for repeated-letter scoring |
| `enum LetterResult` | feedback | three named results instead of magic strings or numbers |

## Design decisions (iteration 1)

| Question | Decision |
|---|---|
| Which word list? | NWL (North American), 107 words, instead of Collins (127). |
| Must guesses be real words? | No. Any two letters A–Z are allowed, because many NWL two-letter words are obscure. |
| Guess limit | 6, like Wordle. |
| Quitting | `STOP`, case insensitive. It can't collide with a guess because it is 4 letters. |
| Invalid input | Throws `IllegalArgumentException`. The client catches it, prints the message, and reprompts without using a guess. |
| Spaces in input | Rejected, not trimmed (`" QI"` is invalid). |
| End of game | Reveal the word and ask to play again. STOP ends the program. |
| Display | Plain text markers instead of colors, so it works in every console, including jGRASP's. |
| Dictionary file | Written by hand. The loader also cleans and validates it, in case it is edited later. |

### Edge cases found while building

- **Upper-casing can change a string's length.** The German letter `ß` becomes `SS` when upper-cased. Checking the length *after* upper-casing would let a one-character input through as the guess `SS`. The validator therefore checks the original characters before converting.
- **Locale.** `toUpperCase()` uses the computer's language setting by default. In Turkish, `i` upper-cases to a dotted `İ`, so `qi` would not match `QI`. The code uses `toUpperCase(Locale.ROOT)` to avoid this.

## Known limitations / ideas for the next iteration

- The word length is a constant (`Word.WORD_LENGTH = 2`). A different word length would also need a new error message and a new word file.
- Possible additions:
  - show the letters already ruled out
  - warn when a guess is repeated
  - show the definition of the secret word at the end
  - an option to require real words as guesses
  - an option to switch to the Collins list

## Word list source

The two-letter NWL2020 list (107 words), cross-checked against Merriam-Webster's Scrabble two-letter word list and WordFinder's NWL/OSPD list.
