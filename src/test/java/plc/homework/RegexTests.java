package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Long URL", "emailaccount@ufl.abcd.edu", true),
                Arguments.of("Periods", "email..account@aol.com", true),
                Arguments.of("Underscores", "email__account@aol.com", true),
                Arguments.of("Hyphens and Tildes", "emailaccount@a~o~l.u-f-l.edu", true),
                Arguments.of("Digits in Subdomain", "emailaccount@1234.com", true),

                Arguments.of("Short TLD", "emailaccount@ufl.lc", false),
                Arguments.of("Long TLD", "emailaccount@ufl.eduu", false),
                Arguments.of("Small Handle", "a@gmail.com", false),
                Arguments.of("Symbol in Subdomain", "emailaccount@u!fl.edu", false),
                Arguments.of("Multiple at Symbols", "emailaccount@ufl@gmail.com", false),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("13 Symbols", "%%^^$$##@@!!@", true),
                Arguments.of("13 Numbers", "1234567891234", true),
                Arguments.of("19 Characters", "qqwweerrttyyuuiioop", true),

                Arguments.of("5 Characters", "5five", false),
                Arguments.of("10 Characters", "qqwweerrtt", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("20 Characters", "qqwweerrttyyuuiioopp", false),
                Arguments.of("Empty String", "", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Numbers", "['1','2','3']", true),
                Arguments.of("Symbols", "['!','@','#']", true),
                Arguments.of("Mixed Format", "['1','@','a']", true),
                Arguments.of("Escape Sequences", "['\\b','\\n','\\t']", true),

                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("No Quotes", "[a,b,c]", false),
                Arguments.of("Some Quotes", "['a',b,'c']", false),
                Arguments.of("Newline Character", "['\n']", false),
                Arguments.of("Long Quote", "['a,b,c']", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("Long Whole", "48219472903.25", true),
                Arguments.of("Long Mantissa", "3.141592655359", true),
                Arguments.of("Long Both", "19473626483.47291284628", true),
                Arguments.of("Less Than One", "0.125", true),
                Arguments.of("Ending 0s", "2.12500", true),
                Arguments.of("Negative", "-4.5", true),

                Arguments.of("Multiple Decimals", "19.24.56", false),
                Arguments.of("Non-digit Characters", "2637erf.48hd", false),
                Arguments.of("Symbols", "!#@.657", false),
                Arguments.of("Missing Mantissa", "923.", false),
                Arguments.of("Missing Whole", ".38272", false),
                Arguments.of("Leading 0s", "00125.678", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
            Arguments.of("Empty String", "\"\"", true),
            Arguments.of("Punctuated String", "\"Hello::;;!!..\"", true),
            Arguments.of("Numbers", "\"38428897982\"", true),
            Arguments.of("Symbols", "\"#$%@!\"", true),
            Arguments.of("Multiple Quotes", "\"Hel\"lo\"", true),
            Arguments.of("Lots of Escapes", "\"\\n\\b\\t\\r\"", true),
            Arguments.of("Spaces", "\"Hello, this is a test string\"", true),
            Arguments.of("Escape, then words", "\"\\bHello there\"", true),
            Arguments.of("Surrounding Spaces", "\"   Hello   \"", true),

            Arguments.of("Missing Quote Begin", "Hello\"", false),
            Arguments.of("Missing Quote End", "\"Hello", false),
            Arguments.of("Invalid Escape", "\"\\descape\"", false),
            Arguments.of("Several Invalid Escapes", "\"\\e\\x\\q\"", false),
            Arguments.of("Good, Good, Bad", "\"\\b\\n\\r\\q\"", false),
            Arguments.of("Wrong Quote Location", "\"Hel\"lo", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
