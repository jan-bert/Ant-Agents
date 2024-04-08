package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.aot.gridworld.Position
import java.util.*

data class SetupCollectorMsg(val size: Position, val obstacles: List<Position>?,val repairIDs: List<String>,val collectorIDs: List<String>)
data class SetupRepairerMsg(val size: Position, val obstacles: List<Position>?,val repairPoints: List<Position>,val collectorIDs: List<String>)

data class gotM(val hasM: Boolean)
data class foundM(val foundM: List<Position>)
data class CallForProposal(val collectorID: String, val position: Position)
data class Proposal(val repairID: String, val dist: Int)
data class accept(val position: Position, val fromID: String)
data class reject(val position: Position)
data class over(val over: Boolean)
