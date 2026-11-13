package ru.test.domain.exception;

public class MigrationException extends Exception {
    private final ErrorCode errorCode;
    private final int detectedVersion;

    public MigrationException(ErrorCode errorCode, int detectedVersion, String details) {
        super(details);
        this.errorCode = errorCode;
        this.detectedVersion = detectedVersion;
    }

    public MigrationException(ErrorCode errorCode, String details) {
        this(errorCode, -1, details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getDetectedVersion() {
        return detectedVersion;
    }
}