package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.BoardUtils;
import com.chess.engine.classic.board.Move;
import com.chess.engine.classic.board.MoveTransition;
import com.chess.engine.classic.player.Player;

import java.util.*;

import static com.chess.engine.classic.board.BoardUtils.mvvlva;
import static com.chess.engine.classic.board.BoardUtils.scoreMove;
import static com.chess.engine.classic.board.Move.MoveFactory;

/**
 * An AI {@link MoveStrategy} that implements the Alpha-Beta pruning search algorithm.
 * This version incorporates a {@link TranspositionTable} to cache previously evaluated
 * positions, potentially significantly speeding up the search by avoiding redundant computations.
 * It also uses a {@link BoardEvaluator} to score terminal or quiescent board states.
 *
 * <p>The search depth is configurable. The AI attempts to find the best move for the
 * current player on a given {@link Board}.
 *
 * <p>Note: Zobrist hashing for transposition table keys is currently computed from scratch
 * at each node. Incremental Zobrist key updates would be a key performance optimization.
 * The quiescence search implementation is also currently minimal.
 */
public class StockAlphaBeta extends Observable implements MoveStrategy {

    private final BoardEvaluator evaluator;
    private final int searchDepth;
    private final boolean trackBoardEvaluations; // To avoid double counting if evaluator is shared.
    private long boardsEvaluated; // Number of board states evaluated in the last top-level search.
    private int quiescenceCount; // Count of quiescence extensions in the last search.
    private final TranspositionTable transpositionTable; // Transposition table for caching search results.

    private static final int MAX_QUIESCENCE = 0; // Effectively disables deep quiescence search (5000 * 0 = 0).

    private static final Comparator<Move> SIMPLE_MOVE_COMPARATOR = (m1, m2) -> {
        if (m1.isCastlingMove() != m2.isCastlingMove()) {
            return m1.isCastlingMove() ? -1 : 1;
        }
        return Integer.compare(mvvlva(m2), mvvlva(m1));
    };

    private static final Comparator<Move> EXPENSIVE_MOVE_COMPARATOR = (m1, m2) -> {
        final int score1 = scoreMove(m1);
        final int score2 = scoreMove(m2);
        return Integer.compare(score2, score1);
    };

    private enum MoveSorter {
        STANDARD {
            @Override
            Collection<Move> sort(Collection<Move> moves) {
                final List<Move> sorted = new ArrayList<>(moves);
                sorted.sort(SIMPLE_MOVE_COMPARATOR);
                return sorted;
            }
        },
        EXPENSIVE {
            @Override
            Collection<Move> sort(Collection<Move> moves) {
                final List<Move> sorted = new ArrayList<>(moves);
                sorted.sort(EXPENSIVE_MOVE_COMPARATOR);
                return sorted;
            }
        };

        abstract Collection<Move> sort(Collection<Move> moves);
    }

    /**
     * Constructor for StockAlphaBeta.
     * Uses the ImprovedStandardBoardEvaluator with default parameters.
     * @param searchDepth The maximum search depth.
     */
    public StockAlphaBeta(final int searchDepth) {
        this(searchDepth, ImprovedStandardBoardEvaluator.get(), true);
    }

    /**
     * Constructor for StockAlphaBeta that accepts a custom BoardEvaluator.
     * @param searchDepth The maximum search depth.
     * @param evaluator The BoardEvaluator to use.
     */
    public StockAlphaBeta(final int searchDepth, final BoardEvaluator evaluator) {
        this(searchDepth, evaluator, true);
    }

    /**
     * Internal constructor.
     * @param searchDepth The maximum search depth.
     * @param evaluator The BoardEvaluator to use.
     * @param trackBoardEvaluations If true, this instance will increment the boardsEvaluated count.
     *                              Set to false if the evaluator is external and tracks its own evaluations.
     */
    private StockAlphaBeta(final int searchDepth, final BoardEvaluator evaluator, final boolean trackBoardEvaluations) {
        this.evaluator = evaluator;
        this.searchDepth = searchDepth;
        this.boardsEvaluated = 0;
        this.quiescenceCount = 0;
        this.trackBoardEvaluations = trackBoardEvaluations; // Not strictly needed if boardEvaluated is an instance var
        this.transpositionTable = new TranspositionTable(); // Use default TT size
    }

