package ru.test.domain.exception;

public class CsvParserException extends Exception {
    private final ErrorCode errorCode;
    private final int lineNumber;

    public CsvParserException(ErrorCode errorCode, int lineNumber, String details) {
        super(details);
        this.errorCode = errorCode;
        this.lineNumber = lineNumber;
    }

    public CsvParserException(ErrorCode errorCode, String details) {
        this(errorCode, 0, details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}