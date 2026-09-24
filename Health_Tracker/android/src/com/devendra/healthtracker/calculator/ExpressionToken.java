package com.devendra.healthtracker.calculator;

public final class ExpressionToken {
    public enum TokenType {
        NUMBER,
        IDENTIFIER,
        OPERATOR,
        LPAREN,
        RPAREN,
        COMMA,
        EOF
    }

    public final TokenType type;
    public final String text;
    public final double numValue;
    public final int position;

    public ExpressionToken(TokenType type, String text, double numValue, int position) {
        this.type = type;
        this.text = text;
        this.numValue = numValue;
        this.position = position;
    }

    @Override
    public String toString() {
        return type + "('" + text + "')@" + position;
    }
}