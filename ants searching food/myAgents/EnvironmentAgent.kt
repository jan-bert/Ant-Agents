package de.dailab.jiacvi.aot.gridworld.myAgents
import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import de.dailab.jiacvi.dsl.agentSystem
import org.kodein.di.newInstance
import java.util.LinkedList


/**
 * Stub for your EnvironmentAgent
 * */
class EnvironmentAgent(private val envId: String): Agent(overrideName=envId) {
    // TODO you might need to put some variables to save stuff here
    private var gameRunning = true
    private val antIds =  LinkedList<String>()
    private var nestPheromones = Array(0) { FloatArray(0) }
    private var foodPheromones = Array(0) { FloatArray(0) }
    private var size = Position(0, 0)
    private var nestpos = Position(0,0)
    private var obstacles: ArrayList<Position>? = null


    fun getCorrectPheromones(pos: Position, antHasFood : Boolean): MutableList<Float> {
        var ans = emptyList<Float>()
        ans = ans.plus(pheromonesCheck(pos.x, pos.y-1, antHasFood))
        ans = ans.plus(pheromonesCheck(pos.x+1, pos.y-1, antHasFood))
        ans = ans.plus(pheromonesCheck(pos.x+1, pos.y, antHasFood))
        ans = ans.plus(pheromonesCheck(pos.x+1, pos.y+1, antHasFood))
        ans = ans.plus(pheromonesCheck(pos.x, pos.y+1, antHasFood))
        ans = ans.plus(pheromonesCheck(pos.x-1, pos.y+1, antHasFood))
        ans = ans.plus(pheromonesCheck(pos.x-1, pos.y, antHasFood))
        ans = ans.plus(pheromonesCheck(pos.x-1, pos.y-1, antHasFood))
        return ans.toMutableList()
    }

    fun pheromonesCheck(x:Int, y: Int, antHasFood : Boolean): Float{
        if(x < 0 || x >= size.x || y < 0 || y >= size.y ){
            return 0.0f
        }else if(antHasFood){
            return nestPheromones[x][y]
        }
        return foodPheromones[x][y]
    }



    override fun preStart() {
        // TODO if you want you can do something once before the normal lifecycle of your agent
        super.preStart()

        for(i in 1..40){
            val antId = "Ant".plus(i)
            antIds.add(antId)
            log.info("Ant".plus(i))
            system.spawnAgent(AntAgent(antId))
        }
        val msg = StartGameMessage(envId,antIds)
        system.resolve("server") invoke ask<StartGameResponse>(msg) { res ->
            size = res.size
            nestPheromones = Array(size.x+1) { FloatArray(size.y+1) }
            foodPheromones = Array(size.x+1) { FloatArray(size.y+1) }
            nestpos = res.nestPosition
            //obstacles = res.obstacles as ArrayList<Position>
            for(a in antIds){
                system.resolve(a) tell GiveAntPosMessage(size, nestpos)
            }
        }
    }

    override fun behaviour() = act {
        /* TODO here belongs most of your agents logic.
        *   - Check the readme "Reactive Behaviour" part and see the Server for some examples
        *   - try to start a game with the StartGameMessage
        *   - you need to initialize your ants, they don't know where they start
        *   - here you should manage the pheromones dropped by your ants
        *   - REMEMBER: pheromones should transpire, so old routes get lost
        *   - adjust your parameters to get better results, i.e. amount of ants (capped at 40
        */
        //abfangen wenn eine runde anfängt
        listen(BROADCAST_TOPIC) {res: GameTurnInform->
            //Verdunstung von Nest Pheromonen und Food Pheromonen
            for(i in 0 ..size.x-1){
                for(j in 0 ..size.y-1){
                    if(nestPheromones[i][j] <= 0.01f){
                        nestPheromones[i][j] = 0.0f
                    }
                    if(foodPheromones[i][j] <= 0.01f){
                        foodPheromones[i][j] = 0.0f
                    }
                    nestPheromones[i][j] = nestPheromones[i][j] * 0.9f
                    foodPheromones[i][j] = foodPheromones[i][j] * 0.9f
                }
            }
            //alle Ameisen einen Zug machen lassen
            for (a in antIds){
                val antMsg = AntTurnMessage(res.gameTurn)
                //Ameise aufforden einen Zug zu machen
                system.resolve(a) invoke ask<AntTurnResponse>(antMsg){ antRes ->
                    //wenn Nest true soll nestPheromon gespeichert werden
                    if(antRes.nest){
                        //nest Pheromon an die richtige stelle speichern
                        nestPheromones[antRes.pos.x][antRes.pos.y] = nestPheromones[antRes.pos.x][antRes.pos.y] + antRes.i
                    }
                    else{
                        //sonst FoodPheromon an die stelle speichern
                        foodPheromones[antRes.pos.x][antRes.pos.y] = foodPheromones[antRes.pos.x][antRes.pos.y] + antRes.i
                    }
                }
            }
        }
        respond<PheromonesRequest, PheromonesResponse> { msg ->
            return@respond PheromonesResponse(getCorrectPheromones(msg.pos, msg.hasFood))
        }

        on{message : EndGameMessage ->
            log.info("collected Food: "+ message.foodCollected + " " + "total Food: " + message.totalFood + " " + "score: " + (message.score).toString())
        }

    }

}
