package clicker.server

import akka.actor.{Actor, ActorSystem, Props}
import clicker.UpdateGames
import com.corundumstudio.socketio.{Configuration, SocketIOServer}

import scala.io.Source


class ClickerServer(val configuration: String) extends Actor {

  val config: Configuration = new Configuration {
    setHostname("localhost")
    setPort(8080)
  }

  val server: SocketIOServer = new SocketIOServer(config)
  server.start()


  override def receive: Receive = {
    case _ =>
  }


  override def postStop(): Unit = {
    println("stopping server")
    server.stop()
  }
}

object ClickerServer {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem()
    import actorSystem.dispatcher

    import scala.concurrent.duration._

    val configuration: String = Source.fromFile("codeConfig.json").mkString

    val server = actorSystem.actorOf(Props(classOf[ClickerServer], configuration))

    actorSystem.scheduler.schedule(0.milliseconds, 100.milliseconds, server, UpdateGames)
  }
}
