package plc.examples;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


class TestRE {
  public static void main(String args[]) {

     System.out.println();
     
    /**
     * from the javadocs of the class String for version 1.8:
     * 
     * boolean matches(String regex)
     *   Tells whether or not this string matches the given 
     *   regular expression.
     */

    print_dashes(65);
    System.out.println("12345 => does it match \\d => " + "12345".matches("\\d"));
    System.out.println("12345 => does it match \\d => " + "12345".matches("\\d+"));
    System.out.println("a2345 => does it match \\d => " + "a2345".matches("\\d+"));

    /**
     * Consider and solve these problems on your own:
     *  -> How would you find each of the individual words in the student_record?
     *  -> What are the delimiters used in the record?
     *  -> What do the delimiters separate?
     *  -> How would you extract the delimited information?
     */

    /**
     *   Sample Student Record:  Smith:Bob,DAS,Smith:Jane;Doe:John
     *
     *   Format (for this specific case):
     *     [0]:  Student
     *    [10]:  Major 
     *    [14]:  Committee
     * 
     */

    //                                  0         1         2         3
    //                                  012345678901234567890123456789012
    String student_record = new String("Smith:Bob,DAS,Smith:Jane;Doe:John");

    // use a the class String to split up parts of the record
    print_dashes(65);
    System.out.println(student_record + " <- comma split ->");
    print_array(student_record.split(","));

    // setup REs to extract information from the record
    Pattern pattern;
    Matcher match;

    // look for Smith
    print_dashes(65);
    pattern = Pattern.compile("Smith");
    match = pattern.matcher(student_record);
    print_matches(match);

    // look for Smith at the start of the record,
    // what is the significance of Smith being at the start of the record?
    print_dashes(65);
    pattern = Pattern.compile("^Smith");
    match = pattern.matcher(student_record);
    print_matches(match);

    // Look for Jane anywhere in the record -- should find one occurrence
    print_dashes(65);
    pattern = pattern.compile("Jane");
    match = pattern.matcher(student_record);
    print_matches(match);

    // Look for Jane at the start of the record -- should not find any occurrences
    print_dashes(65);
    pattern = pattern.compile("^Jane");
    match = pattern.matcher(student_record);
    print_matches(match);
     
     System.out.println();
  }

  // print a list
  public static void print_array( String array[] ) {
    for ( int i = 0; i < array.length; i++ )  {
      System.out.println( "  array[" + i + "]:  " + array[i] );
    }
  }

  // formatting and spacing
  public static void print_dashes(int number) {
    for (int i = 0; i < number; i++) {
      System.out.print("-");
    }
    System.out.println();
  }

  public static void print_matches(Matcher match) {
    boolean noMatch = true;
    while (match.find()) {
      noMatch = false;
      System.out.println(match.group() + " found at index " + match.start());
	}
    if (noMatch) {
      System.out.println("No matches found.");
    }
  }
}



