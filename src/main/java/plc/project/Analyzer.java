package plc.project;

import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        // Visit globals, followed by functions:
        ast.getGlobals().forEach(this::visit);
        ast.getFunctions().forEach(this::visit);
        // Check for exception cases:
        Environment.Function mainFunc = scope.lookupFunction("main", 0);
        if (!mainFunc.getReturnType().getName().equals("Integer")) {
            throw new RuntimeException("Main method must have an integer return type.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        // If the value is present, visit it:
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
        }
        // Create the variable object and check that it meets assignable conditions:
        Environment.Variable var = new Environment.Variable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()),
                ast.getMutable(), Environment.NIL);
        requireAssignable(var.getType(), var.getValue().getType());
        // Set the variable in the AST and define it in the current scope:
        ast.setVariable(var);
        scope.defineVariable(var.getName(), var.getJvmName(), var.getType(), var.getMutable(), var.getValue());

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        // Collect the parameter types in a list:
        List<Environment.Type> paramTypes = new ArrayList<>();
        for (String i : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.getType(i));
        }
        // Construct the function object
        Environment.Function func = new Environment.Function(ast.getName(), ast.getName(), paramTypes,
                Environment.getType(ast.getReturnTypeName().orElse("Nil")), args -> Environment.NIL);
        // Set the function in the AST and define it in the current scope:
        ast.setFunction(func);
        scope.defineFunction(func.getName(), func.getJvmName(), paramTypes, func.getReturnType(), args -> Environment.NIL);
        // Define a new scope, store the function node in a variable (to access return type in return visit method), then visit each statement in the function:
        scope = new Scope(scope);
        function = ast;
        ast.getStatements().forEach(this::visit);
        // Restore function to null and scope to parent:
        function = null;
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expected a function expression.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        // Determine the type of the variable being declared:
        Environment.Type type;
        if (!(ast.getTypeName().isPresent())) {
            if (!(ast.getValue().isPresent())) {
                throw new RuntimeException("Type of declared variable could not be discerned.");
            }
            type = ast.getValue().get().getType();
        }
        else {
            type = Environment.getType(ast.getTypeName().get());
        }
        // Visit the value (if present) and check the exception condition that the variable type and value type are not compatible:
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(type, ast.getValue().get().getType());
        }
        // Declare the variable in the current scope:
        scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL);

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        requireAssignable(Environment.getType(function.getReturnTypeName().orElse("Nil")), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.getName().equals(type.getName())) {
            return;
        }
        if (target.getName().equals("Any")) {
            return;
        }
        if (target.getName().equals("Comparable")) {
            if (type.getName().equals("Integer") || type.getName().equals("Decimal") || type.getName().equals("Character")
                    || type.getName().equals("String")) {
                return;
            }
        }
        throw new RuntimeException("Invalid assignment! Attempting to assign " + type.getName() + " to a " + type.getName() + " variable.");
    }
}
