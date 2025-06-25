package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.board.Move;

/**
 * A Transposition Table (TT) used in game tree search algorithms like Alpha-Beta pruning.
 * <p>
 * It stores information about previously evaluated board positions, specifically their
 * Zobrist hash key, evaluation score, search depth at which they were evaluated,
 * the type of score (exact, lower bound, or upper bound), and optionally the best move found.
 * This helps to avoid re-computing evaluations for positions that can be reached
 * through different sequences of moves (transpositions).
 * <p>
 * The table is implemented as a simple array, and entries are mapped to indices using
 * modulo arithmetic on their Zobrist keys. Collisions (where different Zobrist keys
 * map to the same index) are handled by storing the full Zobrist key in the entry
 * and verifying it upon lookup. A basic "always replace" strategy is used when a new entry
 * needs to be stored at an occupied index.
 *
 * @see TranspositionTableEntry
 * @see ZobristHashing
 * @see StockAlphaBeta
 */
public class TranspositionTable {

    /**
     * Default desired table size in Megabytes. The actual number of entries will be derived from this.
     * This value can be overridden by providing a specific number of entries to the constructor.
     */
    private static final int DEFAULT_TABLE_SIZE_MB = 64;

    /**
     * Rough estimate of the memory footprint of a single {@link TranspositionTableEntry} in bytes.
     * Used to calculate the number of entries from the desired table size in MB.
     * (long key=8, int score=4, int depth=4, enum flag=~4, Move ref=~4-8) -> ~24-28 + object overhead.
     */
    private static final int BYTES_PER_ENTRY_ESTIMATE = 32;

    /**
     * Default number of entries, calculated from {@link #DEFAULT_TABLE_SIZE_MB} and {@link #BYTES_PER_ENTRY_ESTIMATE}.
     * This serves as the default capacity if no specific size is requested.
     */
    private static final int DEFAULT_NUM_ENTRIES = (DEFAULT_TABLE_SIZE_MB * 1024 * 1024) / BYTES_PER_ENTRY_ESTIMATE;

    private final TranspositionTableEntry[] table;
    private final int tableCapacity; // Actual number of entries the table can hold.

    /**
     * Constructs a TranspositionTable with a default capacity derived from {@link #DEFAULT_TABLE_SIZE_MB}.
     */
    public TranspositionTable() {
        this(DEFAULT_NUM_ENTRIES);
    }

    /**
     * Constructs a TranspositionTable with a specified capacity (number of entries).
     *
     * @param numEntries The desired number of entries in the table. Must be positive.
     * @throws IllegalArgumentException if numEntries is not positive.
     */
    public TranspositionTable(int numEntries) {
        if (numEntries <= 0) {
            throw new IllegalArgumentException("Number of entries must be positive.");
        }
        // For simplicity with modulo arithmetic for indexing, any positive size is fine.
        // If bitmask indexing (hash & (table.length - 1)) were used, tableCapacity would need to be a power of two.
        this.tableCapacity = numEntries;
        this.table = new TranspositionTableEntry[this.tableCapacity];
        System.out.println("Transposition Table initialized with capacity for " + this.tableCapacity + " entries.");
    }

    /**
     * Looks up an entry in the transposition table using the Zobrist key of a board position.
     *
     * @param zobristKey The 64-bit Zobrist key of the board position to look up.
     * @return The {@link TranspositionTableEntry} if an entry exists for this Zobrist key,
     *         and the stored key matches (to confirm it's not a collision for a different position).
     *         Returns null otherwise.
     */
    public TranspositionTableEntry lookup(final long zobristKey) {
        final int index = getTableIndex(zobristKey);
        final TranspositionTableEntry entry = this.table[index];

        // Validate entry: must exist and Zobrist keys must match to avoid collisions.
        if (entry != null && entry.getZobristKey() == zobristKey) {
            return entry;
        }
        return null;
    }

    /**
     * Stores an entry into the transposition table.
     * <p>
     * The current implementation uses an "always replace" strategy: if the calculated
     * index is already occupied, the old entry is overwritten. More sophisticated
     * replacement strategies (e.g., depth-preferred replacement, or replacing entries
     * from older search iterations) could be implemented for potentially better performance.
     *
     * @param zobristKey The 64-bit Zobrist key of the board position.
     * @param depth The remaining search depth for which this entry is valid.
     * @param score The evaluation score associated with the position.
     * @param flag The {@link TranspositionTableEntry.EntryType} indicating whether the score is
     *             an exact value, a lower bound (alpha), or an upper bound (beta).
     * @param bestMove The best {@link Move} found from this position. Can be null if no specific
     *                 move is strongly indicated (e.g., for shallow depths or cutoff nodes).
     */
    public void store(final long zobristKey,
                      final int depth,
                      final int score,
                      final TranspositionTableEntry.EntryType flag,
                      final Move bestMove) {
        final int index = getTableIndex(zobristKey);

        // Current strategy: Always replace.
        // Future enhancements could include:
        // 1. Depth-preferred replacement: Only replace if the new entry's depth is greater or equal.
        //    TranspositionTableEntry oldEntry = this.table[index];
        //    if (oldEntry == null || depth >= oldEntry.getDepth()) {
        //        this.table[index] = new TranspositionTableEntry(zobristKey, score, depth, flag, bestMove);
        //    }
        // 2. More complex schemes considering search ID / age of entry for iterative deepening.
        this.table[index] = new TranspositionTableEntry(zobristKey, score, depth, flag, bestMove);
    }

    /**
     * Clears all entries from the transposition table by setting them to null.
     * This is typically invoked before starting a new top-level search (e.g., for a new move
     * from the root of the game tree), unless the table entries are managed with search IDs
     * to persist useful entries across searches in iterative deepening frameworks.
     */
    public void clear() {
        for (int i = 0; i < this.table.length; i++) {
            this.table[i] = null;
        }
        System.out.println("Transposition Table cleared.");
    }

    /**
     * Calculates the table index for a given Zobrist key.
     * <p>
     * This implementation uses modulo arithmetic with {@link Math#abs(long)} to ensure
     * the resulting index is non-negative, as the Zobrist key can be negative and the
     * Java `%` operator can produce negative results if the dividend is negative.
     * <p>
     * If {@code tableCapacity} were guaranteed to be a power of two, a bitwise AND
     * operation (e.g., {@code zobristKey & (tableCapacity - 1)}) could be used for
     * potentially faster index calculation.
     *
     * @param zobristKey The 64-bit Zobrist key.
     * @return The calculated non-negative index into the table array.
     */
    private int getTableIndex(final long zobristKey) {
        return (int) (Math.abs(zobristKey) % this.tableCapacity);
    }

    /**
     * Gets the current capacity (maximum number of entries) of the transposition table.
     * @return The capacity of the transposition table.
     */
    public int getTableCapacity() {
        return tableCapacity;
    }

    // Future considerations:
    // - Statistics tracking (hit rate, number of used entries, collision rate).
    // - More sophisticated replacement strategies.
    // - Support for table resizing or clearing based on search IDs (for iterative deepening).
}
