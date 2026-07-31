package com.zrlog.plugin.backup.service;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

final class MysqlDumpStatements {

    private MysqlDumpStatements() {
    }

    static List<String> read(Reader reader) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder statement = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean backtickQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;
        boolean escaped = false;
        int previous = -1;
        int value;
        while ((value = reader.read()) >= 0) {
            char current = (char) value;
            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    statement.append(' ');
                }
                previous = value;
                continue;
            }
            if (blockComment) {
                if (previous == '*' && current == '/') {
                    blockComment = false;
                    previous = -1;
                    statement.append(' ');
                } else {
                    previous = value;
                }
                continue;
            }
            if (!singleQuoted && !doubleQuoted && !backtickQuoted) {
                if (previous == '/' && current == '*') {
                    if (statement.length() > 0) {
                        statement.setLength(statement.length() - 1);
                    }
                    blockComment = true;
                    previous = value;
                    continue;
                }
                if (current == '#' && atLineStart(statement)) {
                    lineComment = true;
                    previous = value;
                    continue;
                }
                if (previous == '-' && current == '-' && atSqlCommentStart(statement)) {
                    if (statement.length() > 0) {
                        statement.setLength(statement.length() - 1);
                    }
                    lineComment = true;
                    previous = value;
                    continue;
                }
            }
            if (escaped) {
                statement.append(current);
                escaped = false;
                previous = value;
                continue;
            }
            if ((singleQuoted || doubleQuoted) && current == '\\') {
                statement.append(current);
                escaped = true;
                previous = value;
                continue;
            }
            if (!doubleQuoted && !backtickQuoted && current == '\'') {
                singleQuoted = !singleQuoted;
            } else if (!singleQuoted && !backtickQuoted && current == '"') {
                doubleQuoted = !doubleQuoted;
            } else if (!singleQuoted && !doubleQuoted && current == '`') {
                backtickQuoted = !backtickQuoted;
            }
            if (!singleQuoted && !doubleQuoted && !backtickQuoted && current == ';') {
                addStatement(statements, statement);
                previous = value;
                continue;
            }
            statement.append(current);
            previous = value;
        }
        if (singleQuoted || doubleQuoted || backtickQuoted || blockComment) {
            throw new IOException("MySQL dump contains an unterminated quote or comment");
        }
        addStatement(statements, statement);
        return statements;
    }

    private static boolean atLineStart(StringBuilder statement) {
        for (int i = statement.length() - 1; i >= 0; i--) {
            char value = statement.charAt(i);
            if (value == '\n' || value == '\r') {
                return true;
            }
            if (!Character.isWhitespace(value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean atSqlCommentStart(StringBuilder statement) {
        int length = statement.length();
        return length <= 1 || Character.isWhitespace(statement.charAt(length - 2));
    }

    private static void addStatement(List<String> statements, StringBuilder statement) {
        String value = statement.toString().trim();
        statement.setLength(0);
        if (!value.isEmpty()) {
            statements.add(value);
        }
    }
}
