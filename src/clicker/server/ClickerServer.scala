package clicker.server

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import clicker.game.GameActor
import clicker.{GameState, Update, UpdateGames}
import com.corundumstudio.socketio.{Configuration, SocketIOClient, SocketIOServer}

import scala.io.Source

class ClickerServer(val configuration: String) extends Actor {

  val config: Configuration = new Configuration {
    setHostname("localhost")
    setPort(8080)
  }
  val system: ActorSystem = ActorSystem("actor-system")
  val server: SocketIOServer = new SocketIOServer(config)

  private var actorClientMap: Map[ActorRef, SocketIOClient] = Map.empty
  private var clientActorMap: Map[SocketIOClient, ActorRef] = Map.empty

  server.addEventListener("startGame", classOf[String],
    (socket: SocketIOClient, username: String, _) => {
      val newUser = this.system.actorOf(Props(classOf[GameActor], username, this.configuration))
      this.clientActorMap += (socket -> newUser)
      this.actorClientMap += (newUser -> socket)
      socket.sendEvent("initialize", this.configuration)
    }
  )

  server.addEventListener("click", classOf[Nothing],
    (socket: SocketIOClient, _: Nothing, _) =>
      this.clientActorMap.get(socket).map(actor =>
        actor ! clicker.Click
      )
  )

  server.addEventListener("buy", classOf[String],
    (socket: SocketIOClient, itemId: String, _) =>
      this.clientActorMap.get(socket).map(actor =>
        actor ! clicker.BuyEquipment(itemId)
      )
  )

  server.addDisconnectListener((socket: SocketIOClient) =>
    this.clientActorMap.get(socket).map { actor =>
      this.clientActorMap = this.clientActorMap.filterKeys(!_.equals(socket))
      this.actorClientMap = this.actorClientMap.filterKeys(!_.equals(actor))
      actor ! PoisonPill
    }
  )

  server.start()

  override def receive: Receive = {
    case UpdateGames =>
      actorClientMap.foreach(actor => actor._1 ! Update)
    case GameState(gameState) =>
      actorClientMap.get(sender())
        .map(_.sendEvent("gameState", gameState))
    case msg =>
      println("Unexpected message: " + msg)
  }

  override def postStop(): Unit = {
    println("Stopping server")
    server.stop()
  }
}

object ClickerServer {

  private val gameConfigJsonPath = "codeConfig.json"

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem()
    import actorSystem.dispatcher

    import scala.concurrent.duration._

    val configuration: String = Source.fromFile(gameConfigJsonPath).mkString

    val server = actorSystem.actorOf(Props(classOf[ClickerServer], configuration))

    actorSystem.scheduler.schedule(0.milliseconds, 100.milliseconds, server, UpdateGames)
  }
}
