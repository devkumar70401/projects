package com.devendra.healthtracker.calculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic Recursive Descent Expression Parser.
 * Enforces exact mathematical operator precedence, right-associative exponentiation,
 * postfix factorials, function argument parsing, and maximum recursion depth limits.
 */
public final class ExpressionParser {
    private static final int MAX_PARSE_DEPTH = 64;

    private final List<ExpressionToken> tokens;
    private int current;
    private int depth;

    public ExpressionParser(List<ExpressionToken> tokens) {
        this.tokens = tokens;
        this.current = 0;
        this.depth = 0;
    }

    public static ASTNodes.ExpressionNode parse(String expression) {
        List<ExpressionToken> tokens = ExpressionLexer.tokenize(expression);
        ExpressionParser parser = new ExpressionParser(tokens);
        ASTNodes.ExpressionNode node = parser.parseExpression();
        if (!parser.isAtEnd()) {
            ExpressionToken extra = parser.peek();
            throw new IllegalArgumentException("Syntax error: Unexpected token '" + extra.text + "' at index " + extra.position);
        }
        return node;
    }

    private ASTNodes.ExpressionNode parseExpression() {
        return parseAddition();
    }

    // 1. Addition and Subtraction (Lowest binary precedence)
    private ASTNodes.ExpressionNode parseAddition() {
        enter();
        try {
            ASTNodes.ExpressionNode expr = parseMultiplication();

            while (matchOperator("+", "-")) {
                ExpressionToken op = previous();
                ASTNodes.ExpressionNode right = parseMultiplication();
                expr = new ASTNodes.BinaryOpNode(op.text, expr, right);
            }
            return expr;
        } finally {
            exit();
        }
    }

    // 2. Multiplication, Division, Modulo
    private ASTNodes.ExpressionNode parseMultiplication() {
        enter();
        try {
            ASTNodes.ExpressionNode expr = parseUnary();

            while (matchOperator("*", "/", "%")) {
                ExpressionToken op = previous();
                ASTNodes.ExpressionNode right = parseUnary();
                expr = new ASTNodes.BinaryOpNode(op.text, expr, right);
            }
            return expr;
        } finally {
            exit();
        }
    }

    // 3. Unary Prefix (+, -)
    private ASTNodes.ExpressionNode parseUnary() {
        enter();
        try {
            if (matchOperator("+", "-")) {
                ExpressionToken op = previous();
                ASTNodes.ExpressionNode operand = parseUnary();
                return new ASTNodes.UnaryOpNode(op.text, operand);
            }
            return parseExponentiation();
        } finally {
            exit();
        }
    }

    // 4. Exponentiation ^ (Right-associative: 2 ^ 3 ^ 2 = 2 ^ (3 ^ 2))
    private ASTNodes.ExpressionNode parseExponentiation() {
        enter();
        try {
            ASTNodes.ExpressionNode expr = parsePostfix();

            if (matchOperator("^")) {
                ExpressionToken op = previous();
                ASTNodes.ExpressionNode right = parseUnary(); // Right-recursive, allows 2 ^ -3
                expr = new ASTNodes.BinaryOpNode(op.text, expr, right);
            }
            return expr;
        } finally {
            exit();
        }
    }

    // 5. Postfix (!, !!)
    private ASTNodes.ExpressionNode parsePostfix() {
        enter();
        try {
            ASTNodes.ExpressionNode expr = parsePrimary();

            while (matchOperator("!", "!!")) {
                ExpressionToken op = previous();
                expr = new ASTNodes.PostfixOpNode(op.text, expr);
            }
            return expr;
        } finally {
            exit();
        }
    }

    // 6. Primary: Number, Parenthesized expression, Function call, Variable
    private ASTNodes.ExpressionNode parsePrimary() {
        enter();
        try {
            // Numbers
            if (match(ExpressionToken.TokenType.NUMBER)) {
                return new ASTNodes.NumberNode(previous().numValue);
            }

            // Identifiers: Functions or Variables
            if (match(ExpressionToken.TokenType.IDENTIFIER)) {
                ExpressionToken ident = previous();

                // Check if followed by '(' -> function call
                if (match(ExpressionToken.TokenType.LPAREN)) {
                    List<ASTNodes.ExpressionNode> args = new ArrayList<>();
                    if (!check(ExpressionToken.TokenType.RPAREN)) {
                        do {
                            args.add(parseExpression());
                        } while (match(ExpressionToken.TokenType.COMMA));
                    }
                    consume(ExpressionToken.TokenType.RPAREN, "Expected ')' after function arguments");
                    return new ASTNodes.FunctionCallNode(ident.text, args);
                }

                return new ASTNodes.VariableNode(ident.text);
            }

            // Parentheses '(' expr ')'
            if (match(ExpressionToken.TokenType.LPAREN)) {
                ASTNodes.ExpressionNode expr = parseExpression();
                consume(ExpressionToken.TokenType.RPAREN, "Mismatched parentheses: Expected closing ')'");
                return expr;
            }

            ExpressionToken t = peek();
            throw new IllegalArgumentException("Syntax error: Unexpected token '" + t.text + "' at index " + t.position);
        } finally {
            exit();
        }
    }

    private boolean match(ExpressionToken.TokenType... types) {
        for (ExpressionToken.TokenType t : types) {
            if (check(t)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean matchOperator(String... ops) {
        if (!check(ExpressionToken.TokenType.OPERATOR)) return false;
        String opText = peek().text;
        for (String op : ops) {
            if (op.equals(opText)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(ExpressionToken.TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private ExpressionToken advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == ExpressionToken.TokenType.EOF;
    }

    private ExpressionToken peek() {
        return tokens.get(current);
    }

    private ExpressionToken previous() {
        return tokens.get(current - 1);
    }

    private ExpressionToken consume(ExpressionToken.TokenType type, String message) {
        if (check(type)) return advance();
        throw new IllegalArgumentException(message + " at index " + peek().position);
    }

    private void enter() {
        depth++;
        if (depth > MAX_PARSE_DEPTH) {
            throw new SecurityException("Security constraint exceeded: Maximum expression parse depth of " + MAX_PARSE_DEPTH + " exceeded");
        }
    }

    private void exit() {
        depth--;
    }
}