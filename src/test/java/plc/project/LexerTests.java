package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Less than 0", "0.456", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "1. print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.OPERATOR, ".", 1),
                        new Token(Token.Type.IDENTIFIER, "print", 3),
                        new Token(Token.Type.OPERATOR, "(", 8),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 9),
                        new Token(Token.Type.OPERATOR, ")", 24),
                        new Token(Token.Type.OPERATOR, ";", 25)
                )),
                Arguments.of("Example 4", "System.out.print(\"Test\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "System", 0),
                        new Token(Token.Type.OPERATOR, ".", 6),
                        new Token(Token.Type.IDENTIFIER, "out", 7),
                        new Token(Token.Type.OPERATOR, ".", 10),
                        new Token(Token.Type.IDENTIFIER, "print", 11),
                        new Token(Token.Type.OPERATOR, "(", 16),
                        new Token(Token.Type.STRING, "\"Test\"", 17),
                        new Token(Token.Type.OPERATOR, ")", 23),
                        new Token(Token.Type.OPERATOR, ";", 24)
                )),
                Arguments.of("Example 5", "return this.price;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "return", 0),
                        new Token(Token.Type.IDENTIFIER, "this", 7),
                        new Token(Token.Type.OPERATOR, ".", 11),
                        new Token(Token.Type.IDENTIFIER, "price", 12),
                        new Token(Token.Type.OPERATOR, ";", 17)
                )),
                Arguments.of("Example 6", "@Override public int number() { return this.number; }", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "@Override", 0),
                        new Token(Token.Type.IDENTIFIER, "public", 10),
                        new Token(Token.Type.IDENTIFIER, "int", 17),
                        new Token(Token.Type.IDENTIFIER, "number", 21),
                        new Token(Token.Type.OPERATOR, "(", 27),
                        new Token(Token.Type.OPERATOR, ")", 28),
                        new Token(Token.Type.OPERATOR, "{", 30),
                        new Token(Token.Type.IDENTIFIER, "return", 32),
                        new Token(Token.Type.IDENTIFIER, "this", 39),
                        new Token(Token.Type.OPERATOR, ".", 43),
                        new Token(Token.Type.IDENTIFIER, "number", 44),
                        new Token(Token.Type.OPERATOR, ";", 50),
                        new Token(Token.Type.OPERATOR, "}", 52)
                )),
                Arguments.of("Example 7", "int negNum = -1;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "int", 0),
                        new Token(Token.Type.IDENTIFIER, "negNum", 4),
                        new Token(Token.Type.OPERATOR, "=", 11),
                        new Token(Token.Type.INTEGER, "-1", 13),
                        new Token(Token.Type.OPERATOR, ";", 15)
                )),
                Arguments.of("Example 8", "char letter = \'a\';", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "char", 0),
                        new Token(Token.Type.IDENTIFIER, "letter", 5),
                        new Token(Token.Type.OPERATOR, "=", 12),
                        new Token(Token.Type.CHARACTER, "\'a\'", 14),
                        new Token(Token.Type.OPERATOR, ";", 17)
                )),
                Arguments.of("Example 9", "var escapeChar = \"\\t\";", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "var", 0),
                        new Token(Token.Type.IDENTIFIER, "escapeChar", 4),
                        new Token(Token.Type.OPERATOR, "=", 15),
                        new Token(Token.Type.STRING, "\"\\t\"", 17),
                        new Token(Token.Type.OPERATOR, ";", 21)
                )),
                Arguments.of("Example 10", "float myArray2 [] = {1.0, -0.25};", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "float", 0),
                        new Token(Token.Type.IDENTIFIER, "myArray2", 6),
                        new Token(Token.Type.OPERATOR, "[", 15),
                        new Token(Token.Type.OPERATOR, "]", 16),
                        new Token(Token.Type.OPERATOR, "=", 18),
                        new Token(Token.Type.OPERATOR, "{", 20),
                        new Token(Token.Type.DECIMAL, "1.0", 21),
                        new Token(Token.Type.OPERATOR, ",", 24),
                        new Token(Token.Type.DECIMAL, "-0.25", 26),
                        new Token(Token.Type.OPERATOR, "}", 31),
                        new Token(Token.Type.OPERATOR, ";", 32)
                )),
                Arguments.of("Example 11", "01", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.INTEGER, "1", 1)
                )),
                Arguments.of("Example 12", "0.", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.OPERATOR, ".", 1)
                )),
                Arguments.of("Example 13", "one\btwo", Arrays.asList( //SHOULD FAIL
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.OPERATOR, "\b", 3),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
