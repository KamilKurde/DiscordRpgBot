import com.jessecorbett.diskord.api.channel.EmbedFooter
import com.jessecorbett.diskord.api.common.*
import com.jessecorbett.diskord.bot.BotContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

class CommandProcessor {

	val characters: MutableMap<String, Character> = mutableMapOf()

	val json = Json {
		encodeDefaults = true
		ignoreUnknownKeys = true
	}

	private val commands: List<Command>
		get() = buildList {
			addAll(StaticCommand.values())
			val characterCommands = characters.map {
				CharacterCommand(it.key) { _, content, trigger ->
					it.value.modify(content, trigger)
				}
			}
			addAll(characterCommands)
		}

	suspend fun BotContext.process(message: Message) {
		if (message.content.startsWith("rpg ", ignoreCase = true).not()) return
		val content = message.content.drop(4)

		val command = commands.firstOrNull {
			content.startsWith(it.name, ignoreCase = true)
		}

		command?.execute?.let { it(this@process, this@CommandProcessor, content.drop(command.name.length + 1), message) }
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
			if (converted < 0 && it.allowNegativeArguments.not()) return
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

	sealed interface Command {
		val name: String
		val execute: suspend BotContext.(processor: CommandProcessor, content: String, trigger: Message) -> Unit
	}

	enum class StaticCommand(val help: String, override val execute: suspend BotContext.(processor: CommandProcessor, content: String, trigger: Message) -> Unit) : Command {
		Clear("Clears current session", { processor, name, trigger ->
			when {
				name.isBlank() -> processor.characters.clear()
				else -> File("$name.rpg").delete()
			}
			trigger.reply { footer = EmbedFooter("Cleared") }
		}),
		Create("Creates character with given name", { processor, content, trigger ->
			val name = content.substringBefore(" ").trim()
			processor.characters[name] = Character()
			trigger.react("\uD83C\uDD97")
		}),
		Delete("Deletes character with given name", { processor, content, trigger ->
			processor.characters.asIterable().firstOrNull {
				it.key.equals(content, ignoreCase = true)
			}?.let { (characterName) ->
				processor.characters.remove(characterName)
				trigger.react("\uD83C\uDD97")
			}
		}),
		Help("Shows this screen", { _, _, trigger ->
			trigger.reply {
				val commands = StaticCommand.values().mapNotNull {
					it.name to it.help
				}
				title = "Commands:"
				description = buildString {
					appendLine("**Static**:")
					commands.forEach {
						appendLine("* **" + it.first.uppercase() + "** - " + it.second)
					}
					appendLine("\n**Character-specific**:")
					appendLine("* **[CHARACTER_NAME] [CHARACTER_FIELD]** - Gets given character field")
					appendLine("* **[CHARACTER_NAME] [CHARACTER_FIELD] [OPERATION] [VALUE]** - Performs given operation on character field")
					appendLine("**Character fields**: ${ CharacterField.values().joinToString{ it.name }}")
					appendLine("**Operations**: ${ Operation.values().joinToString { it.name } }")
				}
			}
		}),
		Load("Loads session with given name as current session", { processor, name, trigger ->
			try {
				trigger.reply {
					footer = EmbedFooter("Loaded")
				}
				processor.json.decodeFromString<Map<String, Character>>(File("$name.rpg").readText())
			} catch (e: FileNotFoundException) {
				trigger.reply {
					footer = EmbedFooter("Failed to load, session with given name doesn't exist")
				}
				emptyMap()
			}.let {
				processor.characters.clear()
				processor.characters.putAll(it)
			}
		}),
		Save("Saves current session with given name", { processor, name, trigger ->
			processor.save(name)
			trigger.reply { footer = EmbedFooter("Saved") }
		}),
		Status("Prints all data about current session", { processor, _, trigger ->
			trigger.reply {
				this.title = "Status"
				this.description = generateTable(processor.characters)
			}
		})
	}

	class CharacterCommand(override val name: String, override val execute: suspend BotContext.(processor: CommandProcessor, content: String, trigger: Message) -> Unit) : Command
}