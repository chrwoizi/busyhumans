package com.c5000.mastery.backend.services

object Balancing {

    private val MAX_LEVEL = 100
    private val LEVELING_SCALE = 200
    private val LEVELING_POWER = 0.7f
    private val REWARD_FACTOR = 50
    private val REWARD_POWER = 0.7f

    /**
     * Amount of the reward xp that is given to the owner for each result activity of participating persons
     */
    val OWNER_REWARD_FACTOR = 0.1f

    def getLevel(xp: Int): Int = {
        return scala.math.min(MAX_LEVEL, 1 + scala.math.pow(xp / LEVELING_SCALE.toFloat, LEVELING_POWER)).toInt
    }

    def getLevelProgress(xp: Int): Float = {
        val current = getLevel(xp)
        val next = current + 1
        val currentXp = getMinXpForLevel(current)
        val nextXp = getMinXpForLevel(next)
        return (xp - currentXp) / (nextXp - currentXp).toFloat
    }

    def getMinXpForLevel(level: Int): Int = {
        return (scala.math.pow(scala.math.min(level, MAX_LEVEL) - 1, 1f / LEVELING_POWER) * LEVELING_SCALE).toInt
    }

    def getInitialAssignmentReward(level: Int) =
        (REWARD_FACTOR * scala.math.pow(level, REWARD_POWER)).toInt

}
