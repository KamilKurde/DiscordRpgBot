import kotlinx.serialization.Serializable

@Serializable
data class Character(
	var health: Int = 0,
	var mana: Int = 0,
	var stamina: Int = 0,
)

enum class CharacterField(val update: Character.(to: Int) -> Unit, val get: Character.() -> Int, val emoji: String) {
	Health(update = { health = it }, get = { health }, emoji = "\uD83D\uDFE5"),
	Mana(update = { mana = it }, get = { mana }, emoji = "\uD83D\uDFE6"),
	Stamina(update = { stamina = it }, get = { stamina }, emoji = "\uD83D\uDFE9"),
}

enum class Operation(val perform: (oldValue: Int, argument: Int) -> Int, val symbol: String) {
	Set(perform = { _, argument -> argument }, symbol = "="),
	Add(perform = { oldValue, argument -> oldValue + argument }, symbol = "+"),
	Sub(perform = { oldValue, argument -> oldValue - argument }, symbol = "-"),
}