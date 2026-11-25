# Two-Letter Wordle

A console version of Wordle in Java, where the secret word is one of the 107 legal two-letter Scrabble words from the NASPA Word List (NWL). The player gets 6 guesses. After each guess the program shows which letters are in the right spot, which are in the word but in the wrong spot, and which are not in the word.

Iterations 2 and 3 add a Battleship-style hint. When neither guessed letter is in the word, the one closest in the alphabet to a letter of the secret word is marked, and a message appears when it is within 1 or 3 letters.

This project was built with generative AI (Claude) as a coding collaborator, as part of a design → build → test → learn assignment.

## How to play

- Enter any two letters A–Z. Guesses do not have to be real words, and case doesn't matter (`qi`, `Qi` and `QI` are the same).
- Feedback for each letter:

  | Display | Meaning |
  |---|---|
  | `[Q]` | right letter, right spot |
  | `(Q)` | letter is in the word, wrong spot |
  | `<Q>` | not in the word, but the closest of your letters (in A–Z order) to a letter of the secret word |
  | `-Q-` | letter is not in the word |

- `<Q>` appears **only when neither letter of the guess is in the word**. If either letter is `[Q]` or `(Q)`, the other letter is just `-Q-`.
- A `<Q>` letter also gets a message when it is near:

  | Alphabet distance | Message |
  |---|---|
  | 1 | `letter Q is so close!` |
  | 2–3 | `letter Q is almost there` |
  | 4 or more | no message |

  Example with secret word `NO`:

  | Guess | Board | Message | Why |
  |---|---|---|---|
  | `MZ` | `<M> -Z-` | `letter M is so close!` | neither letter is in the word; M is 1 from N |
  | `AO` | `-A- [O]` | none | O is in the word, so closeness is off |
  | `LR` | `<L> -R-` | `letter L is almost there` | neither letter is in the word; L is 2 from N, R is 3 from O |
  | `NO` | `[N] [O]` | none | solved |
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
| `Word.java` | Holds the secret word for one game and all of the game logic: loading the dictionary, picking a random word, storing the secret's alphabet indexes, validating guesses, scoring guesses, closeness hints, and tracking the game state. |
| `TwoLetterWordle.java` | Client class. Creates a new `Word` for each game, reads input, prints the board and hints, and handles STOP and play-again. |
| `WordTester.java` | Separate testing driver for `Word`. |
| `words.txt` | The 107 NWL two-letter words, one per line, upper case, sorted. |

## Design

### `Word` class

- **Two constructors**
  - `Word(File dictionaryFile)` picks a random secret word from the file.
  - `Word(String secret)` uses a specific secret word, for testing and debugging.
- **`storeSecretIndexes()`** (called by the constructor) stores each secret letter's alphabet index in an `int[]`, with A=0 through Z=25. For example, `BG` is stored as `{1, 6}`. `getSecretIndexes()` returns a copy, for debugging.
- **`loadDictionary(File)`** reads the file into a `TreeSet<String>`. It trims spaces, skips blank lines, and converts words to upper case. The set drops duplicates automatically. A malformed line throws an `IllegalArgumentException` that names the line number.
- **`pickRandomWord(Set, Random)`** copies the set into an `ArrayList` so a word can be picked by index. Passing in the `Random` object means a test can use a fixed seed to reproduce the same word.
- **`normalizeGuess(String)`** checks that a guess is exactly two letters A–Z and returns it in upper case. Otherwise it throws `IllegalArgumentException` with the "Improper input" message, which the client catches before prompting again.
- **`evaluate(String)`** returns the result for each letter of a guess. **`getHints(String)`** returns the closeness messages for a guess. **`makeGuess(String)`** scores a guess and records it.
- **State methods:** `isSolved()`, `isGameOver()`, `getGuessCount()`, `getGuessesRemaining()`, `getSecret()`.
- **`toString()`** returns the board, one numbered line per guess, including `<Q>` marks.

### Scoring (private `score` method)

`evaluate` and `getHints` both call one private `score` method, so the marks and the messages can never disagree. `score` works in four passes over the guess and secret indexes, using a `boolean[]` to record which secret positions have already been matched:

1. **CORRECT:** right letter in the right position. That secret position is used up.
2. **PRESENT:** for each remaining guess letter, look for an unused secret position with the same letter. If one is found, the guess letter is PRESENT and that position is used up.
3. **ABSENT:** every other guess letter is ABSENT. Record its alphabet distance to the nearest secret letter.
4. **CLOSEST:** only if no guess letter was CORRECT or PRESENT, the letter(s) with the smallest distance become CLOSEST.

