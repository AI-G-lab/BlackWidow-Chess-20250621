package com.chess.engine.classic.player.ai;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Implements the Simultaneous Perturbation Stochastic Approximation (SPSA) algorithm.
 * SPSA is used for optimizing a set of parameters (theta) when the objective function
 * is noisy or expensive to evaluate, making it suitable for tuning chess evaluation parameters
 * based on game outcomes.
 *
 * <p>The algorithm works by perturbing all parameters simultaneously using a random
 * direction vector (delta_k), evaluating the objective function at these two perturbed points,
 * estimating the gradient, and then updating the parameters.
 *
 * <p>Key SPSA update equations:
 * <ul>
 *   <li>Gain sequences:
 *     <ul>
 *       <li>`a_k = a / (k + 1 + A)^alpha`</li>
 *       <li>`c_k = c / (k + 1)^gamma`</li>
 *     </ul>
 *   </li>
 *   <li>Gradient estimate: `g_hat_k(theta_k) = (y(theta_k + c_k*delta_k) - y(theta_k - c_k*delta_k)) / (2*c_k*delta_ki)` for each component i.</li>
 *   <li>Parameter update: `theta_k+1 = theta_k - a_k * g_hat_k(theta_k)`</li>
 * </ul>
 *
 * @see EvaluationParameters
 * @see AITuner
 */
public class SPSAOptimizer {

    private final double a_spsa; // SPSA 'a' coefficient for a_k sequence.
    private final double c_spsa; // SPSA 'c' coefficient for c_k sequence.
    private final double A_spsa; // SPSA 'A' stability constant for a_k sequence.
    private final double alpha_spsa; // SPSA 'alpha' exponent for a_k sequence.
    private final double gamma_spsa; // SPSA 'gamma' exponent for c_k sequence.

    private EvaluationParameters currentParameters; // Current set of parameters (theta_k) being optimized.
    private final int numParameters; // Number of parameters in EvaluationParameters.
    private final Random random = new Random(); // For generating perturbation vector delta_k.
    private int iteration; // Current iteration number k.

    // Optional bounds to keep parameters within a reasonable range.
    private final EvaluationParameters minBounds;
    private final EvaluationParameters maxBounds;

    /**
     * Constructs an SPSAOptimizer with specified SPSA hyperparameters.
     *
     * @param initialParameters The starting set of {@link EvaluationParameters} (theta_0).
     * @param a SPSA 'a' hyperparameter, influencing the step size of parameter updates.
     * @param c SPSA 'c' hyperparameter, influencing the magnitude of perturbations for gradient estimation.
     * @param A SPSA 'A' stability constant, preventing a_k from becoming too large in early iterations.
     *          Typically set to ~10% of the expected total number of iterations.
     * @param alpha SPSA 'alpha' exponent for a_k decay (recommended value around 0.602).
     * @param gamma SPSA 'gamma' exponent for c_k decay (recommended value around 0.101).
     * @param minBounds Optional {@link EvaluationParameters} instance specifying minimum allowed values for each parameter. Can be null.
     * @param maxBounds Optional {@link EvaluationParameters} instance specifying maximum allowed values for each parameter. Can be null.
     */
    public SPSAOptimizer(EvaluationParameters initialParameters,
                         double a, double c, double A, double alpha, double gamma,
                         EvaluationParameters minBounds, EvaluationParameters maxBounds) {
        if (initialParameters == null) {
            throw new IllegalArgumentException("Initial parameters cannot be null.");
        }
        this.currentParameters = new EvaluationParameters(initialParameters); // Work on a copy
        this.numParameters = countParameters();

        this.a_spsa = a;
        this.c_spsa = c;
        this.A_spsa = A;
        this.alpha_spsa = alpha;
        this.gamma_spsa = gamma;
        this.iteration = 0; // SPSA iterations are 0-indexed (k=0, 1, 2...) in some literature, or 1-indexed. Using 1-indexed for k in formulas.

        this.minBounds = (minBounds != null) ? new EvaluationParameters(minBounds) : null;
        this.maxBounds = (maxBounds != null) ? new EvaluationParameters(maxBounds) : null;
    }

    /**
     * Constructs an SPSAOptimizer with default SPSA hyperparameters.
     * Default values: a=0.1, c=0.01, A=100.0, alpha=0.602, gamma=0.101.
     * These defaults are common starting points but might require tuning for specific problems.
     *
     * @param initialParameters The starting set of {@link EvaluationParameters}.
     * @param minBounds Optional {@link EvaluationParameters} for minimum parameter values.
     * @param maxBounds Optional {@link EvaluationParameters} for maximum parameter values.
     */
    public SPSAOptimizer(EvaluationParameters initialParameters, EvaluationParameters minBounds, EvaluationParameters maxBounds) {
        this(initialParameters, 0.1, 0.01, 100.0, 0.602, 0.101, minBounds, maxBounds);
    }


