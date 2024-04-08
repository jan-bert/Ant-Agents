package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.lang.Math.sqrt
import java.util.*

class RepairAgent (private val repairID: String): Agent(overrideName=repairID) {
    private var added = false;
    private var collectIDs = listOf<String>();
    private var grid = Array(0) { Array(0) { LinkedList<Position>() } }
    private var repairPoints = mutableListOf<Position>()
    private var over = false
    private var foundM = false
    private var goal = Position(0,0)
    private var lastmove = WorkerAction.NORTH
    private var MFound = mutableListOf<Position>();
    private var fromID = repairID
    private var accepted = false
    private var hasM = false;
    private val allMoves = mutableListOf(WorkerAction.NORTH, WorkerAction.NORTHEAST, WorkerAction.EAST, WorkerAction.SOUTHEAST, WorkerAction.SOUTH, WorkerAction.SOUTHWEST, WorkerAction.WEST, WorkerAction.NORTHWEST)
    private var possibleMoves = mutableListOf(WorkerAction.NORTH, WorkerAction.NORTHEAST, WorkerAction.EAST, WorkerAction.SOUTHEAST, WorkerAction.SOUTH, WorkerAction.SOUTHWEST, WorkerAction.WEST, WorkerAction.NORTHWEST)
    private var currentpos = Position(0, 0);
    var obstacles = mutableListOf<Position>()
    /* TODO
        - this WorkerAgent has the ability to drop material
        - NOTE: can walk on open repairpoints, can not collect material
        - participate in cnp instances, meet with CollectAgent, get material
        - go to repairpoint, drop material
     */
    private fun createGraph(size: Position, obstacles: List<Position>){
        grid = Array(size.x+1) { Array(size.y+1) { LinkedList<Position>() } }
        for(i in 0 ..size.x-1){
            for(j in 0 ..size.y-1){
                if(!obstacles.contains(Position(i,j))) {
                    for(k in i-1 ..i+1){
                        for(g in j-1 ..j+1){
                            if(!obstacles.contains(Position(k,g)) && k >= 0 && k < size.x && g >= 0 && g < size.y ) {
                                grid[i][j].add(Position(k,g))
                            }
                        }
                    }
                }
            }
        }
    }
    fun getworkerMove(movesList : MutableList<WorkerAction>){
        if(goal == currentpos && accepted){
            if(!hasM) {
                //take material
                system.resolve("server") tell TransferMaterial(fromID, repairID)
                return
            }else{
                //repair hole
                var y = WorkerAction.DROP
                attemptMove(y)
                repairPoints.remove(goal)
                accepted = false
                hasM = false
                goal = Position(0,0)
            }
        }
        var x = moveTo(goal) // Repairer sagen wo er hin soll
        if(x != null){
            if(movesList.contains(x)) {
                attemptMove(x)
            }else{
                if (!foundM) {
                    attemptMove(movesList.first())
                }
            }
        }
    }

    fun attemptMove(act : WorkerAction){
        //ans == ture -> move ist possible (kein obstacle)
        val actMsg = WorkerActionRequest(repairID,act)
        if(!over) {
            system.resolve("server") invoke ask<WorkerActionResponse>(actMsg) { res ->
                if (res.state) {
                    lastmove = act
                }
            }
        }
    }

    fun removeImpossibleMoves(movesList : MutableList<WorkerAction>){
        //entfernt alle moves die nicht möglich sind
        for (act in movesList) {
            if (act == allMoves[0]) {
                if (obstacles.contains(Position(currentpos.x, currentpos.y - 1))) {
                    possibleMoves.remove(act)
                }
            } else if (act == allMoves[1]) {
                if (obstacles.contains(Position(currentpos.x + 1, currentpos.y - 1))) {
                    possibleMoves.remove(act)
                }
            } else if (act == allMoves[2]) {
                if (obstacles.contains(Position(currentpos.x + 1, currentpos.y))) {
                    possibleMoves.remove(act)
                }
            } else if (act == allMoves[3]) {
                if (obstacles.contains(Position(currentpos.x + 1, currentpos.y + 1))) {
                    possibleMoves.remove(act)
                }
            } else if (act == allMoves[4]) {
                if (obstacles.contains(Position(currentpos.x, currentpos.y + 1))) {
                    possibleMoves.remove(act)
                }
            } else if (act == allMoves[5]) {
                if (obstacles.contains(Position(currentpos.x - 1, currentpos.y + 1))) {
                    possibleMoves.remove(act)
                }
            } else if (act == allMoves[6]) {
                if (obstacles.contains(Position(currentpos.x - 1, currentpos.y))) {
                    possibleMoves.remove(act)
                }
            } else if (act == allMoves[7]) {
                if (obstacles.contains(Position(currentpos.x - 1, currentpos.y - 1))) {
                    possibleMoves.remove(act)
                }
            }
        }
    }

    fun moveTo(pos: Position): WorkerAction? {
        val dx =  pos.x - currentpos.x
        val dy =  pos.y - currentpos.y
        var move: WorkerAction? = null

        // bereits am Ziel?
        if (dx == 0 && dy == 0) {
            return null
        }

        // move berechnen
        if (dx > 0) {
            move = if (dy > 0) WorkerAction.SOUTHEAST else if (dy < 0) WorkerAction.NORTHEAST else WorkerAction.EAST
        } else if (dx < 0) {
            move = if (dy < 0) WorkerAction.SOUTHWEST else if (dy > 0) WorkerAction.NORTHWEST else WorkerAction.WEST
        } else if (dy > 0) {
            move = WorkerAction.SOUTH
        } else if (dy < 0) {
            move = WorkerAction.NORTH
        }

        return move
    }

    override fun behaviour() = act {
        on { msg: SetupRepairerMsg ->
            repairPoints = msg.repairPoints.toMutableList()
            createGraph(msg.size, msg.obstacles!!)
            collectIDs = msg.collectorIDs
        }

        on { msg: CurrentPosition ->
            if (!over) {
                currentpos = msg.position
                if (msg.vision.isNotEmpty()) {
                    for (p in msg.vision){
                        if(!MFound.contains(p)){
                            MFound.add(p)
                            added = true
                        }
                    }
                    if (added) {
                        log.info("sending material pos")
                        for (c in collectIDs) {
                            system.resolve(c) tell foundM(msg.vision)
                        }
                    }
                    added = false
                    foundM = true
                }
                possibleMoves = allMoves.toMutableList()
                removeImpossibleMoves(allMoves)
                getworkerMove(possibleMoves)
            }
        }
        on { msg: over ->
            over = true
        }

        on { msg: CallForProposal ->
            if(!hasM && !accepted) {
                //goal = msg.position
                var distance =
                    kotlin.math.sqrt(((msg.position.x - currentpos.x) * (msg.position.x - currentpos.x) + (msg.position.y - currentpos.y) * (msg.position.y - currentpos.y)).toDouble()).toInt()
                system.resolve(msg.collectorID) tell Proposal(repairID, distance)
            }
            else{
                system.resolve(msg.collectorID) tell reject(currentpos)
            }
        }

        on{msg: accept ->
            if(!accepted){
                goal = msg.position
                accepted = true
                fromID = msg.fromID
            }
        }
        on{msg: reject ->
            accepted = false
        }
        on{msg: TransferInform ->
            hasM = true
            system.resolve(msg.fromID) tell gotM(hasM);
            goal = repairPoints.first()

        }
    }
}