package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.Move;
import com.chess.engine.classic.player.Player;
import com.chess.engine.classic.player.MoveTransition;
import com.chess.pgn.FenUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a single chess game played between two AI players.
 * This class is used by the {@link AITuner} to gather data for the SPSA optimization process.
 * It handles game initialization, turn-by-turn move execution by AI players,
 * and detection of game termination conditions (checkmate, stalemate, draws by repetition,
 * insufficient material, or exceeding a maximum move limit).
 */
public class SelfPlayGameRunner {

    private final MoveStrategy whitePlayerStrategy;
    private final MoveStrategy blackPlayerStrategy;
    private Board board; // Current state of the game board
    private final int maxGameMoves; // Maximum number of moves before game is declared a draw

    // For three-fold repetition detection: maps FEN string of board position to its occurrence count.
    private final Map<String, Integer> boardStateCounts = new HashMap<>();
    // Stores FEN strings of board positions for history, currently unused but could be for 50-move rule.
    private final List<String> boardHistory = new ArrayList<>();


    /**
     * Constructs a SelfPlayGameRunner to play a game between two AI instances.
     *
     * @param whitePlayerParams {@link EvaluationParameters} for the AI playing White.
     * @param blackPlayerParams {@link EvaluationParameters} for the AI playing Black.
     * @param searchDepth The search depth to be used by both AI players.
     * @param initialFen The Forsyth-Edwards Notation (FEN) string for the starting board position.
     *                   If null or empty, the standard chess starting position is used.
     * @param maxGameMoves The maximum number of moves allowed in the game before it's declared a draw.
     *                     This prevents infinitely long games.
     */
    public SelfPlayGameRunner(EvaluationParameters whitePlayerParams,
                              EvaluationParameters blackPlayerParams,
                              int searchDepth,
                              String initialFen,
                              int maxGameMoves) {

        // Create AI strategies with their respective evaluation parameters
        // The StockAlphaBeta constructor might need to be adapted if it doesn't already support
        // passing a custom BoardEvaluator instance.
        // For now, assuming MoveStrategy is an interface and StockAlphaBeta is an implementation.
        // We might need to adjust StockAlphaBeta to accept an ImprovedStandardBoardEvaluator instance.
        // This is a placeholder; actual instantiation might differ based on StockAlphaBeta's design.
        this.whitePlayerStrategy = new StockAlphaBeta(searchDepth, new ImprovedStandardBoardEvaluator(whitePlayerParams));
        this.blackPlayerStrategy = new StockAlphaBeta(searchDepth, new ImprovedStandardBoardEvaluator(blackPlayerParams));

        this.maxGameMoves = maxGameMoves;

        if (initialFen != null && !initialFen.trim().isEmpty()) {
            this.board = FenUtilities.createGameFromFEN(initialFen);
        } else {
            this.board = Board.createStandardBoard();
        }
        recordBoardState();
    }

    /**
     * Plays a full game from the initialized board state until a terminal condition is met.
     * Terminal conditions include checkmate, stalemate, draw by three-fold repetition,
     * draw by insufficient material, or reaching the maximum allowed game moves.
     *
     * @return An integer representing the game outcome:
     *         <ul>
     *           <li>1 if White wins</li>
     *           <li>-1 if Black wins</li>
     *           <li>0 if the game is a draw</li>
     *         </ul>
     */
    public int playGame() {
        int moveCount = 0;

        while (moveCount < this.maxGameMoves) {
            if (isGameOver()) {
                break;
            }

            final Player currentPlayer = board.currentPlayer();
            final MoveStrategy currentStrategy = currentPlayer.getAlliance().isWhite() ? whitePlayerStrategy : blackPlayerStrategy;

            final Move bestMove = currentStrategy.execute(board); // AI calculates its best move

            if (bestMove == null || bestMove.isNullMove()) {
                // This can happen if a player has no legal moves but is not in checkmate (stalemate already handled by isGameOver)
                // or if there's an unexpected issue with the AI.
                // isGameOver() should ideally catch all terminal states.
                // If AI returns null for a non-terminal state, it's problematic.
                System.err.println("Warning: AI for " + currentPlayer.getAlliance() +
                                   " returned null or NullMove in a non-terminal state. Board FEN: " + FenUtilities.createFENFromGame(board));
                break; // Consider this a draw or an error state.
            }

            final MoveTransition transition = board.currentPlayer().makeMove(bestMove);
            if (transition.getMoveStatus().isDone()) {
                this.board = transition.getToBoard();
                recordBoardState();
            } else {
                // Illegal move suggested by AI, should not happen with a correct AI.
                System.err.println("Error: AI suggested an illegal move: " + bestMove);
                // This indicates a deeper problem, perhaps return a specific error code or throw exception.
                // For SPSA, this game might be invalidated.
                return 0; // Or some other indicator of an error/invalid game.
            }
            moveCount++;
        }
        return determineOutcome(moveCount);
    }

    /**
     * Records the current board state (FEN string) for three-fold repetition detection.
     * Uses a FEN representation that excludes move counters.
     */
    private void recordBoardState() {
        String boardFen = FenUtilities.createFENFromBoard(this.board);
        boardStateCounts.put(boardFen, boardStateCounts.getOrDefault(boardFen, 0) + 1);
        boardHistory.add(boardFen);
    }

    /**
     * Checks if the current board position has been repeated three times.
     * @return true if a three-fold repetition is detected, false otherwise.
     */
    private boolean isThreeFoldRepetition() {
        String currentBoardFen = FenUtilities.createFENFromBoard(this.board);
        return boardStateCounts.getOrDefault(currentBoardFen, 0) >= 3;
    }

    /**
     * Determines if the game has reached a terminal state.
     * Checks for checkmate, stalemate, three-fold repetition, and insufficient material.
     * The maximum move limit is checked in the main game loop.
     * @return true if the game is over, false otherwise.
     */
    private boolean isGameOver() {
        if (board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStalemate()) {
            return true;
        }
        if (isThreeFoldRepetition()) {
            return true;
        }
        if (board.isInsufficientMaterial()) {
            return true;
        }
        // Note: 50-move rule is not explicitly implemented here but could be added.
        // The maxGameMoves limit in playGame() serves as a hard stop.
        return false;
    }

    /**
     * Determines the outcome of the game based on the final board state.
     * @param moveCount The total number of moves played in the game.
     * @return 1 if White wins, -1 if Black wins, 0 for a draw.
     */
    private int determineOutcome(int moveCount) {
        if (board.whitePlayer().isInCheckMate()) {
            return -1; // Black wins
        }
        if (board.blackPlayer().isInCheckMate()) {
            return 1; // White wins
        }
        // All other terminal states (stalemate, three-fold, insufficient material, max moves reached) are draws.
        if (board.currentPlayer().isInStalemate() ||
            isThreeFoldRepetition() ||
            board.isInsufficientMaterial() ||
            moveCount >= this.maxGameMoves) {
            return 0; // Draw
        }
        // Should not be reached if isGameOver() is comprehensive and called correctly.
        System.err.println("Warning: determineOutcome called on a non-terminal board state or unaccounted condition. FEN: " + FenUtilities.createFENFromGame(board));
        return 0; // Default to draw in unexpected situations.
    }
}