    /**
     * Performs one iteration of the SPSA optimization algorithm.
     * This involves:
     * <ol>
     *   <li>Calculating current gain coefficients a_k and c_k.</li>
     *   <li>Generating a random perturbation vector delta_k.</li>
     *   <li>Evaluating the objective function at two perturbed points: theta_k + c_k*delta_k and theta_k - c_k*delta_k.</li>
     *   <li>Estimating the gradient of the objective function.</li>
     *   <li>Updating the parameter vector theta_k to theta_k+1.</li>
     *   <li>Applying bounds to the new parameters if specified.</li>
     * </ol>
     *
     * @param objectiveFunction A {@link SPSAObjectiveFunction} that takes {@link EvaluationParameters}
     *                          and returns a scalar score. SPSA aims to minimize this score by default;
     *                          if maximizing, the objective function should return -score or the update rule adjusted.
     *                          The provided implementation assumes higher scores from objectiveFunction are better,
     *                          and the SPSA update rule `theta_k+1 = theta_k - a_k * g_hat_k` implicitly handles this
     *                          if `g_hat_k` is `(y_plus - y_minus) / ...`.
     * @return A copy of the updated {@link EvaluationParameters} after this iteration.
     */
    public EvaluationParameters iterate(SPSAObjectiveFunction objectiveFunction) {
        iteration++;
        double ak = a_spsa / Math.pow(iteration + A_spsa, alpha_spsa);
        double ck = c_spsa / Math.pow(iteration, gamma_spsa);

        // 1. Generate random perturbation vector delta_k (Bernoulli +/-1 distribution)
        double[] delta = new double[numParameters];
        for (int i = 0; i < numParameters; i++) {
            delta[i] = (random.nextBoolean()) ? 1.0 : -1.0;
        }

        // 2. Evaluate objective function at two points
        EvaluationParameters thetaPlus = applyPerturbation(currentParameters, delta, ck);
        EvaluationParameters thetaMinus = applyPerturbation(currentParameters, delta, -ck);

        // Apply bounds if they exist
        applyParameterBounds(thetaPlus);
        applyParameterBounds(thetaMinus);

        // The objective function needs to run self-play games.
        // Example: run N games with thetaPlus vs baseline, N games with thetaMinus vs baseline.
        // Or thetaPlus vs thetaMinus.
        // For simplicity, let's assume objectiveFunction returns a single score (higher is better).
        double yPlus = objectiveFunction.evaluate(thetaPlus);
        double yMinus = objectiveFunction.evaluate(thetaMinus);

        // 3. Estimate gradient g_hat_k
        double[] g_hat = new double[numParameters];
        for (int i = 0; i < numParameters; i++) {
            if (Math.abs(delta[i]) < 1e-9 || Math.abs(2 * ck * delta[i]) < 1e-9) { // Avoid division by zero if ck or delta[i] is zero
                 g_hat[i] = 0; // Or some other handling
            } else {
                g_hat[i] = (yPlus - yMinus) / (2 * ck * delta[i]);
            }
        }

        // 4. Update parameter vector theta_k+1
        double[] currentThetaVec = toDoubleArray(currentParameters);
        double[] nextThetaVec = new double[numParameters];
        for (int i = 0; i < numParameters; i++) {
            nextThetaVec[i] = currentThetaVec[i] - ak * g_hat[i];
        }

        this.currentParameters = fromDoubleArray(nextThetaVec, currentParameters);
        applyParameterBounds(this.currentParameters); // Apply bounds to the new parameters

        System.out.printf("SPSA Iteration %d: ak=%.6f, ck=%.6f, yPlus=%.3f, yMinus=%.3f\n", iteration, ak, ck, yPlus, yMinus);
        // Optionally log g_hat and currentParameters here

        return new EvaluationParameters(this.currentParameters); // Return a copy
    }

