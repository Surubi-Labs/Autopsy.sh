package dev.autopsy.metrics

object HealthScoreComputer {

    private const val WEIGHT_CHURN = 0.25
    private const val WEIGHT_BUS_FACTOR = 0.25
    private const val WEIGHT_DEPS = 0.20
    private const val WEIGHT_TEST_COVERAGE = 0.20
    private const val WEIGHT_HOTSPOTS = 0.10

    private const val NEUTRAL_SCORE = 50.0

    /**
     * Compute overall health score from individual metric components.
     *
     * Each component is normalized to 0-100. Overall = weighted average.
     * Missing components use a neutral score of 50.
     */
    fun computeHealthScore(
        churn: Map<String, List<WeeklyChurn>>?,
        busFactor: Map<String, BusFactorResult>?,
        staleness: DependencyStalenessResult?,
        testCoverage: Map<String, List<WeeklyTestRatio>>?,
        hotspots: List<HotspotResult>?,
    ): HealthScore {
        val churnScore = churn?.let { scoreChurn(it) } ?: NEUTRAL_SCORE
        val busFactorScore = busFactor?.let { scoreBusFactor(it) } ?: NEUTRAL_SCORE
        val depsScore = staleness?.overallScore ?: NEUTRAL_SCORE
        val testScore = testCoverage?.let { scoreTestCoverage(it) } ?: NEUTRAL_SCORE
        val hotspotScore = hotspots?.let { scoreHotspots(it) } ?: NEUTRAL_SCORE

        val overall = churnScore * WEIGHT_CHURN +
            busFactorScore * WEIGHT_BUS_FACTOR +
            depsScore * WEIGHT_DEPS +
            testScore * WEIGHT_TEST_COVERAGE +
            hotspotScore * WEIGHT_HOTSPOTS

        return HealthScore(
            overall = overall.coerceIn(0.0, 100.0),
            breakdown = mapOf(
                "churn" to churnScore,
                "busFactor" to busFactorScore,
                "dependencies" to depsScore,
                "testCoverage" to testScore,
                "hotspots" to hotspotScore,
            ),
        )
    }

    private fun scoreChurn(churn: Map<String, List<WeeklyChurn>>): Double {
        // Low churn is healthy. Score based on average churn across modules.
        val allChurns = churn.values.flatten()
        if (allChurns.isEmpty()) return 100.0

        val avgChurn = allChurns.map { it.totalChurn }.average()
        // Penalty starts at churn > 100 lines/week average
        return maxOf(0.0, 100.0 - (avgChurn / 10.0))
    }

    private fun scoreBusFactor(busFactor: Map<String, BusFactorResult>): Double {
        if (busFactor.isEmpty()) return NEUTRAL_SCORE
        val avgFactor = busFactor.values.map { it.busFactor }.average()
        // Score: 0 at factor 0, 50 at factor 1, 100 at factor 3+
        return minOf(100.0, avgFactor * 33.3)
    }

    private fun scoreTestCoverage(testCoverage: Map<String, List<WeeklyTestRatio>>): Double {
        val allRatios = testCoverage.values.flatten()
        if (allRatios.isEmpty()) return NEUTRAL_SCORE

        val avgRatio = allRatios.map { it.ratio }.average()
        // Score: 100 if ratio >= 0.3, scale linearly below
        return minOf(100.0, avgRatio / 0.3 * 100.0)
    }

    private fun scoreHotspots(hotspots: List<HotspotResult>): Double {
        if (hotspots.isEmpty()) return 100.0
        val maxScore = hotspots.maxOf { it.score }
        // Penalty based on worst hotspot. Score > 100 is extreme.
        return maxOf(0.0, 100.0 - maxScore)
    }
}
