package plc.project;

import java.io.PrintWriter;
import java.util.Optional;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // Print the class header
        print("public class Main {");
        // Print the global variables using indentation
        if (ast.getGlobals().size() != 0) { // Otherwise, the single indent is taken care of below
            newline(0);
        }
        indent++;
        for (int i = 0; i < ast.getGlobals().size(); i++) {
            newline(indent);
            visit(ast.getGlobals().get(i));
        }
        newline(0);
        // Print the java entry point and invocation of OUR main function
        newline(indent);
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);
        print("System.exit(new Main().main());");
        indent--;
        newline(indent);
        print("}");
        // Print the functions using indentation
        if (ast.getFunctions().size() != 0) {   // Otherwise, the single indent is taken care of below
            newline(0);
        }
        for (int i = 0; i < ast.getFunctions().size(); i++) {
            newline(indent);
            visit(ast.getFunctions().get(i));
        }
        newline(0);
        // Print the closing parenthesis for the Main class
        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        // Check if the final keyword should be printed
        if (!ast.getMutable()) {
            print("final ");
        }
        // Print the variable's java type
        print(ast.getVariable().getType().getJvmName());
        // Print a pair of brackets if the variable is a list
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList) {
            print("[]");
        }
        // Print the name of the variable
        print(" " + ast.getName());
        // Print the value of the variable, if it is present
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        // Print the function return type, name, and opening parameter list parenthesis
        print(ast.getFunction().getReturnType().getJvmName() + " " + ast.getName() + "(");
        // Print the list of parameters (typename name, typename name, etc.)
        for (int i = 0; i < ast.getParameters().size(); i++) {
            print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName() + " ");
            print(ast.getParameterTypeNames().get(i));
            // If it is not the last parameter, print a comma and then a space
            if (i != ast.getParameters().size() - 1) {
                print(", ");
            }
        }
        // Print the closing parenthesis for the parameter list and the opening brace for the function itself
        print(") {");
        // Visit each of the function's statements using more indentation
        indent++;
        for (int i = 0; i < ast.getStatements().size(); i++) {
            newline(indent);
            visit(ast.getStatements().get(i));
        }
        // Print a new, less indented line to place the closing brace
        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        // Generate the stored expression
        visit(ast.getExpression());
        // Print the ending semicolon
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        // Discern the variable's typename and name
        print(ast.getVariable().getType().getJvmName() + " " + ast.getName());
        // Print the initialization value, if present
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        // Print the ending semicolon
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // Visit the receiver expression and print an equal sign
        visit(ast.getReceiver());
        print(" = ");
        // Visit the value expression and print a semicolon
        visit(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        // Print the statement header with its condition expression
        print("if (");
        visit(ast.getCondition());
        print(") {");
        // Increase the indent and generate the THEN statements
        indent++;
        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            newline(indent);
            visit(ast.getThenStatements().get(i));
        }
        // Print the closing brace for the THEN block
        indent--;
        newline(indent);
        print("}");
        // Check if there is an ELSE block
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            // Increase the indent and generate the ELSE statements
            indent++;
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                newline(indent);
                visit(ast.getElseStatements().get(i));
            }
            // Print the closing brace for the ELSE block
            indent--;
            newline(indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        // Print the statement header with its condition expression
        print("switch (");
        visit(ast.getCondition());
        print(") {");
        // Increase the indent and generate the CASE statements
        indent++;
        for (int i = 0; i < ast.getCases().size(); i++) {
            newline(indent);
            visit(ast.getCases().get(i));
        }
        // Decrease the indent and print the closing brace
        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        // Print the statement header with its value (if present)
        if (ast.getValue().isPresent()) {
            print("case ");
            visit(ast.getValue().get());
            print(":");
        }
        else {
            print("default:");
        }
        // Increase the indent and generate the CASE's statements
        indent++;
        for (int i = 0; i < ast.getStatements().size(); i++) {
            newline(indent);
            visit(ast.getStatements().get(i));
        }
        // Decrease the indent
        indent--;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        // Print the statement header with its condition expression
        print("while (");
        visit(ast.getCondition());
        print(") {");
        // Increase the indent and generate the WHILE statements, if present
        if (ast.getStatements().size() != 0) {
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                visit(ast.getStatements().get(i));
            }
            // Decrease the indent
            indent--;
            newline(indent);
        }
        // Print the closing brace
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        // Print the expression header
        print("return ");
        // Generate the return expression's value
        visit(ast.getValue());
        // Print the closing semicolon
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        // If it is a string, contain it in quotes
        if (ast.getLiteral() instanceof String) {
            print("\"" + ast.getLiteral() + "\"");
        }
        // If it is a char, contain it in quotes
        else if (ast.getLiteral() instanceof Character) {
            print("'" + ast.getLiteral() + "'");
        }
        // Otherwise, just print the value
        else {
            print(ast.getLiteral().toString());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        // Print the opening parenthesis
        print("(");
        // Generate the expression
        visit(ast.getExpression());
        // Print the closing parenthesis
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        // Generate power operation syntax
        if (ast.getOperator().equals("^")) {
            // Print the function call
            print("Math.pow(");
            // Generate the base
            visit(ast.getLeft());
            print(", ");
            // Generate the exponent
            visit(ast.getRight());
            print(")");
        }
        // Generate regular java syntax
        else {
            // Generate the left
            visit(ast.getLeft());
            // Print the operator
            print(" " + ast.getOperator() + " ");
            // Generate the right
            visit(ast.getRight());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        // Print the jvmname of the variable
        print(ast.getVariable().getJvmName());
        // Check if a list is being accessed
        if (ast.getOffset().isPresent()) {  // Is this a better way of checking for this? -> (ast.getVariable().getValue().getValue() instanceof Ast.Expression.PlcList)
            // Print the generated offset inside two brackets
            print("[");
            visit(ast.getOffset().get());
            print("]");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        // Print the jvmname of the function and the opening parenthesis
        print(ast.getFunction().getJvmName() + "(");
        // Generate each argument
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            // If it is not the last argument, print a comma and a space
            if (i != ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        // Print the closing parenthesis
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        // Print the opening brace
        print("{");
        // Generate each list value
        for (int i = 0; i < ast.getValues().size(); i++) {
            visit(ast.getValues().get(i));
            // If it is not the last value, print a comma and a space
            if (i != ast.getValues().size() - 1) {
                print(", ");
            }
        }
        // Print the closing brace
        print("}");

        return null;
    }

}
