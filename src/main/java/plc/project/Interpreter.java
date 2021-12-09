package plc.project;

import com.sun.org.apache.xpath.internal.operations.Bool;

import javax.crypto.Cipher;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        ast.getGlobals().forEach(this::visit);
        ast.getFunctions().forEach(this::visit);
        Environment.Function mainFunc = scope.lookupFunction("main", 0);
        return mainFunc.invoke(Collections.emptyList());
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope decScope = scope;     // Capture the declaring scope
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope invScope = scope;     // Capture the invoking scope
            scope = new Scope(decScope);    // Set the function scope to be a new child of the declaring scope
            // Define all the arguments as variables:
            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
            }
            // Interpret the function statements until you run out or a return is thrown:
            try {
                ast.getStatements().forEach(this::visit);
            }
            // Return the return value (if found):
            catch (Return returnValue) {
                return returnValue.value;
            }
            // Restore the scope in all cases:
            finally {
                scope = invScope;
            }
            // Return NIL (if a return was not caught):
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()) {   // Variable is being initialized -> visit its value
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        }
        else {  // Variable is not being initialized -> set its value to be NIL
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        // Store values and check for appropriate types:
        Environment.PlcObject val = visit(ast.getValue());
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Expected an access expression at runtime.");
        }
        Ast.Expression.Access recAccess = (Ast.Expression.Access) ast.getReceiver();
        // Look up the variable and check its mutability:
        Environment.Variable var = scope.lookupVariable(recAccess.getName());
        if (!var.getMutable()) {
            throw new RuntimeException("Cannot assign to an immutable variable.");
        }
        // List value is being assigned:
        if (recAccess.getOffset().isPresent()) {
            requireType(List.class, var.getValue());
            int offset = ((BigInteger) visit(recAccess.getOffset().get()).getValue()).intValue();
            ((List<Object>) var.getValue().getValue()).set(offset, val.getValue());
        }
        // Regular variable is being assigned:
        else {
            var.setValue(val);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        // Evaluate the if statement's expression:
        Environment.PlcObject evalExpr = visit(ast.getCondition());
        requireType(Boolean.class, evalExpr);
        scope = new Scope(scope);
        if (evalExpr.getValue().equals(Boolean.TRUE)) { // The if statement's expression is true, execute the then statements
            try {
                ast.getThenStatements().forEach(this::visit);
            }
            finally {   // Restore scope regardless of exceptions
                scope = scope.getParent();
            }
        }
        else {  // The if statement's expression is false, execute the else statements
            try {
                ast.getElseStatements().forEach(this::visit);
            }
            finally {   // Restore scope regardless of exceptions
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        // Set a new scope for the switch block and get its components:
        scope = new Scope(scope);
        Environment.PlcObject evalExpr = visit(ast.getCondition());
        List<Ast.Statement.Case> cases = ast.getCases();
        // Evaluate whether the condition maps to any case:
        try {
            for (Ast.Statement.Case x : cases) {
                if (x.getValue().isPresent()) {
                    Environment.PlcObject evalCase = visit(x.getValue().get());
                    if (evalExpr.getValue().equals(evalCase.getValue())) {  // The case evaluates to true -> execute its block and break out of the loop
                        visit(x);
                        break;
                    }
                }
                // Evaluate the Default block:
                else {
                    visit(x);
                }
            }
        }
        finally {   // Restore scope regardless of exceptions thrown while evaluating cases
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        ast.getStatements().forEach(this::visit);   // Execute each statement in the case block
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) { // As long as the while condition holds true...
            try {   // Evaluate the statements in the while block
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            }
            finally {   // Restore scope regardless of exceptions
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) { // Special case for null literals
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        switch (operator) {
            case "&&":
            case "||": {
                Environment.PlcObject lhs = visit(ast.getLeft());
                requireType(Boolean.class, lhs);
                // Short-circuit cases:
                if (lhs.getValue().equals(Boolean.FALSE) && operator.equals("&&")) {
                    return lhs;
                }
                if (lhs.getValue().equals(Boolean.TRUE) && operator.equals("||")) {
                    return lhs;
                }
                Environment.PlcObject rhs = visit(ast.getRight());
                requireType(Boolean.class, rhs);
                // Find the result:
                if (operator.equals("&&")) {
                    return Environment.create(Boolean.logicalAnd((boolean) lhs.getValue(), (boolean) rhs.getValue()));
                }
                else {
                    return Environment.create(Boolean.logicalOr((boolean) lhs.getValue(), (boolean) rhs.getValue()));
                }
            }
            case "<":
            case ">": {
                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());
                // Check that classes match:
                requireType(lhs.getValue().getClass(), rhs);
                Comparable compLhs = requireType(Comparable.class, lhs);
                Comparable compRhs = requireType(Comparable.class, rhs);
                // Find the result
                int compVal = 0;
                compVal = compLhs.compareTo(compRhs);
                if ((compVal < 0 && operator.equals("<")) || (compVal > 0 && operator.equals(">"))) {
                    return Environment.create(Boolean.TRUE);
                }
                else {
                    return Environment.create(Boolean.FALSE);
                }
            }
            case "==": {
                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());
                return Environment.create(Objects.equals(lhs.getValue(), rhs.getValue()));   // Are their values equal to one another?
            }
            case "!=": {
                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());
                return Environment.create(!Objects.equals(lhs.getValue(), rhs.getValue()));   // Are their values not equal to one another?
            }
            case "+": {
                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof String || rhs.getValue() instanceof String) { // String concatenation
                    return Environment.create("" + lhs.getValue() + rhs.getValue());
                }
                else if (lhs.getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, rhs);
                    return Environment.create(((BigDecimal) lhs.getValue()).add((BigDecimal) rhs.getValue()));
                }
                else if (lhs.getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, rhs);
                    return Environment.create(((BigInteger) lhs.getValue()).add((BigInteger) rhs.getValue()));
                }
                else {
                    throw new RuntimeException("Invalid arithmetic operation detected at runtime.");
                }
            }
            case "*":
            case "-": {
                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, rhs);
                    if (operator.equals("*")) {
                        return Environment.create(((BigDecimal) lhs.getValue()).multiply((BigDecimal) rhs.getValue()));
                    }
                    else {
                        return Environment.create(((BigDecimal) lhs.getValue()).subtract((BigDecimal) rhs.getValue()));
                    }
                }
                else if (lhs.getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, rhs);
                    if (operator.equals("*")) {
                        return Environment.create(((BigInteger) lhs.getValue()).multiply((BigInteger) rhs.getValue()));
                    }
                    else {
                        return Environment.create(((BigInteger) lhs.getValue()).subtract((BigInteger) rhs.getValue()));
                    }
                }
                else {
                    throw new RuntimeException("Invalid arithmetic operation detected at runtime.");
                }
            }
            case "/": {
                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());
                if (rhs.getValue().equals(BigDecimal.valueOf(0)) || rhs.getValue().equals(BigInteger.valueOf(0))) {
                    throw new RuntimeException("Cannot divide by 0.");
                }
                if (lhs.getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, rhs);
                    return Environment.create(((BigDecimal) lhs.getValue()).divide((BigDecimal) rhs.getValue(), RoundingMode.HALF_EVEN));
                }
                else if (lhs.getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, rhs);
                    return Environment.create(((BigInteger) lhs.getValue()).divide((BigInteger) rhs.getValue()));
                }
                else {
                    throw new RuntimeException("Invalid arithmetic operation detected at runtime.");
                }
            }
            case "^": {
                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());
                requireType(BigInteger.class, rhs);
                if (lhs.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) lhs.getValue()).pow(((BigInteger) rhs.getValue()).intValue(), MathContext.DECIMAL64));
                }
                else if (lhs.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) lhs.getValue()).pow(((BigInteger) rhs.getValue()).intValue()));
                }
                else {
                    throw new RuntimeException("Invalid arithmetic operation detected at runtime.");
                }
            }
            default:
                throw new RuntimeException("Invalid binary expression detected at runtime.");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.PlcObject result = null;
        Environment.Variable var = scope.lookupVariable(ast.getName());
        if (ast.getOffset().isPresent()) {  // A list value is being accessed
            Environment.PlcObject offset = visit(ast.getOffset().get());
            requireType(BigInteger.class, offset);
            requireType(List.class, var.getValue());
            // Get the list of values, go to the desired offset, and return its appropriate value (wrapping it as a PlcObject):
            result = Environment.create(((List<Environment.PlcObject>) var.getValue().getValue()).get(((BigInteger) offset.getValue()).intValue()));
        }
        else {  // A normal variable is being accessed
            result = Environment.create(var.getValue().getValue());
        }
        return result;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        Environment.Function func = scope.lookupFunction(ast.getName(), ast.getArguments().size());    // Does the function exist in this scope?
        List<Environment.PlcObject> evalExpr = new ArrayList<>();   // Stores the evaluated argument expressions for passing to the function
        for (Ast.Expression expr : ast.getArguments()) {
            evalExpr.add(visit(expr));
        }
        return func.invoke(evalExpr);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> evalList = new ArrayList<>();
        for (Ast.Expression x : ast.getValues()) {
            evalList.add(visit(x).getValue());
        }
        return Environment.create(evalList);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
