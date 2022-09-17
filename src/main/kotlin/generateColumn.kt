fun generateTable(characters: Map<String, Character>): String = buildString {
	val additionalPadding = 2
	val maxNameSize = characters.maxOf { it.key.length } + additionalPadding
	val maxHealthSize = characters.maxOf { it.value.health.toString().length } + additionalPadding
	val maxManaSize = characters.maxOf { it.value.mana.toString().length } + additionalPadding
	val maxStaminaSize = characters.maxOf { it.value.stamina.toString().length } + additionalPadding
	val lineSeparator = buildString {
		append("+")
		append("-".repeat(maxNameSize))
		append("+")
		append("-".repeat(maxHealthSize))
		append("+")
		append("-".repeat(maxManaSize))
		append("+")
		append("-".repeat(maxStaminaSize))
		append("+")
		append("\n")
	}
	append("```md\n")
	append(lineSeparator)
	append(buildString {
		append("+")
		append(" ".repeat(maxNameSize))
		append("+")
		append("HP".padAround(maxHealthSize))
		append("+")
		append("MP".padAround(maxManaSize, biggerPaddingToLeft = false))
		append("+")
		append("SP".padAround(maxStaminaSize, biggerPaddingToLeft = false))
		append("+\n")
	})
	append(characters.asIterable().joinToString(lineSeparator, prefix = lineSeparator, postfix = lineSeparator) {
		buildString {
			append("+")
			append(it.key.padAround(maxNameSize))
			append("|")
			append(it.value.health.toString().padAround(maxHealthSize))
			append("|")
			append(it.value.mana.toString().padAround(maxManaSize, biggerPaddingToLeft = false))
			append("|")
			append(it.value.stamina.toString().padAround(maxStaminaSize, biggerPaddingToLeft = false))
			append("+")
			append("\n")
		}
	})
	append("```")
}

private fun String.padAround(length: Int, character: Char = ' ', biggerPaddingToLeft: Boolean = true) = buildString {
	val totalPadToApply = length - this@padAround.length
	val smallerPad = totalPadToApply / 2
	val biggerPad = totalPadToApply - smallerPad
	append(character.toString().repeat(if (biggerPaddingToLeft) biggerPad else smallerPad))
	append(this@padAround)
	append(character.toString().repeat(if (biggerPaddingToLeft) smallerPad else biggerPad))
}
