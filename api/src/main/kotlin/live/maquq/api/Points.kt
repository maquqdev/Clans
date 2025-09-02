package live.maquq.api

interface Points {
    fun calculate(winner: User, loser: User): Pair<Int, Int>
}