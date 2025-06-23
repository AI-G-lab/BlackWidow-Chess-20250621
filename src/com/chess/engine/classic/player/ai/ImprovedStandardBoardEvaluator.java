package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.Move;
import com.chess.engine.classic.pieces.Piece;
import com.chess.engine.classic.player.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static com.chess.engine.classic.pieces.Piece.PieceType.BISHOP;

public final class ImprovedStandardBoardEvaluator implements BoardEvaluator {

    private final EvaluationParameters params;

    // Evaluation cache to avoid redundant calculations
    // Each instance of the evaluator will have its own cache.
    private final Map<String, Integer> evaluationCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000; // This could also be part of EvaluationParameters if desired

    private static final ImprovedStandardBoardEvaluator DEFAULT_INSTANCE = new ImprovedStandardBoardEvaluator();

    /**
     * Private constructor for the default instance.
     * Uses default evaluation parameters.
     */
    private ImprovedStandardBoardEvaluator() {
        this(new EvaluationParameters());
    }

    /**
     * Constructor that takes a specific set of evaluation parameters.
     * @param params The evaluation parameters to use.
     */
    public ImprovedStandardBoardEvaluator(EvaluationParameters params) {
        this.params = params;
    }

    /**
     * Gets the singleton instance of the ImprovedStandardBoardEvaluator
     * that uses default parameters.
     * @return The singleton instance with default parameters.
     */
    public static ImprovedStandardBoardEvaluator get() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public int evaluate(final Board board, final int depth) {
        // Create cache key from board position
        // Note: If parameters change, the cache should ideally be invalidated or include params in key.
        // For SPSA, new evaluator instances are created for different param sets, so each has its own cache.
        final String cacheKey = createCacheKey(board, depth);
        
        Integer cachedResult = evaluationCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        final int evaluation = score(board.whitePlayer(), depth) - score(board.blackPlayer(), depth);
        
        if (evaluationCache.size() < MAX_CACHE_SIZE) {
            evaluationCache.put(cacheKey, evaluation);
        }
        
        return evaluation;
    }

    private String createCacheKey(final Board board, final int depth) {
        // Simple hash-based cache key - in production, use a proper zobrist hash
        // For SPSA, since each parameter configuration will have its own evaluator instance,
        // this cache key is per-instance, effectively per-parameter-set.
        return board.toString().hashCode() + "_" + depth;
    }

    private int score(final Player player, final int depth) {
        return mobility(player) +
               cramping(player) +
               checkOrCheckmate(player, depth) +
               attacks(player) +
               castle(player) +
               pieceEvaluations(player) +
               pawnStructure(player) + // These still use singletons; could be parameterized too if needed
               kingSafety(player) +   // These still use singletons; could be parameterized too if needed
               pieceActivity(player) +
               centralControl(player) +
               development(player);
    }

    private int mobility(final Player player) {
        final int ownMobility = player.getLegalMoves().size();
        final int opponentMobility = player.getOpponent().getLegalMoves().size();
        return this.params.mobilityMultiplier * (ownMobility - opponentMobility);
    }

    private int cramping(final Player player) {
        final int own = player.getLegalMoves().size();
        final int opp = player.getOpponent().getLegalMoves().size();
        if (opp == 0) return 0;
        final float ratio = (float) own / opp;
        return ratio > 1.5f ? (int) ((ratio - 1.5f) * 10) * this.params.crampingMultiplier : 0;
    }

    private int checkOrCheckmate(final Player player, final int depth) {
        return player.getOpponent().isInCheckMate() ? 
            this.params.checkMateBonus * depthBonus(depth) : check(player);
    }

    private int check(final Player player) {
        return player.getOpponent().isInCheck() ? this.params.checkBonus : 0;
    }

    private int depthBonus(final int depth) {
        // Uses checkmateDepthBonusMultiplier from params
        return depth == 0 ? 1 : this.params.checkmateDepthBonusMultiplier * depth;
    }

    private int castle(final Player player) {
        return player.isCastled() ? this.params.castleBonus : 0;
    }

    private int attacks(final Player player) {
        int score = 0;
        for (final Move move : player.getLegalMoves()) {
            if (move.isAttack()) {
                final Piece mover = move.getMovedPiece();
                final Piece victim = move.getAttackedPiece();

                if (mover.getPieceValue() <= victim.getPieceValue()) {
                    // The logic for attack score: (victim_value - mover_value)/10 + 1
                    // This part is not directly using a top-level parameter from EvaluationParameters,
                    // but the final score is multiplied by attackMultiplier.
                    // If this internal scoring (e.g., the divisor 10, or the +1) needs tuning,
                    // it would require adding more fields to EvaluationParameters.
                    score += (victim.getPieceValue() - mover.getPieceValue()) / 10 + 1;
                }
            }
        }
        return score * this.params.attackMultiplier;
    }

