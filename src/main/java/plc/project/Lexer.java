package plc.project;

import java.util.ArrayList;
import java.util.List;

import static plc.project.Token.Type.*;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokenList = new ArrayList<Token>();
        while (chars.has(0)) {  // While there are characters in the CharStream:
            if (match("[\\s]") || match("[\b\n\t\r]")) {  // Ignore whitespace
                chars.skip();
                continue;
            }
            Token newToken = lexToken();
            tokenList.add(newToken);
        }
        return tokenList;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        Token newToken;
        if (peek("[A-Za-z@]")) {
            newToken = lexIdentifier();
        }
        else if (peek("(-|[0-9])")) {
            newToken = lexNumber();
        }
        else if (peek("\\'")) {
            newToken = lexCharacter();
        }
        else if (peek("\\\"")) {
            newToken = lexString();
        }
        else {
            newToken = lexOperator();
        }
        return newToken;
    }

    public Token lexIdentifier() {
        chars.advance();    // Advance past the first character
        while (peek("[^\\s]")) {  // While the next character is not a whitespace:
            if (!match("[A-Za-z0-9_-]")) {  // Keep matching to characters of the identifier until an unsupported character is reached
                return chars.emit(IDENTIFIER);
            }
        }
        return chars.emit(IDENTIFIER);  // Emit the identifier if the input ends while lexing
    }

    public Token lexNumber() {
        boolean zero = false;   // Used to track whether or not the number starts with a zero
        if (match("0")) {
            zero = true;
        }
        else if (match("-")) {
            if (!peek("[0-9]")) {   // The hyphen is being used as an operator
                return lexOperator();
            }
            return lexNumber(); // Call the function to lex the actual number part (after the hyphen has been consumed)
        }
        while (!zero && match("[0-9]")); // Empty body because the match function advances the CharStream
        if (peek("\\.")) {
            if (!chars.has(1) || !String.valueOf(chars.get(1)).matches("[0-9]")) {   // There are no numbers following the decimal point
                return chars.emit(INTEGER);
            }
            chars.advance();
            while (match("[0-9]")); // See above about empty body
            return chars.emit(DECIMAL);
        }
        else {
            return chars.emit(INTEGER);
        }
    }

    public Token lexCharacter() {
        chars.advance();    // Advance past the starting apostrophe
        if (match("\\\\")) {
            lexEscape();
        }
        else {
            chars.advance();
        }
        if (!match("\\'")) {
            throw new ParseException("Invalid use of character literal", chars.index);
        }
        return chars.emit(CHARACTER);
    }

    public Token lexString() {
        chars.advance();    // Advance past the starting quote
        while (!match("\\\"")) {    // While the ending quote has not been reached
            if (chars.index >= chars.input.length()) {
                throw new ParseException("Reached EOF in string literal", chars.index);
            }
            if (peek("\\n")) {
                throw new ParseException("String literal cannot span multiple lines", chars.index);
            }
            if (match("\\\\")) {
                lexEscape();
            }
            else {
                chars.advance();
            }
        }
        return chars.emit(STRING);
    }

    public void lexEscape() {   // Matches to a valid escape sequence, error if invalid
        if (!match("[bnrt'\\\"]")) {
            throw new ParseException("Invalid escape sequence", chars.index);
        }
    }

    public Token lexOperator() {    // Checks to see if the next character(s) represent a valid operator. If so, emit. If not, throw parse exception
        if (match("!")) {
            match("=");
        }
        else if (match("=")) {
            match("=");
        }
        else if (match("&")) {
            match("&");
        }
        else if (match("|")) {
            match("|");
        }
        else {
            chars.advance();
        }
        return chars.emit(OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {   // Checks to see if the next character(s) match the pattern parameter
        for (int i = 0; i < patterns.length; i++) { // Verifies that each character in chars matches the corresponding character in the pattern parameter
            if (chars.has(i) && String.valueOf(chars.get(i)).matches(patterns[i])) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        if (peek(patterns)) {   // If the upcoming character(s) match the pattern:
            for (int i = 0; i < patterns.length; i++) { // Advance chars the length of the pattern -- the read characters can now be emitted
                chars.advance();
            }
            return true;
        }
        return false;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
