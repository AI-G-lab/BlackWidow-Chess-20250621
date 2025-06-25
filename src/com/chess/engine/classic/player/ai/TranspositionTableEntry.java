package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.board.Move;

/**
 * Represents an entry in the transposition table.
 * It stores the evaluation score, search depth, type of score (exact, lower bound, upper bound),
 * and optionally the best move found for a given board position (identified by its Zobrist key).
 */
public class TranspositionTableEntry {

    /**
     * Defines the type of score stored in the transposition table entry.
     * EXACT: The score is the true evaluation of the node.
     * LOWER_BOUND: The score is a lower bound (alpha) on the true evaluation.
     *              This occurs when the search failed high (produced a value >= beta).
     * UPPER_BOUND: The score is an upper bound (beta) on the true evaluation.
     *              This occurs when the search failed low (produced a value <= alpha).
     */
    public enum EntryType {
        EXACT,
        LOWER_BOUND,
        UPPER_BOUND
    }

    private final long zobristKey; // Full Zobrist key for validation if using a simple array index.
    private final int score;
    private final int depth; // Remaining depth of the search that stored this entry.
    private final EntryType flag;
    private final Move bestMove; // Best move found from this position. Can be null.

    /**
     * Constructs a new TranspositionTableEntry.
     *
     * @param zobristKey The Zobrist hash key of the board position.
     * @param score The evaluation score.
     * @param depth The depth of the search that generated this score.
     * @param flag The type of score (EXACT, LOWER_BOUND, or UPPER_BOUND).
     * @param bestMove The best move found from this position (can be null).
     */
    public TranspositionTableEntry(long zobristKey, int score, int depth, EntryType flag, Move bestMove) {
        this.zobristKey = zobristKey;
        this.score = score;
        this.depth = depth;
        this.flag = flag;
        this.bestMove = bestMove;
    }

    public long getZobristKey() {
        return zobristKey;
    }

    public int getScore() {
        return score;
    }

    public int getDepth() {
        return depth;
    }

    public EntryType getFlag() {
        return flag;
    }

    public Move getBestMove() {
        return bestMove;
    }

    @Override
    public String toString() {
        return "TTEntry[Key: " + zobristKey + ", Score: " + score + ", Depth: " + depth +
               ", Flag: " + flag + ", BestMove: " + (bestMove != null ? bestMove.toString() : "None") + "]";
    }
}
