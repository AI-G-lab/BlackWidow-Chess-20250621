package com.chess.engine.classic.board;

import com.chess.engine.classic.Alliance;
import com.chess.engine.classic.board.Move.MoveFactory;
import com.chess.engine.classic.pieces.*;
import com.chess.engine.classic.player.BlackPlayer;
import com.chess.engine.classic.player.Player;
import com.chess.engine.classic.player.WhitePlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Board {

    private final Piece[] boardConfig;
    private final int[] whitePieces;
    private final int[] blackPieces;
    private final WhitePlayer whitePlayer;
    private final BlackPlayer blackPlayer;
    private final Player currentPlayer;
    private final Pawn enPassantPawn;
    private final Move transitionMove;

    private static final Board STANDARD_BOARD = createStandardBoardImpl();

    private Board(final Builder builder) {
        this.boardConfig = builder.boardConfig;
        this.whitePieces = calculateActiveIndexes(builder.boardConfig, Alliance.WHITE);
        this.blackPieces = calculateActiveIndexes(builder.boardConfig, Alliance.BLACK);
        this.enPassantPawn = builder.enPassantPawn;
        final Collection<Move> whiteStandardMoves = calculateLegalMoves(builder.boardConfig, this.whitePieces);
        final Collection<Move> blackStandardMoves = calculateLegalMoves(builder.boardConfig, this.blackPieces);
        this.whitePlayer = new WhitePlayer(this, establishKing(this.whitePieces, this.boardConfig), whiteStandardMoves, blackStandardMoves);
        this.blackPlayer = new BlackPlayer(this, establishKing(this.blackPieces, this.boardConfig), whiteStandardMoves, blackStandardMoves);
        this.currentPlayer = builder.nextMoveMaker.choosePlayerByAlliance(this.whitePlayer, this.blackPlayer);
        this.transitionMove = builder.transitionMove != null ? builder.transitionMove : MoveFactory.getNullMove();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
            final String tileText = prettyPrint(this.boardConfig[i]);
            builder.append(String.format("%3s", tileText));
            if ((i + 1) % 8 == 0) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private static String prettyPrint(final Piece piece) {
        if(piece != null) {
            return piece.getPieceAllegiance().isBlack() ?
                    piece.toString().toLowerCase() : piece.toString();
        }
        return "-";
    }

    public Piece[] getBoardCopy() {
        return this.boardConfig.clone();
    }

    public int[] getBlackPieces() {
        return this.blackPieces;
    }

    public int[] getWhitePieces() {
        return this.whitePieces;
    }

    public Collection<Piece> getAllPieces() {
        final List<Piece> allPieces = new ArrayList<>(this.whitePieces.length + this.blackPieces.length);
        for (final int index : this.whitePieces) {
            allPieces.add(this.boardConfig[index]);
        }
        for (final int index : this.blackPieces) {
            allPieces.add(this.boardConfig[index]);
        }
        return Collections.unmodifiableList(allPieces);
    }

    public Collection<Move> getAllLegalMoves() {
        return Stream.concat(this.whitePlayer.getLegalMoves().stream(),
                this.blackPlayer.getLegalMoves().stream()).collect(Collectors.toList());
    }

    private static King establishKing(final int[] activeIndexes,
                                      final Piece[] boardConfig) {
        for (final int index : activeIndexes) {
            final Piece piece = boardConfig[index];
            if (piece.getPieceType() == Piece.PieceType.KING) {
                return (King) piece;
            }
        }
        throw new RuntimeException("No king found for player!");
    }

    public WhitePlayer whitePlayer() {
        return this.whitePlayer;
    }

    public BlackPlayer blackPlayer() {
        return this.blackPlayer;
    }

    public Player currentPlayer() {
        return this.currentPlayer;
    }

    public Piece getPiece(final int coordinate) {
        return this.boardConfig[coordinate];
    }

    public Pawn getEnPassantPawn() {
        return this.enPassantPawn;
    }

    public Move getTransitionMove() {
        return this.transitionMove;
    }

    public static Board createStandardBoard() {
        return STANDARD_BOARD;
    }

    private static Board createStandardBoardImpl() {
        final Builder builder = new Builder();
        // Black Layout
        builder.setPiece(PieceUtils.INSTANCE.getRook(Alliance.BLACK, 0, false));
        builder.setPiece(PieceUtils.INSTANCE.getKnight(Alliance.BLACK, 1, false));
        builder.setPiece(PieceUtils.INSTANCE.getBishop(Alliance.BLACK, 2, false));
        builder.setPiece(PieceUtils.INSTANCE.getQueen(Alliance.BLACK, 3, false));

        builder.setPiece(new King(Alliance.BLACK, 4, true, true));

        builder.setPiece(PieceUtils.INSTANCE.getBishop(Alliance.BLACK, 5, false));
        builder.setPiece(PieceUtils.INSTANCE.getKnight(Alliance.BLACK, 6, false));
        builder.setPiece(PieceUtils.INSTANCE.getRook(Alliance.BLACK, 7, false));

        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.BLACK, 8, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.BLACK, 9, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.BLACK, 10, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.BLACK, 11, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.BLACK, 12, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.BLACK, 13, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.BLACK, 14, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.BLACK, 15, false));

        // White Layout
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, 48, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, 49, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, 50, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, 51, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, 52, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, 53, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, 54, false));
        builder.setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, 55, false));

        builder.setPiece(PieceUtils.INSTANCE.getRook(Alliance.WHITE, 56, false));
        builder.setPiece(PieceUtils.INSTANCE.getKnight(Alliance.WHITE, 57, false));
        builder.setPiece(PieceUtils.INSTANCE.getBishop(Alliance.WHITE, 58, false));
        builder.setPiece(PieceUtils.INSTANCE.getQueen(Alliance.WHITE, 59, false));

        builder.setPiece(new King(Alliance.WHITE, 60, true, true));

        builder.setPiece(PieceUtils.INSTANCE.getBishop(Alliance.WHITE, 61, false));
        builder.setPiece(PieceUtils.INSTANCE.getKnight(Alliance.WHITE, 62, false));
        builder.setPiece(PieceUtils.INSTANCE.getRook(Alliance.WHITE, 63, false));
        //white to move
        builder.setMoveMaker(Alliance.WHITE);

        return builder.build();
    }

    private Collection<Move> calculateLegalMoves(final Piece[] boardConfig,
                                                 final int[] pieces) {
        final Collection<Move> legalsMoves = new ArrayList<>();
        for (final int piece_index : pieces) {
            legalsMoves.addAll(boardConfig[piece_index].calculateLegalMoves(this));
        }
        return legalsMoves;
    }

    private static int[] calculateActiveIndexes(final Piece[] boardConfig,
                                                final Alliance alliance) {
        int count = 0;
        // First, count how many pieces of the given alliance there are
        for (final Piece piece : boardConfig) {
            if (piece != null && piece.getPieceAllegiance() == alliance) {
                count++;
            }
        }
        // Then populate the array with their indexes
        final int[] activeIndexes = new int[count];
        int i = 0;
        for (int idx = 0; idx < boardConfig.length; idx++) {
            final Piece piece = boardConfig[idx];
            if (piece != null && piece.getPieceAllegiance() == alliance) {
                activeIndexes[i++] = idx;
            }
        }
        return activeIndexes;
    }

    public static class Builder {

        Piece[] boardConfig;
        Alliance nextMoveMaker;
        Pawn enPassantPawn;
        Move transitionMove;

        public Builder() {
            this.boardConfig = new Piece[BoardUtils.NUM_TILES];
        }

        public Builder setBoardConfiguration(final Piece[] boardConfig) {
            this.boardConfig = boardConfig;
            return this;
        }

        public Builder setPiece(final Piece piece) {
            this.boardConfig[piece.getPiecePosition()] = piece;
            return this;
        }

        public Builder setMoveMaker(final Alliance nextMoveMaker) {
            this.nextMoveMaker = nextMoveMaker;
            return this;
        }

        public Builder setEnPassantPawn(final Pawn enPassantPawn) {
            this.enPassantPawn = enPassantPawn;
            return this;
        }

        public Builder setMoveTransition(final Move transitionMove) {
            this.transitionMove = transitionMove;
            return this;
        }

        public Board build() {
            return new Board(this);
        }
    }

    /**
     * Checks if the current board position has insufficient material for a checkmate.
     * This method checks for common scenarios like K vs K, K+N vs K, K+B vs K,
     * and K+B vs K+B (bishops on same color).
     * @return true if insufficient material is detected, false otherwise.
     */
    public boolean isInsufficientMaterial() {
        final Collection<Piece> allPieces = getAllPieces();
        if (allPieces.size() <= 2) { // K vs K
            return true;
        }

        final List<Piece> whiteP = new ArrayList<>();
        final List<Piece> blackP = new ArrayList<>();

        for (final int pieceIndex : this.whitePieces) {
            whiteP.add(this.boardConfig[pieceIndex]);
        }
        for (final int pieceIndex : this.blackPieces) {
            blackP.add(this.boardConfig[pieceIndex]);
        }

        // K+N vs K or K+B vs K
        if (whiteP.size() == 1 && blackP.size() == 2) { // White has King, Black has King + Minor
            if (blackP.stream().anyMatch(p -> p.getPieceType() == Piece.PieceType.KNIGHT || p.getPieceType() == Piece.PieceType.BISHOP)) {
                return true;
            }
        }
        if (blackP.size() == 1 && whiteP.size() == 2) { // Black has King, White has King + Minor
            if (whiteP.stream().anyMatch(p -> p.getPieceType() == Piece.PieceType.KNIGHT || p.getPieceType() == Piece.PieceType.BISHOP)) {
                return true;
            }
        }

        // K+B vs K+B (bishops on same color)
        if (whiteP.size() == 2 && blackP.size() == 2) {
            Piece whiteBishop = null;
            Piece blackBishop = null;
            boolean whiteHasOnlyKingAndBishop = true;
            boolean blackHasOnlyKingAndBishop = true;

            for(Piece p : whiteP) {
                if (p.getPieceType() == Piece.PieceType.BISHOP) whiteBishop = p;
                else if (p.getPieceType() != Piece.PieceType.KING) whiteHasOnlyKingAndBishop = false;
            }
            for(Piece p : blackP) {
                if (p.getPieceType() == Piece.PieceType.BISHOP) blackBishop = p;
                else if (p.getPieceType() != Piece.PieceType.KING) blackHasOnlyKingAndBishop = false;
            }

            if (whiteHasOnlyKingAndBishop && blackHasOnlyKingAndBishop && whiteBishop != null && blackBishop != null) {
                // Check if bishops are on the same color squares
                // (piecePosition % 2) == ((piecePosition / 8) % 2) for white squares
                // (piecePosition % 2) != ((piecePosition / 8) % 2) for black squares
                // Simplified: (pos / 8 + pos % 8) % 2 == 0 for one color, 1 for other.
                boolean whiteBishopOnWhiteSquare = (whiteBishop.getPiecePosition() / 8 + whiteBishop.getPiecePosition() % 8) % 2 == 0;
                boolean blackBishopOnWhiteSquare = (blackBishop.getPiecePosition() / 8 + blackBishop.getPiecePosition() % 8) % 2 == 0;
                if (whiteBishopOnWhiteSquare == blackBishopOnWhiteSquare) {
                    return true; // Both bishops on same color squares
                }
            }
        }

        // Add more rules if needed e.g. K vs K + 2N (generally a draw, but engine might not know)
        // For now, this covers common cases. Pawns, Rooks, Queens automatically mean sufficient material.
        // If any player has a pawn, rook or queen, it's not insufficient material with these simple checks.
        for (Piece p : allPieces) {
            if (p.getPieceType() == Piece.PieceType.PAWN ||
                p.getPieceType() == Piece.PieceType.ROOK ||
                p.getPieceType() == Piece.PieceType.QUEEN) {
                return false;
            }
        }
        // If we've passed all checks for sufficient material (pawns, rooks, queens)
        // and haven't hit a specific insufficient material case, it's likely a more complex
        // draw or sufficient material not covered. For safety, assume sufficient unless explicitly K vs K, K+m vs K.
        // The previous K+B vs K+B check is specific.
        // If after all specific checks, we only have Kings, or King+minor vs King, those are draws.
        // If one side has two knights vs King, it's generally a draw but not covered above.
        // The logic above for K+N/B vs K covers one side having only one minor piece.
        // If both sides have only minor pieces and kings, it's complex.
        // The current implementation is a simplification.
        return false; // Default to sufficient material if not an obvious case.
    }
}
