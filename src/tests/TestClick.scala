package tests

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import clicker._
import clicker.game.GameActor
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.duration._
import scala.io.Source


class TestClick extends TestKit(ActorSystem("TestGame"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  val EPSILON: Double = 0.000001

  def equalDoubles(d1: Double, d2: Double): Boolean = {
    (d1 - d2).abs < EPSILON
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Clicker actor" must {
    "react to clicks and equipment purchases" in {

      val configuration: String = Source.fromFile("goldConfig.json").mkString
      val gameActor = system.actorOf(Props(classOf[GameActor], "test", configuration))

      gameActor ! Click

      expectNoMessage(200.millis)

      gameActor ! Update
      val state: GameState = expectMsgType[GameState](1000.millis)

      val jsonState = state.gameState
      val gameState: JsValue = Json.parse(jsonState)
      val gold = (gameState \ "currency").as[Double]
      val expectedGold = 1.0
      assert(equalDoubles(gold, expectedGold))

    }
  }
}
