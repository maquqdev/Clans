package live.maquq.spigot.clans.manager.points

import live.maquq.api.user.points.Points
import live.maquq.api.user.User

class PointsManager(private val points: Points) {

    fun removePointsFromPlayer(winner: User, loser: User): Pair<Int, Int> {
        val calculatedPointsPair = this.points.calculate(winner, loser)

        winner.points += calculatedPointsPair.first
        loser.points -= calculatedPointsPair.second
        return calculatedPointsPair
    }
}