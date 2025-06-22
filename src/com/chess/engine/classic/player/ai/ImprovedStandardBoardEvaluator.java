package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.Move;
import com.chess.engine.classic.pieces.Piece;
import com.chess.engine.classic.player.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static com.chess.engine.classic.pieces.Piece.PieceType.BISHOP;

public final class ImprovedStandardBoardEvaluator implements BoardEvaluator {

    private static final int CHECK_MATE_BONUS = 10000;
    private static final int CHECK_BONUS = 45;
    private static final int CASTLE_BONUS = 25;
    private static final int MOBILITY_MULTIPLIER = 2; // Increased from 1
    private static final int ATTACK_MULTIPLIER = 2; // Increased from 1
    private static final int TWO_BISHOPS_BONUS = 25;
    private static final int CRAMPING_MULTIPLIER = 2;
    
    // New evaluation constants
    private static final int PIECE_ACTIVITY_BONUS = 5;
    private static final int CENTRAL_CONTROL_BONUS = 10;
    private static final int DEVELOPMENT_BONUS = 15;

    // Evaluation cache to avoid redundant calculations
    private final Map<String, Integer> evaluationCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000;

    private static final ImprovedStandardBoardEvaluator INSTANCE = new ImprovedStandardBoardEvaluator();

    private ImprovedStandardBoardEvaluator() {}

    public static ImprovedStandardBoardEvaluator get() {
        return INSTANCE;
    }

    @Override
    public int evaluate(final Board board, final int depth) {
        // Create cache key from board position
        final String cacheKey = createCacheKey(board, depth);
        
        // Check cache first
        Integer cachedResult = evaluationCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        final int evaluation = score(board.whitePlayer(), depth) - score(board.blackPlayer(), depth);
        
        // Cache the result (with size limit)
        if (evaluationCache.size() < MAX_CACHE_SIZE) {
            evaluationCache.put(cacheKey, evaluation);
        }
        
        return evaluation;
    }

    private String createCacheKey(final Board board, final int depth) {
        // Simple hash-based cache key - in production, use a proper zobrist hash
        return board.toString().hashCode() + "_" + depth;
    }

    private static int score(final Player player, final int depth) {
        return mobility(player) +
               cramping(player) +
               checkOrCheckmate(player, depth) +
               attacks(player) +
               castle(player) +
               pieceEvaluations(player) +
               pawnStructure(player) +
               kingSafety(player) +
               pieceActivity(player) +
               centralControl(player) +
               development(player);
    }

    private static int mobility(final Player player) {
        final int ownMobility = player.getLegalMoves().size();
        final int opponentMobility = player.getOpponent().getLegalMoves().size();
        return MOBILITY_MULTIPLIER * (ownMobility - opponentMobility);
    }

    private static int cramping(final Player player) {
        final int own = player.getLegalMoves().size();
        final int opp = player.getOpponent().getLegalMoves().size();
        if (opp == 0) return 0;
        final float ratio = (float) own / opp;
        return ratio > 1.5f ? (int) ((ratio - 1.5f) * 10) * CRAMPING_MULTIPLIER : 0;
    }

    private static int checkOrCheckmate(final Player player, final int depth) {
        return player.getOpponent().isInCheckMate() ? 
            CHECK_MATE_BONUS * depthBonus(depth) : check(player);
    }

    private static int check(final Player player) {
        return player.getOpponent().isInCheck() ? CHECK_BONUS : 0;
    }

    private static int depthBonus(final int depth) {
        return depth == 0 ? 1 : 100 * depth;
    }

    private static int castle(final Player player) {
        return player.isCastled() ? CASTLE_BONUS : 0;
    }

    private static int attacks(final Player player) {
        int score = 0;
        for (final Move move : player.getLegalMoves()) {
            if (move.isAttack()) {
                final Piece mover = move.getMovedPiece();
                final Piece victim = move.getAttackedPiece();

                if (mover.getPieceValue() <= victim.getPieceValue()) {
                    score += (victim.getPieceValue() - mover.getPieceValue()) / 10 + 1;
                }
            }
        }
        return score * ATTACK_MULTIPLIER;
    }

    private static int pieceEvaluations(final Player player) {
        int score = 0;
        int bishops = 0;
        
        // Direct piece access instead of using indices
        for (final int index : player.getActivePieces()) {
            final Piece piece = player.getBoard().getPiece(index);
            if (piece != null) {
                score += piece.getPieceValue() + piece.locationBonus();
                if (piece.getPieceType() == BISHOP) {
                    bishops++;
                }
            }
        }
        
        return score + (bishops >= 2 ? TWO_BISHOPS_BONUS : 0);
    }

    private static int pawnStructure(final Player player) {
        return PawnStructureAnalyzer.get().pawnStructureScore(player);
    }

    private static int kingSafety(final Player player) {
        return ImprovedKingSafetyAnalyzer.get().calculateKingSafety(player);
    }

    // New evaluation functions
    private static int pieceActivity(final Player player) {
        int activity = 0;
        for (final int index : player.getActivePieces()) {
            final Piece piece = player.getBoard().getPiece(index);
            if (piece != null) {
                // Bonus for pieces that have moved from starting positions
                if (piece.isFirstMove()) {
                    activity -= PIECE_ACTIVITY_BONUS; // Penalty for unmoved pieces
                } else {
                    activity += PIECE_ACTIVITY_BONUS;
                }
            }
        }
        return activity;
    }

    private static int centralControl(final Player player) {
        int centralControl = 0;
        final int[] centralSquares = {27, 28, 35, 36}; // d4, e4, d5, e5
        
        for (final Move move : player.getLegalMoves()) {
            final int destination = move.getDestinationCoordinate();
            for (final int centralSquare : centralSquares) {
                if (destination == centralSquare) {
                    centralControl += CENTRAL_CONTROL_BONUS;
                    break;
                }
            }
        }
        return centralControl;
    }

    private static int development(final Player player) {
        int development = 0;
        final boolean isWhite = player.getAlliance().isWhite();
        
        // Check if knights and bishops are developed
        for (final int index : player.getActivePieces()) {
            final Piece piece = player.getBoard().getPiece(index);
            if (piece != null && !piece.isFirstMove()) {
                switch (piece.getPieceType()) {
                    case KNIGHT:
                    case BISHOP:
                        // Bonus for developed minor pieces
                        if (isWhite && index >= 48 || !isWhite && index <= 15) {
                            // Piece is still on back rank - not developed
                            development -= DEVELOPMENT_BONUS;
                        } else {
                            development += DEVELOPMENT_BONUS;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return development;
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
