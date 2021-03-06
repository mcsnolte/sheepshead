package com.github.aglassman.cardengine.games.sheepshead

import com.github.aglassman.cardengine.Card
import com.github.aglassman.cardengine.GameException
import com.github.aglassman.cardengine.Player
import com.github.aglassman.cardengine.StandardPlayer
import org.slf4j.LoggerFactory
import java.io.Serializable


class Blind(
    playerOrder: List<StandardPlayer>
): Serializable {

  companion object {
    val LOGGER = LoggerFactory.getLogger(Blind::class.java)
  }

  enum class Option { owait, opass, opick, oskip }

  data class PickOption(
      val player: StandardPlayer,
      var pickOption: Option = Option.owait
  ): Serializable

  private val pickOption: List<PickOption> = playerOrder.map { PickOption(it) }

  private val blind: MutableList<Card> = mutableListOf()

  fun blindRoundComplete() = pickOption.filter { it.pickOption == Option.owait }.isEmpty()

  fun isAvailable() = (blind.size > 0) && (pickOption.filter { it.pickOption == Option.opick }.isEmpty())

  fun option(): StandardPlayer? = pickOption.firstOrNull { isAvailable() && it.pickOption == Option.owait }?.player

  fun playerHasOption(player: StandardPlayer) = isAvailable() && player == pickOption.first { it.pickOption == Option.owait }.player

  fun hasLastOption(player: StandardPlayer) = player == pickOption.last().player

  fun setBlind(blind: List<Card>) {
    if (this.blind.size == 0) {
      // println("Blind recieved ${blind.joinToString { "${it.toUnicodeString()}" }}")
      this.blind.addAll(blind)
      LOGGER.debug("Blind Set: ${blind.map { it.toUnicodeString() } }")
    } else {
      throw GameException("Blind has already been set.")
    }
  }

  fun picker() = pickOption.firstOrNull { it.pickOption == Option.opick }?.player

  fun peek() = blind.toList()

  fun peek(player: StandardPlayer): List<Card> {
    if(option() == null) {
      throw GameException("Cannot peek as blind has already been picked.")
    }

    return if(playerHasOption(player)) {
      blind.toList()
    }  else {
      throw GameException("${player.name} cannot pick as ${option()?.name} currently has the option.")
    }
  }

  fun pass(player: StandardPlayer) {
    if (option() == null) {
      throw GameException("Cannot pass as blind has already been picked.")
    }

    if (playerHasOption(player)) {
      setOption(player, Option.opass)
    } else {
      throw GameException("${player.name} cannot pick as ${option()?.name} currently has the option.")
    }
  }

  fun pick(player: StandardPlayer) {
    if (option() == null) {
      throw GameException("Cannot pick as blind has already been picked.")
    }

    if (playerHasOption(player)) {
      setOption(player, Option.opick)
      pickOption.filter { it.pickOption == Option.owait }.forEach { it.pickOption = Option.oskip }
      player.recieveCards(blind)
    } else {
      throw GameException("${player.name} cannot pick as ${option()?.name} currently has the option.")
    }
  }

  private fun setOption(player: Player, option: Option) {
    pickOption
        .first { it.player == player }
        .pickOption = option
  }

}