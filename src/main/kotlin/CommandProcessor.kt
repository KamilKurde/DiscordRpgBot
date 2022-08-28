import com.jessecorbett.diskord.api.channel.EmbedFooter
import com.jessecorbett.diskord.api.common.Message
import com.jessecorbett.diskord.bot.BotContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

class CommandProcessor {

	private val characters: MutableMap<String, Character> = mutableMapOf()

	private val json = Json {
		encodeDefaults = true
		ignoreUnknownKeys = true
	}

	context(BotContext)
	suspend fun process(message: Message) {
		if (message.content.startsWith("rpg ", ignoreCase = true).not()) return
		val content = message.content.drop(4)
		when {
			content.startsWith("create", ignoreCase = true) -> create(content.drop(7), message)
			content.startsWith("clear", ignoreCase = true) -> clear(content.drop(6), message).also { message.reply { footer = EmbedFooter("Cleared") } }
			content.startsWith("save", ignoreCase = true) -> save(content.drop(5)).also { message.reply { footer = EmbedFooter("Saved") } }
			content.startsWith("load", ignoreCase = true) -> load(content.drop(5)).also { message.reply { footer = EmbedFooter("Loaded") } }
			characters.any { content.startsWith(it.key, ignoreCase = true) } -> characters.asIterable().first { content.startsWith(it.key, ignoreCase = true) }.run { value.modify(content.drop(key.length + 1), message) }
			else -> return
		}
	}

	fun save(name: String) = File("$name.rpg").writeText(json.encodeToString(characters))

	fun load(name: String) = try {
		json.decodeFromString<Map<String, Character>>(File("$name.rpg").readText())
	} catch (e: FileNotFoundException) {
		emptyMap()
	}.let {
		characters.clear()
		characters.putAll(it)
	}

	private fun clear(name: String, trigger: Message){
		when{
			name.isBlank() -> characters.clear()
			else -> File("$name.rpg").delete()
		}
	}

	context(BotContext)
	private suspend fun create(content: String, trigger: Message) {
		val name = content.substringBefore(" ").trim()
		characters[name] = Character()
		trigger.react("\uD83C\uDD97")
	}

	context(BotContext)
	private suspend fun Character.modify(content: String, trigger: Message) {
		val key = content.substringBefore(" ")
		CharacterField.values().firstOrNull {
			it.name.lowercase() == key.lowercase()
		}?.let {
			updateField(content.drop(it.name.length + 1), trigger, it)
		}
	}

	context(BotContext)
	private suspend fun Character.updateField(content: String, trigger: Message, field: CharacterField) {
		val key = content.substringBefore(" ")
		val value = content.substringAfter(" ").trim()
		val converted = value.toIntOrNull()
		if (converted == null) {
			when (value.isBlank()) {
				true -> trigger.reply {
					title = "${field.emoji} ${field.name}"
					footer = EmbedFooter(
						field.get(this@updateField).toString()
					)
				}

				false -> trigger.reply("\"$value\" is not a number")
			}
			return
		}
		Operation.values().firstOrNull {
			it.name.lowercase() == key.lowercase()
		}?.let {
			val oldValue = field.get(this)
			val newValue = it.perform(oldValue, converted)
			field.update(this, newValue)
			trigger.reply {
				title = "${field.emoji} ${field.name}($oldValue) ${it.symbol} $converted"

				footer = EmbedFooter(
					"Current: $newValue"
				)
			}
		}
	}
}