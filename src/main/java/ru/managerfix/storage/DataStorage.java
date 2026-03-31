package ru.managerfix.storage;

/**
 * Base interface for data storage backends.
 */
public interface DataStorage {

    /**
     * Initializes the storage (e.g. create tables, ensure folders exist).
     */
    void init();

    /**
     * Shuts down the storage (e.g. close connections).
     */
    void shutdown();
}
