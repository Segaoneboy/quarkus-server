package ru.test.domain.exception;

public enum ErrorCode {
    FILE_NOT_FOUND("Файл не найден"),
    FILE_READ_ERROR("Ошибка чтения файла"),
    FILE_WRITE_ERROR("Ошибка записи файла"),
    WRONG_FIELD_COUNT("Неверное количество колонок в строке"),
    BAD_NUMBER("Ошибка формата числа (возраст, ID, рейтинг и т.д.)"),
    INVALID_HEADER("Неверный формат заголовка CSV файла"),
    UNKNOWN_ERROR("Неизвестная ошибка"),
    FUTURE_SCHEMA_VERSION("Версия файла новее поддерживаемой сервером (файл из будущего)"),
    UNSUPPORTED_SCHEMA_VERSION("Неподдерживаемая версия схемы"),
    CORRUPTED_FILE("Файл повреждён или имеет неверную сигнатуру"),
    INVALID_FIELD_VALUE("Некорректное значение поля, обнаруженное при миграции");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}