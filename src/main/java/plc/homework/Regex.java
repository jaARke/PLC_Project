package plc.homework;

import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regexes as needed.
 */
public class Regex {

    public static final Pattern
            EMAIL = Pattern.compile("[A-Za-z0-9._]{2,}@[A-Za-z0-9~]+\\.([A-Za-z0-9-]+\\.)*[a-z]{3}"),
            ODD_STRINGS = Pattern.compile("^([^\\\\]|(?:\\\\[bnrt'\"]))(([^\\\\]|(?:\\\\[bnrt'\"])){2}){5,9}$"),
            CHARACTER_LIST = Pattern.compile("\\[(\\'(.|\\\\[bnrt'\"])\\')?(, ?\\'(.|\\\\[bnrt'\"])\\')*]"),
            DECIMAL = Pattern.compile("^-?(0(?=\\.)|[1-9])[0-9]*\\.[0-9]+$"),
            STRING = Pattern.compile("^\\\"(\\\\(?=[bnrt'\"\\\\])|[^\\\\])*\\\"$");
}
