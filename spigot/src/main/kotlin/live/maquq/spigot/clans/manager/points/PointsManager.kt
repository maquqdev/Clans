package live.maquq.spigot.clans.manager.points

import live.maquq.api.Points
import live.maquq.api.User

class PointsManager(private val points: Points) {

    fun removePointsFromPlayer(winner: User, loser: User): Pair<Int, Int> {
        val calculatedPointsPair = this.points.calculate(winner, loser)

        winner.points += calculatedPointsPair.first
        loser.points -= calculatedPointsPair.second
        return calculatedPointsPair
    }
}