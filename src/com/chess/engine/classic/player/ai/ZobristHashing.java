package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.Alliance;
import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.BoardUtils;
import com.chess.engine.classic.pieces.Piece;
import com.chess.engine.classic.pieces.Pawn;

import java.security.SecureRandom; // Using SecureRandom for better quality random numbers

/**
 * Utility class for generating Zobrist hash keys for chess board positions.
 * <p>
 * Zobrist hashing is a technique to create a hash value for a board state by XORing
 * pseudo-random numbers associated with each possible feature of the state (e.g.,
 * piece type at a square, side to move, castling rights, en passant target).
 * This allows for efficient storage and lookup in transposition tables.
 * <p>
 * This implementation initializes the random keys once and provides a method to
 * compute the hash for a given board state from scratch. Incremental updates
 * (updating the hash based on a move made) are a common optimization but are
 * not implemented in this version for simplicity of the initial hash computation.
 *
 * @see TranspositionTable
 */
public final class ZobristHashing {

    // PIECE_KEYS[pieceType.ordinal()][alliance.ordinal()][squareIndex]
    private static final long[][][] PIECE_KEYS = new long[Piece.PieceType.values().length][Alliance.values().length][BoardUtils.NUM_TILES];
    private static final long BLACK_TO_MOVE_KEY;
    private static final long[] CASTLING_KEYS = new long[4]; // Index 0: WK, 1: WQ, 2: BK, 3: BQ
    private static final long[] EN_PASSANT_FILE_KEYS = new long[8]; // Index by file (0-7)

    private static final SecureRandom RANDOM_NUMBER_GENERATOR = new SecureRandom();

    static {
        // Initialize piece keys: one for each piece type, for each color, on each square.
        for (Piece.PieceType pieceType : Piece.PieceType.values()) {
            for (Alliance alliance : Alliance.values()) {
                for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                    PIECE_KEYS[pieceType.ordinal()][alliance.ordinal()][i] = RANDOM_NUMBER_GENERATOR.nextLong();
                }
            }
        }

        // Initialize key for black to move (XORed if it's black's turn).
        BLACK_TO_MOVE_KEY = RANDOM_NUMBER_GENERATOR.nextLong();

        // Initialize keys for each of the four castling rights.
        // Order: White King-side, White Queen-side, Black King-side, Black Queen-side
        for (int i = 0; i < CASTLING_KEYS.length; i++) {
            CASTLING_KEYS[i] = RANDOM_NUMBER_GENERATOR.nextLong();
        }

        // Initialize keys for each possible en passant file.
        // An en passant target is typically identified by the file of the capturable pawn.
        for (int i = 0; i < EN_PASSANT_FILE_KEYS.length; i++) {
            EN_PASSANT_FILE_KEYS[i] = RANDOM_NUMBER_GENERATOR.nextLong();
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ZobristHashing() {
        throw new UnsupportedOperationException("ZobristHashing is a utility class and cannot be instantiated.");
    }

    /**
     * Computes the Zobrist hash for the given board state from scratch.
     * This method iterates over all pieces on the board, considers the current side to move,
     * active castling rights, and any en passant target square, XORing the corresponding
     * random keys to produce the final hash.
     *
     * @param board The {@link Board} for which to compute the Zobrist hash. Must not be null.
     * @return The 64-bit Zobrist hash key representing the board state.
     */
    public static long computeInitialHash(final Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null for Zobrist hash computation.");
        }
        long hash = 0L;

        // Piece positions
        for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
            final Piece piece = board.getPiece(i);
            if (piece != null) {
                hash ^= PIECE_KEYS[piece.getPieceType().ordinal()][piece.getPieceAllegiance().ordinal()][i];
            }
        }

        // Side to move
        if (board.currentPlayer().getAlliance() == Alliance.BLACK) {
            hash ^= BLACK_TO_MOVE_KEY;
        }

        // Castling rights
        if (board.whitePlayer().isKingSideCastleCapable()) hash ^= CASTLING_KEYS[0]; // WK
        if (board.whitePlayer().isQueenSideCastleCapable()) hash ^= CASTLING_KEYS[1]; // WQ
        if (board.blackPlayer().isKingSideCastleCapable()) hash ^= CASTLING_KEYS[2]; // BK
        if (board.blackPlayer().isQueenSideCastleCapable()) hash ^= CASTLING_KEYS[3]; // BQ

        // En passant target
        final Pawn enPassantPawn = board.getEnPassantPawn();
        if (enPassantPawn != null) {
            // The en passant target square is behind the pawn that just moved two squares.
            // The file of this target square is what matters for the FEN en passant notation.
            // The enPassantPawn is the pawn that *can be captured*.
            // The actual en passant *target square* is one rank behind this pawn from current player's perspective.
            // For hashing, we usually hash the file of the en passant *target square*.
            // Let's use the file of the enPassantPawn itself, as that's simpler to get from board.getEnPassantPawn().
            // The critical part is consistency.
            final int enPassantFile = BoardUtils.INSTANCE.getFile(enPassantPawn.getPiecePosition());
            hash ^= EN_PASSANT_FILE_KEYS[enPassantFile];
        }
        return hash;
    }

    // TODO: Implement incremental hash updates:
    // public static long updateHash(long currentHash, Move move, Board boardBeforeMove, Board boardAfterMove) { ... }
    // This would need detailed info about the move:
    // - Piece moved from, piece moved to
    // - Captured piece (if any) and its square
    // - Promotion piece type (if any)
    // - Changes in castling rights
    // - Change in en passant target square
    // - Change in side to move (always happens)
}
