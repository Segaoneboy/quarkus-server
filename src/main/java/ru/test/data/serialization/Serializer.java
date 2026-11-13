package ru.test.data.serialization;

import java.io.File;
import java.io.IOException;



public interface Serializer<T> {
    String getFormatName();
    String getExtension();

    void save(T data, File file) throws IOException;

    T load(File file) throws IOException;
}
