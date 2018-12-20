package com.github.aglassman.cardengine.games.sheepshead

import com.github.aglassman.cardengine.GameException
import com.github.aglassman.cardengine.GameStateException
import com.github.aglassman.cardengine.Player

typealias TeamPoints = List<Pair<Team, Int>>
typealias PlayerScores = List<Pair<Player, Int>>
typealias PlayerScore = Pair<Player, Int>

class Points(
    scoring: Scoring,
    trickTracker: TrickTracker,
    burriedCards: BurriedCards,
    teams: Teams?
) {

  private val scoring = scoring
  private val trickTracker = trickTracker
  private val burriedCards = burriedCards
  private val teams = teams ?: throw GameStateException("Cannot calculate points when teams are unknown.")

  /**
   * Returns the teams, and their tallied trick points.
   * Returned ranked in descending order by points.
   */
  fun determinePoints(): TeamPoints {

    val trickPoints = trickTracker
        .calculateCurrentPoints(teams)
        .toMutableMap()

    when(scoring) {
      Scoring.leaster -> {
        // need to figure out what to do with blind cards here
      }
      else -> {
        val pickerTeam: Team = teams
            .teamList()
            .first { it.second.contains(teams.picker) }

        trickPoints.computeIfPresent(pickerTeam, { team, currentPoints -> currentPoints +  burriedCards.points()} )

      }
    }

    return trickPoints.toList()
        .sortedByDescending { it.second }
  }

  fun determineWinner(): Team =
      when(scoring){
        Scoring.leaster -> {
          trickTracker.tricks()
              .filter { it.trickTaken() }
              .minBy { it.trickPoints() }
              ?.let { Team(it.trickWinner()!!.name, listOf(it.trickWinner()!!)) }
              ?: throw GameException("Could not determine winner for leaster scoring.")
        }
        else -> {
          determinePoints().maxBy { it.second }?.first ?: throw GameException("Could not determine winner for normal scoring.")
        }
      }

  fun determineScore(): PlayerScores  {

    val teamPoints = determinePoints()

    return when (scoring) {
      Scoring.normal -> {
        scoreNormal(teamPoints)
      }
      Scoring.doubler -> {
        scoreNormal(teamPoints)
            .map { it.first to it.second * 2 }
      }
      Scoring.leaster -> {
        scoreLeaster(teamPoints)
      }
    }
  }

  private fun scoreNormal(teamPoints: TeamPoints): PlayerScores = when(teams.teamList().size) {
    2 -> {
      // two teams, assumes there was a picker

      val winners = teamPoints.get(0).first.second
      val losers = teamPoints.get(1).first.second

      val pickerWon = winners.contains(teams.picker)

      val scoreList = mutableListOf<PlayerScore>()

      when(winners.size) {
        1 -> {
          scoreList.add(teams.picker!! to 4)
          losers.forEach { player -> scoreList.add(player to  -1) }
        }
        2 -> {
          scoreList.add(teams.picker!! to 2)
          scoreList.add(teams.partner()!! to 1)
          losers.forEach { player -> scoreList.add(player to  -1) }
        }
        3 -> {
          scoreList.add(teams.picker!! to -2)
          scoreList.add(teams.partner()!! to -1)
          winners.forEach { player -> scoreList.add(player to  1) }
        }
        4 -> {
          scoreList.add(teams.picker!! to -4)
          winners.forEach { player -> scoreList.add(player to  1) }
        }
        else -> {}
      }

      scoreList.toList()
    }
    else -> {
      emptyList()
    }
  }

  private fun scoreLeaster(teamPoints: TeamPoints): PlayerScores = emptyList()
}