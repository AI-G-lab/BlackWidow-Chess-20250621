package com.chess.engine.classic.player.ai;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays; // For printing arrays if needed

/**
 * Orchestrates the AI tuning process for chess evaluation parameters using the SPSA algorithm.
 * This class sets up the SPSA optimizer, defines the objective function (which involves
 * running self-play games between AI instances with different evaluation parameters),
 * and manages the overall tuning loop.
 *
 * <p>The tuner iteratively adjusts {@link EvaluationParameters} based on the outcomes of
 * games played by AI using {@link ImprovedStandardBoardEvaluator} managed by
 * {@link SelfPlayGameRunner}. Results and parameters are logged to a file.
 *
 * <p>A main method is provided as an example to configure and start the tuning process.
 */
public class AITuner {

    private final SPSAOptimizer spsa;
    private final int gamesPerEvaluationSet; // Number of game pairs (White/Black) for each parameter set evaluation in SPSA.
    private final int totalSpsaIterations;
    private final int gameSearchDepth;
    private final int maxMovesPerGame;
    private final String gameInitialFen; // Optional starting FEN for all self-play games.

    private EvaluationParameters bestParametersFound; // Tracks the best parameters found during tuning.
    // private double bestScoreSoFar = -Double.MAX_VALUE; // TODO: Implement robust best score tracking.

    private static final String LOG_FILE_NAME_PREFIX = "spsa_tuning_log_";
    private final PrintWriter logWriter;

    /**
     * Constructs an AITuner instance.
     *
     * @param totalSpsaIterations The total number of SPSA iterations to perform.
     * @param gamesPerEvaluationSet The number of game pairs (one as white, one as black) to play for evaluating
     *                             each of the two perturbed parameter sets (theta+ and theta-) in one SPSA iteration.
     *                             So, `gamesPerEvaluationSet * 2 * 2` games are played per SPSA iteration.
     * @param gameSearchDepth The search depth for AI players during self-play games.
     * @param maxMovesPerGame The maximum number of moves per game before it's declared a draw.
     * @param gameInitialFen Optional FEN string for a custom starting position for all games. Null for standard start.
     * @param initialParams The initial {@link EvaluationParameters} to start tuning from.
     * @param minBounds {@link EvaluationParameters} specifying minimum allowed values for each parameter.
     * @param maxBounds {@link EvaluationParameters} specifying maximum allowed values for each parameter.
     * @throws IOException If there's an error creating the log file.
     */
    public AITuner(int totalSpsaIterations,
                   int gamesPerEvaluationSet,
                   int gameSearchDepth,
                   int maxMovesPerGame,
                   String gameInitialFen,
                   EvaluationParameters initialParams,
                   EvaluationParameters minBounds,
                   EvaluationParameters maxBounds) throws IOException {
        if (initialParams == null || minBounds == null || maxBounds == null) {
            throw new IllegalArgumentException("Initial, min, or max parameters cannot be null.");
        }
        this.totalSpsaIterations = totalSpsaIterations;
        this.gamesPerEvaluationSet = gamesPerEvaluationSet;
        this.gameSearchDepth = gameSearchDepth;
        this.maxMovesPerGame = maxMovesPerGame;
        this.gameInitialFen = gameInitialFen;

        this.spsa = new SPSAOptimizer(initialParams, minBounds, maxBounds); // Uses default SPSA hyperparams
        this.bestParametersFound = new EvaluationParameters(initialParams);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        this.logWriter = new PrintWriter(new FileWriter(LOG_FILE_NAME_PREFIX + timestamp + ".txt"), true);
        logWriter.println("SPSA AI Tuner Log - " + timestamp);
        logWriter.println("Total SPSA Iterations: " + totalSpsaIterations);
        logWriter.println("Games per Evaluation Set (pairs): " + gamesPerEvaluationSet);
        logWriter.println("Game Search Depth: " + gameSearchDepth);
        logWriter.println("Max Moves per Game: " + maxMovesPerGame);
        logWriter.println("Initial FEN: " + (gameInitialFen == null ? "Standard Start" : gameInitialFen));
        logParameters("Initial Parameters:", initialParams);
        logParameters("Min Parameter Bounds:", minBounds);
        logParameters("Max Parameter Bounds:", maxBounds);
    }

