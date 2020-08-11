package clicker

// Received by  GameActors
case object Update
case object Click
final case class BuyEquipment(equipmentId: String)

// Received by ClickerServer
case object UpdateGames
final case class GameState(gameState: String)
