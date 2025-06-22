package com.chess.engine.classic.player.ai;

import com.chess.engine.classic.board.Move;
import com.chess.engine.classic.pieces.Piece;
import com.chess.engine.classic.player.Player;

public final class ImprovedKingSafetyAnalyzer {

    private static final ImprovedKingSafetyAnalyzer INSTANCE = new ImprovedKingSafetyAnalyzer();
    
    // King safety constants
    private static final int CENTER_KING_PENALTY = 30;
    private static final int UNCASTLED_KING_PENALTY = 20;
    private static final int PAWN_SHIELD_BONUS = 15;
    private static final int KING_TROPISM_MULTIPLIER = 2;
    private static final int OPEN_FILE_PENALTY = 25;
    
    // Precomputed file and rank arrays for performance
    private static final int[] FILE_OF_SQUARE = new int[64];
    private static final int[] RANK_OF_SQUARE = new int[64];
    
    static {
        // Initialize file and rank lookup tables
        for (int square = 0; square < 64; square++) {
            FILE_OF_SQUARE[square] = square % 8;
            RANK_OF_SQUARE[square] = square / 8;
        }
    }

    private ImprovedKingSafetyAnalyzer() {}

    public static ImprovedKingSafetyAnalyzer get() {
        return INSTANCE;
    }

    public int calculateKingSafety(final Player player) {
        int safety = 0;
        final int kingSquare = player.getPlayerKing().getPiecePosition();
        
        // Basic king position safety
        safety += basicKingPositionSafety(player, kingSquare);
        
        // Pawn shield evaluation
        safety += evaluatePawnShield(player, kingSquare);
        
        // King tropism (enemy pieces near king)
        safety += evaluateKingTropism(player, kingSquare);
        
        // Open files near king
        safety += evaluateOpenFiles(player, kingSquare);
        
        return safety;
    }

    private int basicKingPositionSafety(final Player player, final int kingSquare) {
        int safety = 0;
        
        if (!player.isCastled()) {
            final int kingFile = FILE_OF_SQUARE[kingSquare];
            
            // Penalty for king in center files
            if (kingFile >= 3 && kingFile <= 4) {
                safety -= CENTER_KING_PENALTY;
            } else {
                safety -= UNCASTLED_KING_PENALTY;
            }
        }
        
        return safety;
    }

    private int evaluatePawnShield(final Player player, final int kingSquare) {
        int shieldBonus = 0;
        final boolean isWhite = player.getAlliance().isWhite();
        final int kingFile = FILE_OF_SQUARE[kingSquare];
        final int kingRank = RANK_OF_SQUARE[kingSquare];
        
        // Check for pawn shield in front of king
        final int shieldRank = isWhite ? kingRank - 1 : kingRank + 1;
        
        if (shieldRank >= 0 && shieldRank <= 7) {
            // Check files around king for pawn shield
            for (int fileOffset = -1; fileOffset <= 1; fileOffset++) {
                final int checkFile = kingFile + fileOffset;
                if (checkFile >= 0 && checkFile <= 7) {
                    final int shieldSquare = shieldRank * 8 + checkFile;
                    final Piece piece = player.getBoard().getPiece(shieldSquare);
                    
                    if (piece != null && 
                        piece.getPieceType() == Piece.PieceType.PAWN && 
                        piece.getPieceAllegiance() == player.getAlliance()) {
                        shieldBonus += PAWN_SHIELD_BONUS;
                    }
                }
            }
        }
        
        return shieldBonus;
    }

    private int evaluateKingTropism(final Player player, final int kingSquare) {
        int tropismPenalty = 0;
        
        // Check distance of enemy pieces to our king
        for (final Move enemyMove : player.getOpponent().getLegalMoves()) {
            final int enemyDestination = enemyMove.getDestinationCoordinate();
            final int distance = calculateManhattanDistance(kingSquare, enemyDestination);
            final Piece enemyPiece = enemyMove.getMovedPiece();
            
            if (distance <= 3) { // Only consider close threats
                final int pieceValue = enemyPiece.getPieceValue();
                tropismPenalty -= (4 - distance) * (pieceValue / 100) * KING_TROPISM_MULTIPLIER;
            }
        }
        
        return tropismPenalty;
    }

    private int evaluateOpenFiles(final Player player, final int kingSquare) {
        int penalty = 0;
        final int kingFile = FILE_OF_SQUARE[kingSquare];
        
        // Check if files near king are open (no pawns)
        for (int fileOffset = -1; fileOffset <= 1; fileOffset++) {
            final int checkFile = kingFile + fileOffset;
            if (checkFile >= 0 && checkFile <= 7) {
                if (isFileOpen(player, checkFile)) {
                    penalty -= OPEN_FILE_PENALTY;
                }
            }
        }
        
        return penalty;
    }

    private boolean isFileOpen(final Player player, final int file) {
        // Check if there are any pawns (from either side) on this file
        for (int rank = 0; rank < 8; rank++) {
            final int square = rank * 8 + file;
            final Piece piece = player.getBoard().getPiece(square);
            if (piece != null && piece.getPieceType() == Piece.PieceType.PAWN) {
                return false; // File is not open
            }
        }
        return true; // File is open
    }

    private static int calculateManhattanDistance(final int square1, final int square2) {
        final int file1 = FILE_OF_SQUARE[square1];
        final int rank1 = RANK_OF_SQUARE[square1];
        final int file2 = FILE_OF_SQUARE[square2];
        final int rank2 = RANK_OF_SQUARE[square2];
        
        return Math.abs(file1 - file2) + Math.abs(rank1 - rank2);
    }

    private static int calculateChebyshevDistance(final int square1, final int square2) {
        final int file1 = FILE_OF_SQUARE[square1];
        final int rank1 = RANK_OF_SQUARE[square1];
        final int file2 = FILE_OF_SQUARE[square2];
        final int rank2 = RANK_OF_SQUARE[square2];
        
        return Math.max(Math.abs(file1 - file2), Math.abs(rank1 - rank2));
    }

    // Legacy method for compatibility with existing code
    public int calculateKingTropism(final Player player) {
        return evaluateKingTropism(player, player.getPlayerKing().getPiecePosition());
    }

    public int gptKingSafety(final Player player) {
        return basicKingPositionSafety(player, player.getPlayerKing().getPiecePosition());
    }
}
