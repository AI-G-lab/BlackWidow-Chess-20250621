package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.Move;
import com.chess.engine.classic.pieces.Piece;
import com.chess.engine.classic.player.Player;

import static com.chess.engine.classic.pieces.Piece.PieceType.BISHOP;

/**
 * A standard board evaluator that assigns a numerical score to a given board state.
 * This evaluator considers various factors such as material balance, mobility, king safety,
 * pawn structure, and other tactical and positional elements.
 * It uses a singleton pattern to ensure a single instance.
 */
public final class StandardBoardEvaluator implements BoardEvaluator {

    private static final int CHECK_MATE_BONUS = 10000;      // A large bonus awarded for delivering checkmate. Scaled by depth.
    private static final int CHECK_BONUS = 45;               // A bonus for putting the opponent's king in check.
    private static final int CASTLE_BONUS = 25;              // A bonus for having castled, improving king safety and rook development.
    private static final int MOBILITY_MULTIPLIER = 1;        // Multiplier for the mobility score. Higher values emphasize piece activity.
    private static final int ATTACK_MULTIPLIER = 1;          // Multiplier for the attack score. Higher values emphasize aggressive moves.
    private static final int TWO_BISHOPS_BONUS = 25;         // A bonus for possessing the bishop pair, which can control many squares.
    private static final int CRAMPING_MULTIPLIER = 2;        // Multiplier for the cramping penalty. Higher values more severely penalize restricted mobility.

    private static final StandardBoardEvaluator INSTANCE = new StandardBoardEvaluator();

    private StandardBoardEvaluator() {}

    /**
     * Gets the singleton instance of the StandardBoardEvaluator.
     * @return The singleton instance.
     */
    public static StandardBoardEvaluator get() {
        return INSTANCE;
    }

    /**
     * Evaluates the given board position and returns a score.
     * A positive score favors white, while a negative score favors black.
     * The evaluation is based on the difference between the white player's score and the black player's score.
     *
     * @param board The board to evaluate.
     * @param depth The current search depth (used for scaling checkmate bonus).
     * @return The numerical evaluation of the board position.
     */
    @Override
    public int evaluate(final Board board, final int depth) {
        return score(board.whitePlayer(), depth) - score(board.blackPlayer(), depth);
    }

    private static int score(final Player player, final int depth) {
        final int score =  mobility(player) +                           // Bonus for having more legal moves than the opponent
                           cramping(player) +                         // Penalty if the opponent has significantly more moves
                           check_or_checkmate(player, depth) +          // Large bonus for checkmate, smaller for check
                           attacks(player) +                            // Bonus for attacks, especially on higher-value pieces
                           castle(player) +                             // Bonus for having castled
                           pieceEvaluations(player) +                   // Sum of piece values and positional bonuses (includes two bishops bonus)
                           pawnStructure(player) +                      // Score based on pawn formations
                           kingSafety(player);                          // Score based on the safety of the king

        return score;
    }

    private static int mobility(final Player player) {
        return MOBILITY_MULTIPLIER * (player.getLegalMoves().size() - player.getOpponent().getLegalMoves().size());
    }

    private static int cramping(final Player player) {
        int own = player.getLegalMoves().size();
        int opp = player.getOpponent().getLegalMoves().size();
        if (opp == 0) return 0;
        float ratio = (float) own / opp;
        return ratio > 1.5f ? (int) ((ratio - 1.5f) * 10) * CRAMPING_MULTIPLIER : 0;
    }

    private static int check_or_checkmate(final Player player, final int depth) {
        return player.getOpponent().isInCheckMate() ? CHECK_MATE_BONUS * depthBonus(depth) : check(player);
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
                    score++;
                }
            }
        }
        return score * ATTACK_MULTIPLIER;
    }

    private static int pieceEvaluations(final Player player) {
        int score = 0;
        int bishops = 0;
        for (final int index : player.getActivePieces()) {
            final Piece p = player.getBoard().getPiece(index);
            score += p.getPieceValue() + p.locationBonus();
            if (p.getPieceType() == BISHOP) bishops++;
        }
        return score + (bishops == 2 ? TWO_BISHOPS_BONUS : 0);
    }

    private static int pawnStructure(final Player player) {
        return PawnStructureAnalyzer.get().pawnStructureScore(player);
    }

    private static int kingSafety(final Player player) {
        int f1 = KingSafetyAnalyzer.get().calculateKingTropism(player);
        int f2 = KingSafetyAnalyzer.get().gptKingSafety(player);
        //int f3 = KingSafetyAnalyzer.get().pawnShieldPenalty(player);
        //return 0;
        return f1 + f2;
        //return f1;
    }

    /**
     * Provides a detailed string representation of the board evaluation components.
     * This includes scores for mobility, king threats, attacks, castling, piece evaluations,
     * and pawn structure for both white and black, along with the final aggregate score.
     *
     * @param board The board for which to get evaluation details.
     * @param depth The current search depth.
     * @return A string detailing the evaluation components.
     */
    public String evaluationDetails(final Board board, final int depth) {
        return
                ("White Mobility : " + mobility(board.whitePlayer()) + "\n") +
                        "White kingThreats : " + check_or_checkmate(board.whitePlayer(), depth) + "\n" +
                        "White attacks : " + attacks(board.whitePlayer()) + "\n" +
                        "White castle : " + castle(board.whitePlayer()) + "\n" +
                        "White pieceEval : " + pieceEvaluations(board.whitePlayer()) + "\n" +
                        "White pawnStructure : " + pawnStructure(board.whitePlayer()) + "\n" +
                        "---------------------\n" +
                        "Black Mobility : " + mobility(board.blackPlayer()) + "\n" +
                        "Black kingThreats : " + check_or_checkmate(board.blackPlayer(), depth) + "\n" +
                        "Black attacks : " + attacks(board.blackPlayer()) + "\n" +
                        "Black castle : " + castle(board.blackPlayer()) + "\n" +
                        "Black pieceEval : " + pieceEvaluations(board.blackPlayer()) + "\n" +
                        "Black pawnStructure : " + pawnStructure(board.blackPlayer()) + "\n\n" +
                        "Final Score = " + evaluate(board, depth);
    }

}
