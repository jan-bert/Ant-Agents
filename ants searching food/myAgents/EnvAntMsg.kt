package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.aot.gridworld.Position

data class GiveAntPosMessage(val size: Position, val nestPosition: Position)

data class AntTurnMessage(val gameTurn:Int)
data class AntTurnResponse(val pos: Position, val i: Float, val nest: Boolean)

data class PheromonesRequest(val pos: Position, val hasFood: Boolean)
data class PheromonesResponse(val pheromones: MutableList<Float>)