    /**
     * Defines the objective function for SPSA.
     * This function evaluates a given set of {@link EvaluationParameters} by playing a series of games
     * against a baseline AI (using default parameters) and returns a score.
     * The score is typically the net win rate ( (wins - losses) / total_games ).
     * Higher scores are considered better.
     */
    private final SPSAOptimizer.SPSAObjectiveFunction objectiveFunction = paramsToEvaluate -> {
        EvaluationParameters baselineParams = new EvaluationParameters(); // Baseline uses default parameters
        int netGameScore = 0; // Aggregates outcomes: win = +1, loss = -1, draw = 0

        // System.out.println("Evaluating parameters: ..."); // SPSAOptimizer may log its steps.
        // AITuner's logParameters method logs parameters at the start/end of SPSA iterations.

        for (int i = 0; i < gamesPerEvaluationSet; i++) {
            // Game 1: paramsToEvaluate is White, baseline is Black
            SelfPlayGameRunner runner1 = new SelfPlayGameRunner(
                    paramsToEvaluate, baselineParams, gameSearchDepth, gameInitialFen, maxMovesPerGame);
            int outcome1 = runner1.playGame(); // Returns 1 for white win, -1 for black win, 0 for draw
            netGameScore += outcome1;

            // Game 2: paramsToEvaluate is Black, baseline is White
            SelfPlayGameRunner runner2 = new SelfPlayGameRunner(
                    baselineParams, paramsToEvaluate, gameSearchDepth, gameInitialFen, maxMovesPerGame);
            int outcome2 = runner2.playGame(); // Returns 1 for white win, -1 for black win, 0 for draw
            netGameScore -= outcome2; // If paramsToEvaluate (Black) wins, outcome2 is -1, so netGameScore increases by 1.
                                      // If paramsToEvaluate (Black) loses, outcome2 is 1, so netGameScore decreases by 1.
        }

        // Normalize the score to be per game played by the entity with paramsToEvaluate
        double normalizedScore = (double) netGameScore / (gamesPerEvaluationSet * 2);
        System.out.printf("Objective function score for current params: %.3f (Net game score: %d over %d games)\n",
                          normalizedScore, netGameScore, gamesPerEvaluationSet * 2);
        return normalizedScore;
    };

    /**
     * Starts the SPSA tuning process for the specified number of iterations.
     * Logs progress and final parameters.
     */
    public void tune() {
        logWriter.println("\n--- Starting SPSA Tuning ---");
        for (int i = 0; i < totalSpsaIterations; i++) {
            System.out.printf("\n--- SPSA Iteration %d/%d ---\n", (i + 1), totalSpsaIterations);
            logWriter.printf("\n--- SPSA Iteration %d/%d ---\n", (i + 1), totalSpsaIterations);

            EvaluationParameters currentIterParams = spsa.iterate(objectiveFunction);

            // TODO: Implement a more robust way to track the 'bestParametersFound'.
            // This might involve an additional evaluation of `currentIterParams` against
            // the current `bestParametersFound` or a fixed benchmark.
            // For now, `bestParametersFound` is simply updated to the latest parameters from SPSA.
            logParameters("Parameters after SPSA iteration " + (i + 1) + ":", currentIterParams);
            this.bestParametersFound = new EvaluationParameters(currentIterParams); // Update with the latest
        }
        logWriter.println("\n--- SPSA Tuning Complete ---");
        logParameters("Final (Best Tracked) Parameters:", this.bestParametersFound);
        System.out.println("\n--- SPSA Tuning Complete ---");
        System.out.println("Final parameters logged to the log file starting with: " + LOG_FILE_NAME_PREFIX);
        logWriter.close();
    }

