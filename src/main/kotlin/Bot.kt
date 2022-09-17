import com.jessecorbett.diskord.bot.bot
import com.jessecorbett.diskord.bot.events
import kotlinx.coroutines.*

suspend fun main(): Unit = bot(System.getenv("RpgBotToken")) {
	val processor = CommandProcessor()
	GlobalScope.launch {
		processor.load("CURRENT")
		while (isActive){
			delay(10000L)
			processor.save("CURRENT")
		}
	}
	events {
		onMessageCreate {
			with(processor){
				process(it)
			}
		}
	}
}