    /**
     * Constructor for StockAlphaBeta that accepts a custom BoardEvaluator and Transposition Table size.
     * @param searchDepth The maximum search depth.
     * @param evaluator The BoardEvaluator to use.
     * @param ttSizeInMB The size of the transposition table in Megabytes.
     */
    public StockAlphaBeta(final int searchDepth, final BoardEvaluator evaluator, final int ttSizeInMB) {
        this.evaluator = evaluator;
        this.searchDepth = searchDepth;
        this.boardsEvaluated = 0;
        this.quiescenceCount = 0;
        this.trackBoardEvaluations = true; // Assuming this constructor implies primary ownership
        int numEntries = (ttSizeInMB * 1024 * 1024) / 32; // Approx 32 bytes per entry
        if (numEntries <=0) numEntries = TranspositionTable.DEFAULT_NUM_ENTRIES; // Fallback to default if calc is too small
        this.transpositionTable = new TranspositionTable(numEntries);
    }


    @Override
    public String toString() {
        return "StockAB";
    }

    @Override
    public long getNumBoardsEvaluated() {
        return this.boardsEvaluated;
    }

    @Override
    public Move execute(final Board board) {
        final long startTime = System.currentTimeMillis();
        this.transpositionTable.clear(); // Clear TT at the start of a new root search
        this.boardsEvaluated = 0;      // Reset board evaluation count for this move search
        this.quiescenceCount = 0;      // Reset quiescence count

        final Player currentPlayer = board.currentPlayer();
        Move bestMove = MoveFactory.getNullMove();
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;
        int currentValue;
        System.out.println(board.currentPlayer() + " THINKING with depth = " + this.searchDepth);
        int moveCounter = 1;
        int numMoves = board.currentPlayer().getLegalMoves().size();
        for (final Move move : MoveSorter.EXPENSIVE.sort(board.currentPlayer().getLegalMoves())) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            this.quiescenceCount = 0;
            final String s;
            if (moveTransition.getMoveStatus().isDone()) {
                final long candidateMoveStartTime = System.nanoTime();
                currentValue = currentPlayer.getAlliance().isWhite() ?
                        min(moveTransition.getToBoard(), this.searchDepth - 1, highestSeenValue, lowestSeenValue) :
                        max(moveTransition.getToBoard(), this.searchDepth - 1, highestSeenValue, lowestSeenValue);
                if (currentPlayer.getAlliance().isWhite() && currentValue > highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                    if (moveTransition.getToBoard().blackPlayer().isInCheckMate()) {
                        break;
                    }
                } else if (currentPlayer.getAlliance().isBlack() && currentValue < lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                    if (moveTransition.getToBoard().whitePlayer().isInCheckMate()) {
                        break;
                    }
                }
                final String quiescenceInfo = " " + score(currentPlayer, highestSeenValue, lowestSeenValue) + " q: " + this.quiescenceCount;
                s = "\t" + this + "(" + this.searchDepth + "), m: (" + moveCounter + "/" + numMoves + ") " + move + ", best:  " + bestMove +
                        quiescenceInfo + ", t: " + calculateTimeTaken(candidateMoveStartTime, System.nanoTime());
            } else {
                s = "\t" + this + "(" + this.searchDepth + ")" + ", m: (" + moveCounter + "/" + numMoves + ") " + move + " is illegal! best: " + bestMove;
            }
            System.out.println(s);
            setChanged();
            notifyObservers(s);
            moveCounter++;
        }

