package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.lang.reflect.Array
import java.util.LinkedList

class SetupAgent (private val setupID: String): Agent(overrideName=setupID) {
    private var collectorIDs = listOf<String>()
    private var repairIDs = listOf<String>()
    private var repairPoints = listOf<Position>()

    override fun preStart() {
        // TODO if you want you can do something once before the normal lifecycle of your agent
        super.preStart()
        var setupMessage = SetupGameMessage(setupID,"/grids/grid16.grid")
        system.resolve("server") invoke ask<SetupGameResponse>(setupMessage) { res ->
            collectorIDs = res.collectorIDs
            repairIDs = res.repairIDs
            repairPoints = res.repairPoints
            for(c in collectorIDs){
                system.spawnAgent(CollectAgent(c))
                system.resolve(c) tell SetupCollectorMsg(res.size, res.obstacles, res.repairIDs, res.collectorIDs)
            }
            for(r in repairIDs){
                system.spawnAgent(RepairAgent(r))
                system.resolve(r) tell SetupRepairerMsg(res.size, res.obstacles, res.repairPoints,res.collectorIDs)
            }
        }
        var startMsg = StartGame(setupID)
        system.resolve("server") invoke ask<Boolean>(startMsg) { res ->
            if(!res){

            }
        }
    }

    /* TODO
        - setup the game using the SetupGameMessage, you need to define a gridfile
        - use the list of ids to spawn Repair & Collect Agents i.e. system.spawn(CollectAgent("x"))
        - if you need to, do some more prep work
        - start the game by telling the server "StartGame(setupID)"
     */
    override fun behaviour() = act {
        on<EndGameMessage> {
            for(c in collectorIDs){ system.spawnAgent(CollectAgent(c))
                system.resolve(c) tell over(true)
            }
            for(r in repairIDs){ system.spawnAgent(RepairAgent(r))
                system.resolve(r) tell over(true)
            }
            log.info("Received $it")
        }
    }
}
