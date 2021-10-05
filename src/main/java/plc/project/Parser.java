package plc.project;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();

        while (peek("LIST") || peek("VAR") || peek("VAL")) {
            globals.add(parseGlobal());
        }
        while (match("FUN")) {
            functions.add(parseFunction());
        }

        if (tokens.has(0)) {    // There should be no more tokens after globals and functions have been parsed
            throw new ParseException("Unexpected token", tokens.get(0).getIndex());
        }

        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global result;

        if (match("LIST")) {
            result = parseList();
        }
        else if (match("VAR")) {
            result = parseMutable();
        }
        else  {
            match("VAL");
            result = parseImmutable();
        }
        mustMatch(";");

        return result;
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        String name;
        Ast.Expression.PlcList list;
        List<Ast.Expression> expressionList = new ArrayList<>();

        mustMatch(Token.Type.IDENTIFIER);
        name = tokens.get(-1).getLiteral();
        mustMatch("=");
        mustMatch("[");
        expressionList.add(parseExpression());  // There must be at least one expression in the list
        while (!match("]")) {
            expressionList.add(parseExpression());
            checkCommas("]");
        }
        list = new Ast.Expression.PlcList(expressionList);

        return new Ast.Global(name, true, Optional.of(list));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        String name;
        Optional<Ast.Expression> expression = Optional.empty();

        mustMatch(Token.Type.IDENTIFIER);
        name = tokens.get(-1).getLiteral();
        if (match("=")) {
            expression = Optional.of(parseExpression());
        }

        return new Ast.Global(name, true, expression);
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        String name;
        Optional<Ast.Expression> expression;

        mustMatch(Token.Type.IDENTIFIER);
        name = tokens.get(-1).getLiteral();
        mustMatch("=");
        expression = Optional.of(parseExpression());

        return new Ast.Global(name, false, expression);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Function parseFunction() throws ParseException {
        String name;
        List<String> parameters = new ArrayList<>();
        List<Ast.Statement> statements;

        mustMatch(Token.Type.IDENTIFIER);
        name = tokens.get(-1).getLiteral();
        mustMatch("(");

        while (!match(")")) {
            mustMatch(Token.Type.IDENTIFIER);
            parameters.add(tokens.get(-1).getLiteral());
            checkCommas(")");
        }

        mustMatch("DO");
        statements = parseBlock();
        mustMatch("END");

        return new Ast.Function(name, parameters, statements);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();

        while (!peek("END") && !peek("ELSE") && !peek("CASE") && !peek("DEFAULT")) {    // All the cases where a block ends
            statements.add(parseStatement());
        }

        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (match("LET")) {
            return parseDeclarationStatement();
        }
        else if (match("SWITCH")) {
            return parseSwitchStatement();
        }
        else if (match("IF")) {
            return parseIfStatement();
        }
        else if (match("WHILE")) {
            return parseWhileStatement();
        }
        else if (match("RETURN")) {
            return parseReturnStatement();
        }
        // Functionality for parsing expression and assignment statements:
        else {
            Ast.Expression expression = parseExpression();
            if (expression instanceof Ast.Expression.Access && match("=")) {    // An assignment expression
                Ast.Expression rightSide = parseExpression();
                mustMatch(";");
                return new Ast.Statement.Assignment(expression, rightSide);
            }
            else  {
                mustMatch(";");
                return new Ast.Statement.Expression(expression);
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        Optional<Ast.Expression> rightSide = Optional.empty();

        mustMatch(Token.Type.IDENTIFIER);
        String name = tokens.get(-1).getLiteral();
        if (match("=")) {   // Receiver is being initialized
            rightSide = Optional.of(parseExpression());
        }
        mustMatch(";");

        return new Ast.Statement.Declaration(name, rightSide);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        List<Ast.Statement> ifBlock;
        List<Ast.Statement> elseBlock = new ArrayList<>();

        Ast.Expression expression = parseExpression();
        mustMatch("DO");
        ifBlock = parseBlock();
        if (match("ELSE")) {
            elseBlock = parseBlock();
        }
        mustMatch("END");
        return new Ast.Statement.If(expression, ifBlock, elseBlock);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        List<Ast.Statement.Case> cases = new ArrayList<>();
        Ast.Expression firstExpression = parseExpression();

        while (match("CASE")) {
            cases.add(parseCaseStatement());
        }
        mustMatch("DEFAULT");
        cases.add(parseCaseStatement());
        mustMatch("END");

        return new Ast.Statement.Switch(firstExpression, cases);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        Optional<Ast.Expression> expression = Optional.empty();
        List<Ast.Statement> block;

        if (!tokens.get(-1).getLiteral().equals("DEFAULT")) {
            expression = Optional.of(parseExpression());
            mustMatch(":");
        }
        block = parseBlock();

        return new Ast.Statement.Case(expression, block);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        List<Ast.Statement> block;
        Ast.Expression expression = parseExpression();

        mustMatch("DO");
        block = parseBlock();
        mustMatch("END");

        return new Ast.Statement.While(expression, block);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        Ast.Expression expression = parseExpression();
        mustMatch(";");
        return new Ast.Statement.Return(expression);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression left = null;
        Ast.Expression right = null;
        left = parseComparisonExpression();
        while (match("&&") || match("||")) {
            String operator = tokens.get(-1).getLiteral();
            right = parseComparisonExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression left = null;
        Ast.Expression right = null;
        left = parseAdditiveExpression();
        while (match(">") || match("<") || match("==") || match("!=")) {
            String operator = tokens.get(-1).getLiteral();
            right = parseAdditiveExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = null;
        Ast.Expression right = null;
        left = parseMultiplicativeExpression();
        while (match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            right = parseMultiplicativeExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = null;
        Ast.Expression right = null;
        left = parsePrimaryExpression();
        while (match("*") || match("/") || match("^")) {
            String operator = tokens.get(-1).getLiteral();
            right = parsePrimaryExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        }
        else if (match("TRUE")) {
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        else if (match("FALSE")) {
            return new Ast.Expression.Literal(Boolean.FALSE);
        }
        else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.CHARACTER)) {
            String character = tokens.get(-1).getLiteral();
            character = character.substring(1, character.length() - 1);    // Remove opening and closing quotes
            // Replace all literal escape sequences with their actual value:
            character = replaceEscapes(character);
            return new Ast.Expression.Literal(character.charAt(0));
        }
        else if (match(Token.Type.STRING)) {
            String string = tokens.get(-1).getLiteral();
            string = string.substring(1, string.length() - 1);  // Remove opening and closing quotes
            // Replace all literal escape sequences with their actual value:
            string = replaceEscapes(string);
            return new Ast.Expression.Literal(string);
        }
        else if (match("(")) {
            Ast.Expression expression = parseExpression();
            mustMatch(")");
            return new Ast.Expression.Group(expression);
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if (match("[")) {
                Ast.Expression expression = parseExpression();
                mustMatch("]");
                return new Ast.Expression.Access(Optional.of(expression), name);
            }
            else if (match("(")) {
                Vector<Ast.Expression> parameterList = new Vector<>();  // Vector to collect the function arguments
                while (!match(")")) {
                    parameterList.add(parseExpression());
                    checkCommas(")");
                }
                return new Ast.Expression.Function(name, parameterList);
            }
            else {
                return new Ast.Expression.Access(Optional.empty(), name);
            }
        }
        try {
            throw new ParseException("Expected an expression", tokens.get(0).getIndex());
        }
        catch (IndexOutOfBoundsException e) {
            throw new ParseException("Expected an expression", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
    }

    private String replaceEscapes(String input) {
        input = input.replace("\\b", "\b");
        input = input.replace("\\n", "\n");
        input = input.replace("\\r", "\r");
        input = input.replace("\\t", "\t");
        input = input.replace("\\\"", "\"");
        input = input.replace("\\'", "'");
        input = input.replace("\\\\", "\\");
        return input;
    }

    private void mustMatch(Object pattern) throws ParseException {
         if (!match(pattern)) {
             try {
                 throw new ParseException("Expected a '" + pattern.toString() + "'", tokens.get(0).getIndex());
             }
             catch (IndexOutOfBoundsException e) {
                 throw new ParseException("Expected a '" + pattern.toString() + "'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
             }
         }
    }

    private void checkCommas(Object pattern) {
        if (match(",")) {
            if (peek(pattern) || match(",")) {
                throw new ParseException("Unexpected comma", tokens.get(0).getIndex());
            }
        }
        else {
            if (!peek(pattern)) {
                throw new ParseException("Expected a '" + pattern.toString() + "'", tokens.get(0).getIndex());
            }
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object:" + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
