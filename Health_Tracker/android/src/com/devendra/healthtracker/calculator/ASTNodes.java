package com.devendra.healthtracker.calculator;

import java.util.List;

public final class ASTNodes {

    public interface ExpressionNode {
        ComplexNumber evaluate(EvaluationContext context);
    }

    public static final class NumberNode implements ExpressionNode {
        public final double value;

        public NumberNode(double value) {
            this.value = value;
        }

        @Override
        public ComplexNumber evaluate(EvaluationContext context) {
            return ComplexNumber.ofReal(value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class VariableNode implements ExpressionNode {
        public final String name;

        public VariableNode(String name) {
            this.name = name;
        }

        @Override
        public ComplexNumber evaluate(EvaluationContext context) {
            return context.resolveVariable(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class UnaryOpNode implements ExpressionNode {
        public final String operator;
        public final ExpressionNode operand;

        public UnaryOpNode(String operator, ExpressionNode operand) {
            this.operator = operator;
            this.operand = operand;
        }

        @Override
        public ComplexNumber evaluate(EvaluationContext context) {
            ComplexNumber val = operand.evaluate(context);
            if ("-".equals(operator)) {
                return val.negate();
            } else if ("+".equals(operator)) {
                return val;
            }
            throw new UnsupportedOperationException("Unknown unary operator: " + operator);
        }
    }

    public static final class BinaryOpNode implements ExpressionNode {
        public final String operator;
        public final ExpressionNode left;
        public final ExpressionNode right;

        public BinaryOpNode(String operator, ExpressionNode left, ExpressionNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public ComplexNumber evaluate(EvaluationContext context) {
            ComplexNumber lVal = left.evaluate(context);
            ComplexNumber rVal = right.evaluate(context);

            switch (operator) {
                case "+":
                    return lVal.add(rVal);
                case "-":
                    return lVal.subtract(rVal);
                case "*":
                    return lVal.multiply(rVal);
                case "/":
                    if (rVal.abs() < 1e-15) {
                        throw new ArithmeticException("Division by zero");
                    }
                    return lVal.divide(rVal);
                case "%":
                    if (!lVal.isReal() || !rVal.isReal()) {
                        throw new ArithmeticException("Modulo operator % is only supported on real numbers");
                    }
                    if (Math.abs(rVal.real) < 1e-15) {
                        throw new ArithmeticException("Division by zero in modulo operation");
                    }
                    return ComplexNumber.ofReal(lVal.real % rVal.real);
                case "^":
                    return lVal.pow(rVal);
                default:
                    throw new UnsupportedOperationException("Unknown binary operator: " + operator);
            }
        }
    }

    public static final class PostfixOpNode implements ExpressionNode {
        public final String operator;
        public final ExpressionNode operand;

        public PostfixOpNode(String operator, ExpressionNode operand) {
            this.operator = operator;
            this.operand = operand;
        }

        @Override
        public ComplexNumber evaluate(EvaluationContext context) {
            ComplexNumber val = operand.evaluate(context);
            if (!val.isReal()) {
                throw new ArithmeticException("Factorial operator is only supported on real numbers");
            }
            double x = val.real;
            if ("!".equals(operator)) {
                if (x < 0) throw new ArithmeticException("Factorial is not defined for negative numbers");
                if (Math.floor(x) == x) {
                    return ComplexNumber.ofReal(NumberTheory.factorial((int) x).doubleValue());
                } else {
                    return ComplexNumber.ofReal(NumberTheory.gamma(x + 1.0));
                }
            } else if ("!!".equals(operator)) {
                if (x < 0 || Math.floor(x) != x) {
                    throw new ArithmeticException("Double factorial requires non-negative integer");
                }
                return ComplexNumber.ofReal(NumberTheory.doubleFactorial((int) x).doubleValue());
            }
            throw new UnsupportedOperationException("Unknown postfix operator: " + operator);
        }
    }

    public static final class FunctionCallNode implements ExpressionNode {
        public final String name;
        public final List<ExpressionNode> arguments;

        public FunctionCallNode(String name, List<ExpressionNode> arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public ComplexNumber evaluate(EvaluationContext context) {
            return context.callFunction(name, arguments);
        }
    }
}