package live.maquq.api.user.points

import live.maquq.api.user.User

interface Points {
    fun calculate(winner: User, loser: User): Pair<Int, Int>
}