Two rules follow from this:

- **Repeated letters are scored correctly.** With secret `BA` and guess `AA`, the second `A` is CORRECT and uses up the secret's only `A`. The first `A` is therefore not PRESENT; it is ABSENT.
- **A hit switches closeness off.** With secret `BG` and guess `BH`, `B` is CORRECT, so `H` is shown as `-H-` even though it is 1 from `G`. Closeness is a tool for the "searching for a hit" stage. Once a letter is found, the player switches to Wordle-style guessing.

### Data structures used

| Structure | Where | Why |
|---|---|---|
| `TreeSet<String>` | dictionary | removes duplicates and keeps words sorted |
| `ArrayList<String>` | random pick, guess history | indexed access, guesses kept in order |
| `int[]` | secret and guess alphabet indexes | position-by-position comparison and distance math |
| `boolean[]` | scoring | records which secret positions are already matched |
| `HashSet<Character>` | `getHints` | gives one message per letter when a guess repeats a letter |
| `enum LetterResult` | feedback | four named results instead of magic strings or numbers |

Iteration 1 counted unmatched letters with a `HashMap<Character, Integer>`. A count says how many copies of a letter remain, but not *which positions* are still unfound. Iteration 2's closeness rule needed the positions, so the map was replaced with the `boolean[]`. Iteration 3's rule no longer needs the positions, because closeness now only runs when nothing was matched. The `boolean[]` was kept because it is still the simplest way to score repeated letters.

## Design decisions

### Iteration 1

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

### Iteration 2: Battleship closeness

The trigger for this iteration came from play-testing. Players often guess two random letters just to find a hit, and the game only becomes interesting once one letter is found. Closeness hints make the misses useful.

| Question | Decision |
|---|---|
| Distance measure | Alphabet index difference, A=0 … Z=25. |
| Wrap around (Z next to A)? | No. A Battleship grid has edges. |
| Measured against which secret letters? | Only secret letters not already found by CORRECT or PRESENT. |
| Which secret position? | Either one. A letter is compared with every unfound secret letter, and the smallest distance counts. |
| Mark `<Q>` only when near? | No. The closest miss is always marked, as specified. Messages appear only at distance 3 or less. |
| Ties | All tied letters are marked, since choosing one would be arbitrary. Each distinct letter gets one message. |
| Message wording | `letter Q is so close!` (distance 1), `letter Q is almost there` (2–3). |

### Iteration 3: closeness only on a full miss

Play-testing iteration 2 showed two problems:

- When one letter was a hit and the other a miss, the miss was always marked `<Q>`, even when it was far away. The mark carried no information.
- The hint mixed two stages of play: searching for a first hit, and narrowing down once a hit is found.

| Question | Decision |
|---|---|
| When does closeness apply? | Only when neither guessed letter is in the word. |
| Measured against which secret letters? | All of them. Since nothing in the guess was found, no secret letter is excluded. |
| Memory across guesses? | None. Each guess is judged on its own (see limitations). |

### Edge cases found while building

- **Upper-casing can change a string's length.** The German letter `ß` becomes `SS` when upper-cased. Checking the length *after* upper-casing would let a one-character input through as the guess `SS`. The validator therefore checks the original characters before converting.
- **Locale.** `toUpperCase()` uses the computer's language setting by default. In Turkish, `i` upper-cases to a dotted `İ`, so `qi` would not match `QI`. The code uses `toUpperCase(Locale.ROOT)` to avoid this.

## Known limitations / ideas for the next iteration

- Closeness doesn't remember earlier guesses. If the secret is `NO` and the player already found `O`, a full-miss guess like `PZ` still says `letter P is so close!`, because P is 1 from O. The hint is accurate, but it points at a letter the player already has. One fix is to measure only against secret letters not found on any earlier guess.
- When both letters miss and both are far away, the closer one is still marked `<Q>` with no message. This tells the player which of the two letters is nearer, but not by how much.
- The hint doesn't say whether the secret letter is earlier or later in the alphabet. Adding a direction (e.g. "letter A is so close! (go later)") would make the game more like a guided search.
- The word length is a constant (`Word.WORD_LENGTH = 2`). A different word length would also need a new error message and a new word file.
- Possible additions:
  - show the letters already ruled out
  - warn when a guess is repeated
  - an option to require real words as guesses

## Word list source

The two-letter NWL2020 list (107 words), cross-checked against Merriam-Webster's Scrabble two-letter word list and WordFinder's NWL/OSPD list.
