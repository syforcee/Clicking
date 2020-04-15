package clicker

// Received by  GameActors
case object Update
case object Click
case class BuyEquipment(equipmentId: String)

// Received by ClickerServer
case object UpdateGames
case class GameState(gameState: String)