    private int pieceEvaluations(final Player player) {
        int score = 0;
        int bishops = 0;
        
        for (final int index : player.getActivePieces()) {
            final Piece piece = player.getBoard().getPiece(index);
            if (piece != null) {
                // Piece value and location bonus are inherent to the Piece objects themselves,
                // not part of the tunable params in EvaluationParameters at this level.
                score += piece.getPieceValue() + piece.locationBonus();
                if (piece.getPieceType() == BISHOP) {
                    bishops++;
                }
            }
        }
        // Adds the two bishops bonus from parameters
        return score + (bishops >= 2 ? this.params.twoBishopsBonus : 0);
    }

    // PawnStructureAnalyzer and ImprovedKingSafetyAnalyzer are still singletons.
    // If their internal logic needs tuning, they would also need similar refactoring.
    private int pawnStructure(final Player player) {
        return PawnStructureAnalyzer.get().pawnStructureScore(player);
    }

    private int kingSafety(final Player player) {
        return ImprovedKingSafetyAnalyzer.get().calculateKingSafety(player);
    }

    private int pieceActivity(final Player player) {
        int activity = 0;
        for (final int index : player.getActivePieces()) {
            final Piece piece = player.getBoard().getPiece(index);
            if (piece != null) {
                if (piece.isFirstMove()) {
                    activity -= this.params.pieceActivityBonus;
                } else {
                    activity += this.params.pieceActivityBonus;
                }
            }
        }
        return activity;
    }

    private int centralControl(final Player player) {
        int centralControlScore = 0; // Renamed to avoid conflict with class member if any
        final int[] centralSquares = {27, 28, 35, 36}; // d4, e4, d5, e5
        
        for (final Move move : player.getLegalMoves()) {
            final int destination = move.getDestinationCoordinate();
            for (final int centralSquare : centralSquares) {
                if (destination == centralSquare) {
                    centralControlScore += this.params.centralControlBonus;
                    break;
                }
            }
        }
        return centralControlScore;
    }

    private int development(final Player player) {
        int developmentScore = 0; // Renamed to avoid conflict
        final boolean isWhite = player.getAlliance().isWhite();
        
        for (final int index : player.getActivePieces()) {
            final Piece piece = player.getBoard().getPiece(index);
            if (piece != null && !piece.isFirstMove()) {
                switch (piece.getPieceType()) {
                    case KNIGHT:
                    case BISHOP:
                        if (isWhite && index >= 48 || !isWhite && index <= 15) {
                            developmentScore -= this.params.developmentBonus;
                        } else {
                            developmentScore += this.params.developmentBonus;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return developmentScore;
    }

    public String evaluationDetails(final Board board, final int depth) {
        final Player white = board.whitePlayer();
        final Player black = board.blackPlayer();
        
        return String.format(
            "=== IMPROVED BOARD EVALUATION ===\n" +
            "White Mobility: %d\n" +
            "White King Threats: %d\n" +
            "White Attacks: %d\n" +
            "White Castle: %d\n" +
            "White Piece Eval: %d\n" +
            "White Pawn Structure: %d\n" +
            "White King Safety: %d\n" +
            "White Piece Activity: %d\n" +
            "White Central Control: %d\n" +
            "White Development: %d\n" +
            "---------------------\n" +
            "Black Mobility: %d\n" +
            "Black King Threats: %d\n" +
            "Black Attacks: %d\n" +
            "Black Castle: %d\n" +
            "Black Piece Eval: %d\n" +
            "Black Pawn Structure: %d\n" +
            "Black King Safety: %d\n" +
            "Black Piece Activity: %d\n" +
            "Black Central Control: %d\n" +
            "Black Development: %d\n" +
            "---------------------\n" +
            "Final Score: %d\n" +
            "Cache Size: %d\n",
            mobility(white), checkOrCheckmate(white, depth), attacks(white), castle(white),
            pieceEvaluations(white), pawnStructure(white), kingSafety(white),
            pieceActivity(white), centralControl(white), development(white),
            mobility(black), checkOrCheckmate(black, depth), attacks(black), castle(black),
            pieceEvaluations(black), pawnStructure(black), kingSafety(black),
            pieceActivity(black), centralControl(black), development(black),
            evaluate(board, depth), evaluationCache.size()
        );
    }

    // Method to clear cache when needed
    public void clearCache() {
        evaluationCache.clear();
    }
}
