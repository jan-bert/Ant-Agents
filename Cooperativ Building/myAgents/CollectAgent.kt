package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.*
import java.lang.Math.abs
import java.util.Random
import kotlin.concurrent.timerTask


class CollectAgent (private val collectID: String): Agent(overrideName=collectID) {
    private var added = false;
    private var goal = Position(0,0)
    private var goto = false;
    private var proposals = mutableMapOf<String, Int>();
    private var cnpactive = false;
    private var wait = false;
    private var repairIDs = listOf<String>();
    private var collectIDs = listOf<String>();
    private var smart = false;
    private var over = false;
    private var size = Position(0,0);
    private var lastmove = WorkerAction.NORTH;
    private var MFound = mutableListOf<Position>();
    private var hasM = false;
    private val allMoves = mutableListOf(WorkerAction.NORTH, WorkerAction.NORTHEAST, WorkerAction.EAST, WorkerAction.SOUTHEAST, WorkerAction.SOUTH, WorkerAction.SOUTHWEST, WorkerAction.WEST, WorkerAction.NORTHWEST)
    private var possibleMoves = mutableListOf(WorkerAction.NORTH, WorkerAction.NORTHEAST, WorkerAction.EAST, WorkerAction.SOUTHEAST, WorkerAction.SOUTH, WorkerAction.SOUTHWEST, WorkerAction.WEST, WorkerAction.NORTHWEST)
    private var currentpos = Position(0, 0);
    var obstacles = mutableListOf<Position>()
    /* TODO
        - this WorkerAgent has the ability to collect material
        - NOTE: can not walk on open repairpoints, can not drop material
        - find material, collect it, start a cnp instance
        - once your cnp is done, meet the RepairAgents and transfer the material
     */
    fun cnp (){
        log.info("started cnp")
        wait = true
        cnpactive = true;
        for(r in repairIDs){
            system.resolve(r) tell CallForProposal(collectID, currentpos)
        }
        var t = Timer();
        var rt = timerTask {
            log.info("timer done")
            wait = false
            var min = proposals.values.min()
            var winner = ""
            for(k in proposals.keys){
                if(min == proposals.get(k)){
                    winner = k
                }
            }
            log.info("winnder is: "+ winner)
            system.resolve(winner) tell accept(currentpos, collectID)

            proposals.remove(winner)

            for(k in proposals.keys){
                system.resolve(k) tell reject(currentpos)
            }
            proposals.clear()
        }
        t.schedule(rt,200)
        return
    }
    fun getworkerMove(movesList : MutableList<WorkerAction>){
        var x = WorkerAction.NORTH
        if(MFound.contains(currentpos) && !hasM){
            x = WorkerAction.TAKE
            hasM = true;
            goto = false;
            attemptMove(x)
        }
        else{
            if(!hasM && MFound.isNotEmpty()) {
                for (p in MFound) {
                    if (null != calculateact(p)) {
                        smart = true
                        x = calculateact(p)!!
                    }
                }
            }
            if(!hasM && !smart) {
                val rand = Random()
                movesList.remove(lastmove)
                x = movesList.get(abs((rand.nextInt()) % (movesList.size)))
            }
        }
        if(goto && !hasM) {
            var y = moveTo(goal) // Repairer sagen wo er hin soll
            if (y != null) {
                if (movesList.contains(y)) {
                    attemptMove(y)
                } else {
                    attemptMove(movesList.first())
                }
            }
        }
        else {
            if(!hasM) {
                attemptMove(x)
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

    fun attemptMove(act : WorkerAction){
        //ans == ture -> move ist possible (kein obstacle)
        val actMsg = WorkerActionRequest(collectID,act)
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

    fun calculateact(pos: Position): WorkerAction? {
        //berechnet aus einer Position einen Move
        if(pos == Position(currentpos.x, currentpos.y-1)){
            return allMoves[0]
        }else if(pos == Position(currentpos.x+1, currentpos.y-1)){
            return allMoves[1]
        }else if(pos == Position(currentpos.x+1, currentpos.y)){
            return allMoves[2]
        }else if(pos == Position(currentpos.x+1, currentpos.y+1)){
            return allMoves[3]
        }else if( pos == Position(currentpos.x, currentpos.y+1)){
            return allMoves[4]
        }else if(pos == Position(currentpos.x-1, currentpos.y+1)){
            return allMoves[5]
        }else if(pos == Position(currentpos.x-1, currentpos.y)){
            return allMoves[6]
        }else if( pos == Position(currentpos.x-1, currentpos.y-1)){
            return allMoves[7]
        }
        else{
            return null
        }
    }

    override fun behaviour() = act {
        on{msg: SetupCollectorMsg ->
            obstacles = msg.obstacles!!.toMutableList()
            size = msg.size
            repairIDs = msg.repairIDs
            collectIDs = msg.collectorIDs
        }
        on{msg: CurrentPosition ->
            //wenn die position mit geteilt wird und das spiel noch nicht zu ende ist wird ei move berechnet
            if (!over) {

                currentpos = msg.position
                if (msg.vision.isNotEmpty()) {
                    for (p in msg.vision){
                        if(!MFound.contains(p)){
                            MFound.add(p)
                            added = true
                        }
                    }
                    if(added) {
                        for (c in collectIDs) {
                            //wenn neue materialien gefunden wurden sende die position an die collector
                            system.resolve(c) tell foundM(msg.vision)
                        }
                    }
                    added =false
                }
                possibleMoves = allMoves.toMutableList()

                    removeImpossibleMoves(allMoves)

                if(!cnpactive && hasM) {
                    //wenn der collector Material hält und kein cnp aktiv ist wird eins gestartet
                    cnp();
                }
                getworkerMove(possibleMoves)
            }
        }
        on{msg: Proposal ->
            log.info("got proposal from " + msg.repairID+ " " + msg.dist)
            if(wait && cnpactive){
                proposals.put(msg.repairID,msg.dist)
            }
        }
        on{msg: over ->
            over = true
        }
        on{msg: foundM->
            log.info("got send material")
            for (p in msg.foundM){
                if(!MFound.contains(p)){
                    MFound.add(p)
                    if(goto) {
                        var distancenew =
                            kotlin.math.sqrt(((p.x - currentpos.x) * (p.x - currentpos.x) + (p.y - currentpos.y) * (p.y - currentpos.y)).toDouble()).toInt()
                        var distanceold =
                            kotlin.math.sqrt(((goal.x - currentpos.x) * (goal.x - currentpos.x) + (goal.y - currentpos.y) * (goal.y - currentpos.y)).toDouble()).toInt()
                        if(distancenew > distanceold){
                            //wenn neueres material näher ist wird das verfolgt
                            goal = p
                        }
                    }
                }
            }
            if(!goto) {
                goal = MFound.first()
                goto = true;
            }
        }
        on{msg: gotM ->
            log.info("Transferd material")
            hasM = false;
        }
        on{msg: reject->
          log.info("repair agent is busy")
        }
    }
}