    private void applyParameterBounds(EvaluationParameters params) {
        if (minBounds == null || maxBounds == null) {
            return;
        }
        // This is a bit manual due to direct field access. Reflection could make it generic.
        params.checkMateBonus = Math.max(minBounds.checkMateBonus, Math.min(params.checkMateBonus, maxBounds.checkMateBonus));
        params.checkBonus = Math.max(minBounds.checkBonus, Math.min(params.checkBonus, maxBounds.checkBonus));
        params.castleBonus = Math.max(minBounds.castleBonus, Math.min(params.castleBonus, maxBounds.castleBonus));
        params.mobilityMultiplier = Math.max(minBounds.mobilityMultiplier, Math.min(params.mobilityMultiplier, maxBounds.mobilityMultiplier));
        params.attackMultiplier = Math.max(minBounds.attackMultiplier, Math.min(params.attackMultiplier, maxBounds.attackMultiplier));
        params.twoBishopsBonus = Math.max(minBounds.twoBishopsBonus, Math.min(params.twoBishopsBonus, maxBounds.twoBishopsBonus));
        params.crampingMultiplier = Math.max(minBounds.crampingMultiplier, Math.min(params.crampingMultiplier, maxBounds.crampingMultiplier));
        params.pieceActivityBonus = Math.max(minBounds.pieceActivityBonus, Math.min(params.pieceActivityBonus, maxBounds.pieceActivityBonus));
        params.centralControlBonus = Math.max(minBounds.centralControlBonus, Math.min(params.centralControlBonus, maxBounds.centralControlBonus));
        params.developmentBonus = Math.max(minBounds.developmentBonus, Math.min(params.developmentBonus, maxBounds.developmentBonus));
        params.checkmateDepthBonusMultiplier = Math.max(minBounds.checkmateDepthBonusMultiplier, Math.min(params.checkmateDepthBonusMultiplier, maxBounds.checkmateDepthBonusMultiplier));
    }


    private EvaluationParameters applyPerturbation(EvaluationParameters baseParams, double[] delta, double scale) {
        EvaluationParameters perturbedParams = new EvaluationParameters(baseParams);
        double[] baseVec = toDoubleArray(baseParams);
        double[] perturbedVec = new double[numParameters];

        for (int i = 0; i < numParameters; i++) {
            perturbedVec[i] = baseVec[i] + scale * delta[i];
        }
        return fromDoubleArray(perturbedVec, perturbedParams); // Modifies perturbedParams in place
    }

    // Helper methods to convert EvaluationParameters to/from double array for SPSA math
    // This is order-dependent and assumes integer parameters can be treated as doubles for SPSA.
    // A more robust solution might involve reflection or a more structured parameter definition.
    private double[] toDoubleArray(EvaluationParameters params) {
        return new double[]{
                params.checkMateBonus, params.checkBonus, params.castleBonus,
                params.mobilityMultiplier, params.attackMultiplier, params.twoBishopsBonus,
                params.crampingMultiplier, params.pieceActivityBonus, params.centralControlBonus,
                params.developmentBonus, params.checkmateDepthBonusMultiplier
        };
    }

    private EvaluationParameters fromDoubleArray(double[] array, EvaluationParameters targetParams) {
        // Modifies targetParams in place, or creates a new one if null
        EvaluationParameters result = targetParams != null ? targetParams : new EvaluationParameters();
        result.checkMateBonus = (int) Math.round(array[0]);
        result.checkBonus = (int) Math.round(array[1]);
        result.castleBonus = (int) Math.round(array[2]);
        result.mobilityMultiplier = (int) Math.round(array[3]);
        result.attackMultiplier = (int) Math.round(array[4]);
        result.twoBishopsBonus = (int) Math.round(array[5]);
        result.crampingMultiplier = (int) Math.round(array[6]);
        result.pieceActivityBonus = (int) Math.round(array[7]);
        result.centralControlBonus = (int) Math.round(array[8]);
        result.developmentBonus = (int) Math.round(array[9]);
        result.checkmateDepthBonusMultiplier = (int) Math.round(array[10]);
        return result;
    }

    /**
     * Counts the number of tunable parameters in {@link EvaluationParameters}.
     * This is currently hardcoded and must be updated if {@link EvaluationParameters} changes.
     * @return The number of tunable parameters.
     */
    private int countParameters() {
        // This is hardcoded based on the current fields in EvaluationParameters.
        // Reflection could be used for a more dynamic count if fields change often,
        // or EvaluationParameters could provide a method to return its field count or a list of fields.
        return 11;
    }

    /**
     * Gets a copy of the current best set of evaluation parameters.
     * @return A new {@link EvaluationParameters} instance with the current parameter values.
     */
    public EvaluationParameters getCurrentParameters() {
        return new EvaluationParameters(this.currentParameters); // Return a copy
    }

    /**
     * Functional interface for the SPSA objective function.
     */
    @FunctionalInterface
    public interface SPSAObjectiveFunction {
        /**
         * Evaluates the given set of parameters.
         * Typically involves running self-play games and returning a score (e.g., win rate).
         * A higher score should indicate better performance.
         * @param params The EvaluationParameters to evaluate.
         * @return The score for these parameters.
         */
        double evaluate(EvaluationParameters params);
    }
}
