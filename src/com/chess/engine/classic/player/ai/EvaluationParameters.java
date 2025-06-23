package com.chess.engine.classic.player.ai;

/**
 * Holds the tunable parameters for the chess board evaluation function.
 * These parameters are used by {@link ImprovedStandardBoardEvaluator} and are tuned by {@link SPSAOptimizer}.
 * All fields are public for direct manipulation by the SPSA algorithm, which simplifies
 * the conversion to and from a vector representation.
 */
public class EvaluationParameters {

    /** Bonus for delivering checkmate. Typically a large positive value. */
    public int checkMateBonus;
    /** Bonus for putting the opponent's king in check. */
    public int checkBonus;
    /** Bonus for having castled, improving king safety and rook development. */
    public int castleBonus;
    /** Multiplier for the mobility score. Higher values emphasize piece activity. */
    public int mobilityMultiplier;
    /** Multiplier for the attack score. Higher values emphasize aggressive moves. */
    public int attackMultiplier;
    /** Bonus for possessing the bishop pair. */
    public int twoBishopsBonus;
    /** Multiplier for the cramping penalty, applied when a player has significantly restricted mobility. */
    public int crampingMultiplier;
    /** Bonus for piece activity (e.g., pieces moved from starting positions). Can be negative for penalties. */
    public int pieceActivityBonus;
    /** Bonus for controlling central squares of the board. */
    public int centralControlBonus;
    /** Bonus for developed pieces (e.g., knights and bishops off the back rank). Can be negative for penalties. */
    public int developmentBonus;
    /** Multiplier for scaling the checkmate bonus by depth (e.g., checkmate found at shallower depth is more valuable). */
    public int checkmateDepthBonusMultiplier;

    /**
     * Constructor that initializes parameters with default values.
     * These defaults are based on the original hardcoded values in {@link ImprovedStandardBoardEvaluator}.
     */
    public EvaluationParameters() {
        this.checkMateBonus = 10000;
        this.checkBonus = 45;
        this.castleBonus = 25;
        this.mobilityMultiplier = 2;
        this.attackMultiplier = 2;
        this.twoBishopsBonus = 25;
        this.crampingMultiplier = 2;
        this.pieceActivityBonus = 5;
        this.centralControlBonus = 10;
        this.developmentBonus = 15;
        this.checkmateDepthBonusMultiplier = 100;
    }

    /**
     * Copy constructor. Creates a deep copy of another EvaluationParameters instance.
     * @param other The EvaluationParameters instance to copy. Must not be null.
     */
    public EvaluationParameters(EvaluationParameters other) {
        if (other == null) {
            throw new IllegalArgumentException("Source EvaluationParameters cannot be null for copy constructor.");
        }
        this.checkMateBonus = other.checkMateBonus;
        this.checkBonus = other.checkBonus;
        this.castleBonus = other.castleBonus;
        this.mobilityMultiplier = other.mobilityMultiplier;
        this.attackMultiplier = other.attackMultiplier;
        this.twoBishopsBonus = other.twoBishopsBonus;
        this.crampingMultiplier = other.crampingMultiplier;
        this.pieceActivityBonus = other.pieceActivityBonus;
        this.centralControlBonus = other.centralControlBonus;
        this.developmentBonus = other.developmentBonus;
        this.checkmateDepthBonusMultiplier = other.checkmateDepthBonusMultiplier;
    }

    // SPSA directly modifies public fields after creating a default object or using the copy constructor.
    // This simplifies the toDoubleArray/fromDoubleArray conversions within SPSAOptimizer.
    // Adding setters or a constructor that takes all parameters could be an alternative
    // if direct field access becomes problematic.
}
