package com.automattic.android.tracks.networklogger

import com.automattic.android.tracks.networklogger.model.NetworkLogEntry
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe circular buffer for storing network log entries in memory.
 * When the buffer reaches max capacity, oldest entries are evicted (FIFO).
 */
internal class LogBuffer(private val maxSize: Int) {

    private val entries = ConcurrentLinkedQueue<NetworkLogEntry>()

    /**
     * Adds a new entry to the buffer.
     * If buffer is full, oldest entry is removed first.
     */
    @Synchronized
    fun add(entry: NetworkLogEntry) {
        // Add new entry
        entries.add(entry)

        // Evict oldest if over capacity
        while (entries.size > maxSize) {
            entries.poll()
        }
    }

    /**
     * Returns all entries in the buffer and clears it.
     * @return List of all entries, ordered from oldest to newest
     */
    @Synchronized
    fun drainAll(): List<NetworkLogEntry> {
        val list = entries.toList()
        entries.clear()
        return list
    }

    /**
     * Returns a copy of all entries without clearing the buffer.
     * @return List of all entries, ordered from oldest to newest
     */
    @Synchronized
    fun peekAll(): List<NetworkLogEntry> {
        return entries.toList()
    }

    /**
     * Returns the current number of entries in the buffer.
     */
    fun size(): Int = entries.size

    /**
     * Clears all entries from the buffer.
     */
    @Synchronized
    fun clear() {
        entries.clear()
    }
}
