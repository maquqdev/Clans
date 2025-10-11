package live.maquq.spigot.clans.manager.points.impl

import live.maquq.api.Points
import live.maquq.api.User
import live.maquq.spigot.clans.configuration.impl.SkillBasedPointsConfiguration
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

class SkillBasedPoints(
    private val skillBasedPointsConfiguration: SkillBasedPointsConfiguration,
) : Points {

    override fun calculate(winner: User, loser: User): Pair<Int, Int> {
        val rankingDifference = loser.points - winner.points
        val upsetFactor = this.calculateUpsetFactor(rankingDifference)
        val pointsExchange = (this.skillBasedPointsConfiguration.basePointExchange * upsetFactor).roundToInt()

        val finalChange = max(1, pointsExchange)

        return finalChange to finalChange
    }

    private fun calculateUpsetFactor(rankingDifference: Int): Double {
        val config = this.skillBasedPointsConfiguration
        return if (rankingDifference > 0)
            1.0 + (rankingDifference / config.underdogDivisor)
        else
            0.9.pow(abs(rankingDifference.toDouble()) / config.favouriteDivisor)
    }
}