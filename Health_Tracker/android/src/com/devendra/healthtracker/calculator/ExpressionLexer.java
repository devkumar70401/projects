package com.devendra.healthtracker.calculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic Lexical Scanner for Mathematical Expressions.
 * Tokenizes floating-point, scientific notation, operators, and handles implicit multiplication.
 */
public final class ExpressionLexer {
    private static final int MAX_TOKENS = 512;

    public static List<ExpressionToken> tokenize(String input) {
        if (input == null) throw new IllegalArgumentException("Expression input cannot be null");
        String trimmed = input.trim();
        List<ExpressionToken> rawTokens = new ArrayList<>();
        int len = trimmed.length();
        int i = 0;

        while (i < len) {
            char c = trimmed.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            int start = i;

            // Numbers (including scientific notation e.g. 1.23e-5)
            if (Character.isDigit(c) || (c == '.' && i + 1 < len && Character.isDigit(trimmed.charAt(i + 1)))) {
                boolean hasDot = (c == '.');
                i++;
                while (i < len && (Character.isDigit(trimmed.charAt(i)) || (!hasDot && trimmed.charAt(i) == '.'))) {
                    if (trimmed.charAt(i) == '.') hasDot = true;
                    i++;
                }

                // Check for scientific exponent 'e' or 'E'
                if (i < len && (trimmed.charAt(i) == 'e' || trimmed.charAt(i) == 'E')) {
                    int expStart = i;
                    i++;
                    if (i < len && (trimmed.charAt(i) == '+' || trimmed.charAt(i) == '-')) {
                        i++;
                    }
                    if (i < len && Character.isDigit(trimmed.charAt(i))) {
                        while (i < len && Character.isDigit(trimmed.charAt(i))) {
                            i++;
                        }
                    } else {
                        // Not a valid exponent, backtrack
                        i = expStart;
                    }
                }

                String numStr = trimmed.substring(start, i);
                double val = Double.parseDouble(numStr);
                rawTokens.add(new ExpressionToken(ExpressionToken.TokenType.NUMBER, numStr, val, start));
                checkLimit(rawTokens.size());
                continue;
            }

            // Identifiers / Function names / Variables (letters, underscores)
            if (Character.isLetter(c) || c == '_' || c == 'π' || c == 'τ' || c == 'φ' || c == 'γ') {
                i++;
                while (i < len) {
                    char next = trimmed.charAt(i);
                    if (Character.isLetterOrDigit(next) || next == '_') {
                        i++;
                    } else {
                        break;
                    }
                }
                String ident = trimmed.substring(start, i);
                rawTokens.add(new ExpressionToken(ExpressionToken.TokenType.IDENTIFIER, ident, 0.0, start));
                checkLimit(rawTokens.size());
                continue;
            }

            // Parentheses and delimiters
            if (c == '(') {
                rawTokens.add(new ExpressionToken(ExpressionToken.TokenType.LPAREN, "(", 0.0, start));
                i++;
                checkLimit(rawTokens.size());
                continue;
            }
            if (c == ')') {
                rawTokens.add(new ExpressionToken(ExpressionToken.TokenType.RPAREN, ")", 0.0, start));
                i++;
                checkLimit(rawTokens.size());
                continue;
            }
            if (c == ',' || c == ';') {
                rawTokens.add(new ExpressionToken(ExpressionToken.TokenType.COMMA, ",", 0.0, start));
                i++;
                checkLimit(rawTokens.size());
                continue;
            }

            // Double Factorial !!
            if (c == '!' && i + 1 < len && trimmed.charAt(i + 1) == '!') {
                rawTokens.add(new ExpressionToken(ExpressionToken.TokenType.OPERATOR, "!!", 0.0, start));
                i += 2;
                checkLimit(rawTokens.size());
                continue;
            }

            // Single Operators: +, -, *, /, %, ^, !
            if (c == '+' || c == '-' || c == '*' || c == '×' || c == '·' || c == '/' || c == '÷' || c == '%' || c == '^' || c == '!') {
                String opStr = String.valueOf(c);
                if (c == '×' || c == '·') opStr = "*";
                if (c == '÷') opStr = "/";
                rawTokens.add(new ExpressionToken(ExpressionToken.TokenType.OPERATOR, opStr, 0.0, start));
                i++;
                checkLimit(rawTokens.size());
                continue;
            }

            throw new IllegalArgumentException("Lexer error: Unexpected character '" + c + "' at index " + start);
        }

        // Second pass: Insert implicit multiplication where mathematically valid
        // E.g., [NUMBER, IDENT] -> [NUMBER, *, IDENT], [NUMBER, LPAREN] -> [NUMBER, *, LPAREN], [RPAREN, LPAREN] -> [RPAREN, *, LPAREN]
        List<ExpressionToken> finalTokens = new ArrayList<>();
        for (int k = 0; k < rawTokens.size(); k++) {
            ExpressionToken curr = rawTokens.get(k);
            finalTokens.add(curr);

            if (k + 1 < rawTokens.size()) {
                ExpressionToken next = rawTokens.get(k + 1);
                boolean needsMultiply = false;

                if (curr.type == ExpressionToken.TokenType.NUMBER &&
                        (next.type == ExpressionToken.TokenType.IDENTIFIER || next.type == ExpressionToken.TokenType.LPAREN)) {
                    needsMultiply = true;
                } else if (curr.type == ExpressionToken.TokenType.RPAREN &&
                        (next.type == ExpressionToken.TokenType.LPAREN || next.type == ExpressionToken.TokenType.NUMBER || next.type == ExpressionToken.TokenType.IDENTIFIER)) {
                    needsMultiply = true;
                } else if (curr.type == ExpressionToken.TokenType.IDENTIFIER && next.type == ExpressionToken.TokenType.NUMBER) {
                    // e.g. pi 2 or variable 5
                    needsMultiply = true;
                } else if (curr.type == ExpressionToken.TokenType.OPERATOR && (curr.text.equals("!") || curr.text.equals("!!")) &&
                        (next.type == ExpressionToken.TokenType.NUMBER || next.type == ExpressionToken.TokenType.IDENTIFIER || next.type == ExpressionToken.TokenType.LPAREN)) {
                    needsMultiply = true;
                }

                if (needsMultiply) {
                    finalTokens.add(new ExpressionToken(ExpressionToken.TokenType.OPERATOR, "*", 0.0, curr.position));
                }
            }
        }

        finalTokens.add(new ExpressionToken(ExpressionToken.TokenType.EOF, "", 0.0, len));
        return finalTokens;
    }

    private static void checkLimit(int tokenCount) {
        if (tokenCount > MAX_TOKENS) {
            throw new SecurityException("Security constraint exceeded: Expression exceeds maximum token limit (" + MAX_TOKENS + ")");
        }
    }
}