        final long executionTime = System.currentTimeMillis() - startTime;
        final String result = board.currentPlayer() + " SELECTS " + bestMove + " [#boards evaluated = " + this.boardsEvaluated +
                " time taken = " + executionTime / 1000 + " rate = " + (1000 * ((double) this.boardsEvaluated / executionTime));
        System.out.printf("%s SELECTS %s [#boards evaluated = %d, time taken = %s, rate = %.1f\n", board.currentPlayer(),
                bestMove, this.boardsEvaluated, BoardUtils.humanReadableElapsedTime(executionTime), (1000 * ((double) this.boardsEvaluated / executionTime)));
        setChanged();
        notifyObservers(result);
        return bestMove;
    }

    private static String score(final Player currentPlayer, final int highestSeenValue, final int lowestSeenValue) {
        return currentPlayer.getAlliance().isWhite() ? "[score: " + highestSeenValue + "]" : "[score: " + lowestSeenValue + "]";
    }

    private int max(final Board board, final int depth, final int alpha, final int beta) { // Renamed highest->alpha, lowest->beta
        final long zobristKey = ZobristHashing.computeInitialHash(board);
        final int originalAlpha = alpha; // For TT storing
        Move ttBestMove = null;

        final TranspositionTableEntry ttEntry = this.transpositionTable.lookup(zobristKey);
        if (ttEntry != null && ttEntry.getDepth() >= depth) {
            ttBestMove = ttEntry.getBestMove();
            switch (ttEntry.getFlag()) {
                case EXACT:
                    return ttEntry.getScore();
                case LOWER_BOUND: // True score is >= ttEntry.getScore()
                    if (ttEntry.getScore() >= beta) return ttEntry.getScore(); // Beta cutoff
                    alpha = Math.max(alpha, ttEntry.getScore()); // Update alpha if this lower bound is tighter
                    break;
                case UPPER_BOUND: // True score is <= ttEntry.getScore()
                    if (ttEntry.getScore() <= alpha) return ttEntry.getScore(); // Alpha cutoff
                    beta = Math.min(beta, ttEntry.getScore()); // Update beta if this upper bound is tighter
                    break;
            }
        }

        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            int eval = this.evaluator.evaluate(board, depth);
            if (depth > 0 || BoardUtils.isEndGame(board)) { // Store terminal or deeper quiescence nodes
                this.transpositionTable.store(zobristKey, depth, eval, TranspositionTableEntry.EntryType.EXACT, null);
            }
            return eval;
        }

        int currentHighest = alpha; // In a MAX node, we want to find a score >= beta. Start with alpha.
        Move bestMoveInThisNode = null;

        List<Move> moves = new ArrayList<>(MoveSorter.STANDARD.sort(board.currentPlayer().getLegalMoves()));
        if (ttBestMove != null) {
            if(moves.remove(ttBestMove)) {
                moves.add(0, ttBestMove);
            }
        }

        for (final Move move : moves) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                final Board toBoard = moveTransition.getToBoard();
                final int score = min(toBoard, depth - 1, currentHighest, beta); // Pass currentHighest as alpha

                if (score > currentHighest) {
                    currentHighest = score;
                    bestMoveInThisNode = move;
                }

                if (currentHighest >= beta) { // Beta cutoff (fail high)
                    this.transpositionTable.store(zobristKey, depth, currentHighest, TranspositionTableEntry.EntryType.LOWER_BOUND, bestMoveInThisNode);
                    return beta;
                }
            }
        }

        TranspositionTableEntry.EntryType storeFlag;
        if (currentHighest <= originalAlpha) { // Score didn't improve beyond original alpha
            storeFlag = TranspositionTableEntry.EntryType.UPPER_BOUND;
        } else { // No beta cutoff occurred, score is between originalAlpha and beta (or was beta itself)
            storeFlag = TranspositionTableEntry.EntryType.EXACT; // currentHighest > originalAlpha implies it's exact or new alpha
        }
        // If currentHighest was already beta due to cutoff, it's stored as LOWER_BOUND.
        // Here, if no cutoff, and currentHighest > originalAlpha, it's an exact score for this node's range.
        this.transpositionTable.store(zobristKey, depth, currentHighest, storeFlag, bestMoveInThisNode);
        return currentHighest;
    }

    private int min(final Board board, final int depth, final int alpha, final int beta) {
        final long zobristKey = ZobristHashing.computeInitialHash(board);
        final int originalBeta = beta; // For TT storing
        Move ttBestMove = null;

        final TranspositionTableEntry ttEntry = this.transpositionTable.lookup(zobristKey);
        if (ttEntry != null && ttEntry.getDepth() >= depth) {
            ttBestMove = ttEntry.getBestMove();
            switch (ttEntry.getFlag()) {
                case EXACT:
                    return ttEntry.getScore();
                case UPPER_BOUND: // True score is <= ttEntry.getScore()
                    if (ttEntry.getScore() <= alpha) return ttEntry.getScore(); // Alpha cutoff
                    beta = Math.min(beta, ttEntry.getScore()); // Update beta if this upper bound is tighter
                    break;
                case LOWER_BOUND: // True score is >= ttEntry.getScore()
                    if (ttEntry.getScore() >= beta) return ttEntry.getScore(); // Beta cutoff
                    alpha = Math.max(alpha, ttEntry.getScore()); // Update alpha if this lower bound is tighter
                    break;
            }
        }

        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            int eval = this.evaluator.evaluate(board, depth);
            if (depth > 0 || BoardUtils.isEndGame(board)) {
                 this.transpositionTable.store(zobristKey, depth, eval, TranspositionTableEntry.EntryType.EXACT, null);
            }
            return eval;
        }

        int currentLowest = beta;
        Move bestMoveInThisNode = null;

        List<Move> moves = new ArrayList<>(MoveSorter.STANDARD.sort(board.currentPlayer().getLegalMoves()));
        if (ttBestMove != null) {
            if(moves.remove(ttBestMove)) {
                moves.add(0, ttBestMove);
            }
        }

        for (final Move move : moves) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                final Board toBoard = moveTransition.getToBoard();
                final int score = max(toBoard, depth - 1, alpha, currentLowest);

                if (score < currentLowest) {
                    currentLowest = score;
                    bestMoveInThisNode = move;
                }

                if (currentLowest <= alpha) { // Alpha cutoff (fail low)
                    this.transpositionTable.store(zobristKey, depth, currentLowest, TranspositionTableEntry.EntryType.UPPER_BOUND, bestMoveInThisNode);
                    return alpha;
                }
            }
        }

        TranspositionTableEntry.EntryType storeFlag;
        if (currentLowest >= originalBeta) { // Score didn't improve beyond original beta
            storeFlag = TranspositionTableEntry.EntryType.LOWER_BOUND;
        } else { // No alpha cutoff occurred, score is between alpha and originalBeta
            storeFlag = TranspositionTableEntry.EntryType.EXACT;
        }
        this.transpositionTable.store(zobristKey, depth, currentLowest, storeFlag, bestMoveInThisNode);
        return currentLowest;
    }

    private int calculateQuiescenceDepth(final Board toBoard, final int depth) {
        if (depth == 1 && this.quiescenceCount < MAX_QUIESCENCE) {
            int activityMeasure = 0;
            if (toBoard.currentPlayer().isInCheck()) {
                activityMeasure++;
            }
            for (final Move move : BoardUtils.lastNMoves(toBoard, 2)) {
                if (move.isAttack()) {
                    activityMeasure++;
                }
            }
            if (activityMeasure >= 2) {
                this.quiescenceCount++;
                return 2;
            }
        }
        return depth - 1;
    }

    private static String calculateTimeTaken(final long start, final long end) {
        final long timeTaken = (end - start) / 1_000_000;
        return timeTaken + " ms";
    }
}
