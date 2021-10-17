package plc.project;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            }
            finally {
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
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        if (operator.equals("&&") || operator.equals("||")) {
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
        else if (operator.equals("<") || operator.equals(">")) {
            Environment.PlcObject lhs = visit(ast.getLeft());
            Environment.PlcObject rhs = visit(ast.getRight());
            // Check that classes match:
            requireType(Comparable.class, lhs);
            requireType(Comparable.class, rhs);
            requireType(lhs.getValue().getClass(), rhs);
            // Find the result:
            return Environment.create(Boolean.valueOf(lhs.getValue() + operator + rhs.getValue()));
        }
        else if (operator.equals("==") || operator.equals("!=")) {
            Environment.PlcObject lhs = visit(ast.getLeft());
            Environment.PlcObject rhs = visit(ast.getRight());
            return Environment.create(Objects.equals(lhs.getValue(), rhs.getValue()));   // Are their values equal to one another?
        }
        else if (operator.equals("+")) {
            Environment.PlcObject lhs = visit(ast.getLeft());
            Environment.PlcObject rhs = visit(ast.getRight());
            if (lhs.getValue() instanceof String || rhs.getValue() instanceof String) {
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
        else if (operator.equals("*") || operator.equals("-")) {
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
        else if (operator.equals("/")) {
            Environment.PlcObject lhs = visit(ast.getLeft());
            Environment.PlcObject rhs = visit(ast.getRight());
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
        else if (operator.equals("^")) {
            Environment.PlcObject lhs = visit(ast.getLeft());
            Environment.PlcObject rhs = visit(ast.getRight());
            if (lhs.getValue() instanceof BigDecimal) {
                requireType(BigDecimal.class, rhs);
                return Environment.create(Math.pow((double) lhs.getValue(), (double) rhs.getValue()));
            }
            else if (lhs.getValue() instanceof BigInteger) {
                requireType(BigInteger.class, rhs);
                return Environment.create(((BigInteger) lhs.getValue()).pow((int) rhs.getValue()));
            }
            else {
                throw new RuntimeException("Invalid arithmetic operation detected at runtime.");
            }
        }
        throw new RuntimeException("Invalid binary expression detected at runtime.");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.PlcObject result = null;
        Environment.Variable var = scope.lookupVariable(ast.getName());
        if (ast.getOffset().isPresent()) {
            Environment.PlcObject offset = visit(ast.getOffset().get());
            requireType(BigInteger.class, offset);
            // Get the list of values, go to the desired offset, visit the residing expression, and return its appropriate PlcObject:
            result = visit(((Ast.Expression.PlcList) var.getValue().getValue()).getValues().get((int) offset.getValue()));
        }
        else {
            result = Environment.create(var.getValue());
        }
        return result;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        Environment.Function func = scope.lookupFunction(ast.getName(), ast.getArguments().size());    // Does the function exist in this scope?
        List<Environment.PlcObject> evalExpr = new ArrayList<>();
        for (Ast.Expression expr : ast.getArguments()) {
            evalExpr.add(visit(expr));
        }
        return func.invoke(evalExpr);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        return Environment.create(ast.getValues());
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
