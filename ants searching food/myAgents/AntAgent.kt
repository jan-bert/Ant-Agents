package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.lang.Math.abs
import java.util.Random

/**
 * Stub for your AntAgent
 * */
class AntAgent(antId: String): Agent(overrideName=antId) {
    // TODO you might need to put some variables to save stuff here
    var currentpos = Position(0, 0);
    private var lastmove = AntAction.NORTH
    private var lastnotmove = 1.0f
    private var nest = Position(0, 0);
    private var foundFood = false;
    private var food = false;
    private var id = antId;
    private var ans = true
    private var possibleMoves = mutableListOf(AntAction.NORTH, AntAction.NORTHEAST, AntAction.EAST, AntAction.SOUTHEAST, AntAction.SOUTH, AntAction.SOUTHWEST, AntAction.WEST, AntAction.NORTHWEST)

    //update currentpos
    fun calculateCP(act : AntAction){
        if(act == possibleMoves[0]){
            currentpos = Position(currentpos.x, currentpos.y-1)
        }else if(act == possibleMoves[1]){
            currentpos = Position(currentpos.x+1, currentpos.y-1)
        }else if(act == possibleMoves[2]){
            currentpos = Position(currentpos.x+1, currentpos.y)
        }else if(act == possibleMoves[3]){
            currentpos = Position(currentpos.x+1, currentpos.y+1)
        }else if(act == possibleMoves[4]){
            currentpos = Position(currentpos.x, currentpos.y+1)
        }else if(act == possibleMoves[5]){
            currentpos = Position(currentpos.x-1, currentpos.y+1)
        }else if(act == possibleMoves[6]){
            currentpos = Position(currentpos.x-1, currentpos.y)
        }else if(act == possibleMoves[7]){
            currentpos = Position(currentpos.x-1, currentpos.y-1)
        }
    }

    //check ob ein AntMove funktioniert
    fun attemptMove(act : AntAction){
        //ans == ture -> move ist possible (kein obstacle)
        ans = true
        val actMsg = AntActionRequest(id,act)
        system.resolve("server") invoke ask<AntActionResponse>(actMsg){res ->
            //prüft ob der aktuelle move so geht
            when (res.flag) {
                ActionFlag.OBSTACLE -> ans = false
                ActionFlag.HAS_FOOD -> {
                    if (!food) {
                        foundFood = true
                    }
                }
                else -> {}
            }
            if(!res.state) ans = false
            if(ans){
                lastmove = act
                calculateCP(act)
                lastnotmove += 1.0f
            }
        }
    }
    fun getAntMove(movesList : MutableList<AntAction>){
        //pheromonesList: Floats in einer Liste mit 8 Werten, wobei Index:N = 0, NE = 1, E = 2, SE = 3, S = 4, SW = 5, W = 6, NW = 7 Position in Liste
        var pheromonesList = emptyList<Float>()
        val ask = PheromonesRequest(currentpos, food)
        system.resolve("env") invoke ask<PheromonesResponse>(PheromonesRequest(currentpos, food)) { pheromones ->
            pheromonesList = pheromones.pheromones

            //Ant geht nie zu letzter pos zurück
            val last = abs((movesList.indexOf(lastmove) + 4) % movesList.size)
            movesList.removeAt(last)
            var x = AntAction.NORTH
            val rand = Random()
            var tryr = rand.nextDouble(0.0,1.0)
            if (pheromonesList.max() != 0.0f && pheromonesList.isNotEmpty() && tryr >0.29) {
                //Move nach Pheromonen auswählen
                val y = possibleMoves[pheromonesList.indexOf(pheromonesList.max())]
                if (y == possibleMoves[last]) {
                    x = possibleMoves[pheromonesList.indexOf(pheromonesList.sorted().elementAt(1))]
                } else {
                    x = y
                }
                //log.info("Used Pheromones")

            } else {

                //random neuen move Auswählen
                x = movesList.get(abs((rand.nextInt()) % (movesList.size)))
                //log.info("Used random")
                //log.info(id + " " + currentpos)
            }
            //food an currentpos -> nächster Move = food nehmen
            if (foundFood) {
                x = AntAction.TAKE
                foundFood = false
                food = true
                attemptMove(x)
                lastnotmove = 1.0f
            } else //food im Maul + currentpos = nest -> nächster Move = food droppen
                if (food && currentpos == nest) {
                    x = AntAction.DROP
                    attemptMove(x)
                    food = false
                    lastnotmove = 1.0f
                } else //weder TAKE noch DROP -> versuche zu laufen
                    attemptMove(x)
                    if (!ans) {
                        movesList.remove(x)
                        //rekursive Schleife die so lange neue Moves ausprobiert, bis kein obstacle den Weg versperrt //TODO ist das so richtig?
                        getAntMove(movesList)
                    }
        }
        return
    }

    override fun behaviour() = act {
        /* TODO here belongs most of your agents logic.
        *   - Check the readme "Reactive Behaviour" part and see the Server for some examples
        *   - try to make a move in the gridworld
        *   - build your ant algorithm by communicating with your environment when looking for the way
        *   - adjust your parameters to get better results
        */

        on<String>({it == "welcome little ant"}){
            log.info("Got hello message\n")
        }

        on{message: GiveAntPosMessage ->
            log.info("Give received")
            currentpos = message.nestPosition
            nest = message.nestPosition
            //log.info(currentpos.toString())
        }
        respond<AntTurnMessage, AntTurnResponse> { msg ->
            if(msg.gameTurn > 0){
                val possibleMoves = possibleMoves.toMutableList()
                //ruft rekursive Funktion auf um einen möglichen kommenden move zu berechnen
                getAntMove(possibleMoves)
            }
            var i = 2.0f/lastnotmove

            AntTurnResponse(currentpos, i , !food)
        }
    }
}
