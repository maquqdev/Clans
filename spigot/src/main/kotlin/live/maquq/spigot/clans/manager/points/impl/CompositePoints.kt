package live.maquq.spigot.clans.manager.points.impl

import live.maquq.api.Points
import live.maquq.api.User
import live.maquq.spigot.clans.configuration.impl.ProportionalPointsConfiguration
import kotlin.math.max
import kotlin.math.roundToInt

class CompositePoints(
    private val proportionalPointsConfiguration: ProportionalPointsConfiguration
) : Points {

    override fun calculate(winner: User, loser: User): Pair<Int, Int> {
        val gainForWinner = calculateGain(loser.points)
        val lossForLoser = calculateLoss(loser.points)

        return gainForWinner to (-1*lossForLoser)
    }

    private fun calculateGain(opponentRanking: Int): Int {
        val config = proportionalPointsConfiguration
        val gain = (opponentRanking * config.gainPercent).roundToInt()

        return gain.coerceIn(config.minimumChange, config.maximumGain)
    }

    private fun calculateLoss(playerRanking: Int): Int {
        val config = proportionalPointsConfiguration
        val loss = (playerRanking * config.lossPercent).roundToInt()

        return -max(config.minimumChange, loss)
    }
}