    /**
     * Logs the provided {@link EvaluationParameters} to the log file.
     * @param description A description for this set of parameters (e.g., "Initial Parameters").
     * @param params The {@link EvaluationParameters} to log.
     */
    private void logParameters(String description, EvaluationParameters params) {
        if (params == null) {
            logWriter.println(description + " (null)");
            return;
        }
        logWriter.println(description);
        logWriter.println("  CheckMateBonus: " + params.checkMateBonus);
        logWriter.println("  CheckBonus: " + params.checkBonus);
        logWriter.println("  CastleBonus: " + params.castleBonus);
        logWriter.println("  MobilityMultiplier: " + params.mobilityMultiplier);
        logWriter.println("  AttackMultiplier: " + params.attackMultiplier);
        logWriter.println("  TwoBishopsBonus: " + params.twoBishopsBonus);
        logWriter.println("  CrampingMultiplier: " + params.crampingMultiplier);
        logWriter.println("  PieceActivityBonus: " + params.pieceActivityBonus);
        logWriter.println("  CentralControlBonus: " + params.centralControlBonus);
        logWriter.println("  DevelopmentBonus: " + params.developmentBonus);
        logWriter.println("  CheckmateDepthBonusMultiplier: " + params.checkmateDepthBonusMultiplier);
        logWriter.flush(); // Ensure data is written to file immediately
    }

    /**
     * Main method to demonstrate and run the AI tuner.
     * Configures tuner parameters, initializes default evaluation parameters and bounds,
     * and starts the tuning process.
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            EvaluationParameters initial = new EvaluationParameters(); // Start with default evaluation parameters

            // Define sensible min/max bounds for each evaluation parameter
            EvaluationParameters minBounds = new EvaluationParameters();
            minBounds.checkMateBonus = 1000;    minBounds.checkBonus = 0;       minBounds.castleBonus = 0;
            minBounds.mobilityMultiplier = -5;  minBounds.attackMultiplier = -5;minBounds.twoBishopsBonus = 0;
            minBounds.crampingMultiplier = -5;  minBounds.pieceActivityBonus = -20; minBounds.centralControlBonus = -10;
            minBounds.developmentBonus = -20;   minBounds.checkmateDepthBonusMultiplier = 10;

            EvaluationParameters maxBounds = new EvaluationParameters();
            maxBounds.checkMateBonus = 20000;   maxBounds.checkBonus = 200;     maxBounds.castleBonus = 100;
            maxBounds.mobilityMultiplier = 10;  maxBounds.attackMultiplier = 10;maxBounds.twoBishopsBonus = 100;
            maxBounds.crampingMultiplier = 10;  maxBounds.pieceActivityBonus = 20;  maxBounds.centralControlBonus = 50;
            maxBounds.developmentBonus = 50;    maxBounds.checkmateDepthBonusMultiplier = 300;

            // Tuner settings - these may need significant adjustment for good results
            int totalSpsaIterations = 50;     // Number of SPSA iterations (e.g., 100-1000 for serious tuning)
            int gamesPerEvalSet = 5;          // Games per (theta+, theta-) evaluation set (e.g., 10-20 for smoother gradients)
                                              // Total games per SPSA iteration = gamesPerEvalSet * 2 (for White/Black) * 2 (for y_plus/y_minus)
            int searchDepthForTuning = 3;     // Keep search depth low for faster tuning games. Increase for final AI.
            int maxMovesPerGameInTuning = 150;// Max moves per game to prevent excessively long or stuck games.

            AITuner tuner = new AITuner(totalSpsaIterations, gamesPerEvalSet, searchDepthForTuning,
                                        maxMovesPerGameInTuning, null, initial, minBounds, maxBounds);
            tuner.tune();

        } catch (IOException e) {
            System.err.println("Error initializing or running AITuner: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
