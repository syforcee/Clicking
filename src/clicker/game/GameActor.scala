package clicker.game

import akka.actor.Actor
import clicker.{BuyEquipment, Click, GameState, Update}
import play.api.libs.json.{JsValue, Json}

final case class Item(
  id: String,
  name: String,
  incomePerClick: Int,
  incomePerSecond: Int,
  initialCost: Int,
  priceExponent: Double
)

final case class OwnedItem(
  item: Item,
  quantity: Int,
  cost: Double
)

final case class CurrentState(var currency: Double, ownedItems: Seq[OwnedItem])

class GameActor(username: String, configuration: String) extends Actor {

  import GameActor._

  private val ItemList: Seq[Item] = readEquipment(configuration)

  def receive: Receive = gameState(
    0,
    ItemList.map(item => OwnedItem(item, 0, item.initialCost)),
    System.nanoTime()
  )

  def gameState(currency: Double, ownedItems: Seq[OwnedItem], time: Long): Receive = {
    case Click =>
      val currencyDiff: Double = ownedItems.foldLeft(1.0)(
        (sum, ownedItem) => sum + (ownedItem.item.incomePerClick * ownedItem.quantity)
      )
      context.become(gameState(
        currency + currencyDiff,
        ownedItems,
        time
      ))
    case BuyEquipment(itemId) =>
      val updatedItems: Seq[(OwnedItem, Double)] = ownedItems.map(item => {
        if (item.item.id == itemId) {
          (OwnedItem(item.item, item.quantity + 1, item.cost * item.item.priceExponent), item.cost)
        } else {
          (item, 0.0)
        }
      })
      if (updatedItems.map(_._2).sum <= currency) {
        context.become(gameState(
          currency - updatedItems.map(_._2).sum,
          updatedItems.map(_._1),
          time
        ))
      } else {
        context.become(gameState(currency, ownedItems, time))
      }
    case Update =>
      val newTime = System.nanoTime()
      val timeDifference: Double = (newTime - time).toDouble / 1000000000.0
      val currencyDifference: Double = ownedItems.foldLeft(0.0)(
        (sum, ownedItem) => sum + (ownedItem.item.incomePerSecond * ownedItem.quantity * timeDifference)
      )
      val gameStateJson: String = writeGameState(username, currency + currencyDifference, ownedItems)
      sender() ! GameState(gameStateJson)
      context.become(gameState(
        currency + currencyDifference,
        ownedItems,
        newTime
      ))
    case msg =>
      println("Unexpected message: " + msg)
  }
}

object GameActor {

  private def readEquipment(configuration: String): Seq[Item] = {
    val jsonValue = Json.parse(configuration)
    val equipmentJsonList = (jsonValue \ "equipment").as[List[Map[String, JsValue]]]
    equipmentJsonList.map(item => Item(
      id = item("id").as[String],
      name = item("name").as[String],
      incomePerClick = item("incomePerClick").as[Int],
      incomePerSecond = item("incomePerSecond").as[Int],
      initialCost = item("initialCost").as[Int],
      priceExponent = item("priceExponent").as[Double]
    ))
  }

  private def writeGameState(username: String, currency: Double, ownedEquipment: Seq[OwnedItem]): String = {
    val equipmentJson: Seq[Map[String, JsValue]] = writeEquipmentListJson(ownedEquipment)
    Json.stringify(
      Json.toJson(Map(
        "username" -> Json.toJson(username),
        "currency" -> Json.toJson(currency),
        "equipment" -> Json.toJson(equipmentJson)
      ))
    )
  }

  private def writeEquipmentListJson(ownedEquipment: Seq[OwnedItem]) =
    ownedEquipment.map {
      equipment => {
        Map[String, JsValue](
          "id" -> Json.toJson(equipment.item.id),
          "numberOwned" -> Json.toJson(equipment.quantity),
          "cost" -> Json.toJson(equipment.cost)
        )
      }
    }
}
