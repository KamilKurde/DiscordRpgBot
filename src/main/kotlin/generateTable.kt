const val ROW_SEPARATOR = "═"
const val COLUMN_SEPARATOR = "║"
const val CELL_SEPARATOR = "╬"
const val TOP_CELL_SEPARATOR = "╦"
const val BOTTOM_CELL_SEPARATOR = "╩"
const val LEFT_ROW_SEPARATOR = "╠"
const val RIGHT_ROW_SEPARATOR = "╣"
const val TOP_LEFT_BORDER = "╔"
const val TOP_RIGHT_BORDER = "╗"
const val BOTTOM_LEFT_BORDER = "╚"
const val BOTTOM_RIGHT_BORDER = "╝"


fun generateTable(characters: Map<String, Character>): String = buildString {
	val additionalPadding = 2

	val columns = buildList {
		add(Column("") { it.first })

		CharacterField.values().forEach { field ->
			add(Column(field.name[0] + "P") { (_, character) ->
				field.get(character).toString()
			})
		}
	}

	val rows = buildList {
		add(
			columns.map {
				it.header
			}
		)
		characters.forEach { (name, character) ->
			add(
				columns.map {
					it.generateContentForCharacter(name to character)
				}
			)
		}
	}

	val maxSizes = List(columns.size) { column ->
		rows.maxOf { row ->
			row[column].length
		} + additionalPadding
	}

	val lineSeparator = maxSizes.joinToString(CELL_SEPARATOR, prefix = LEFT_ROW_SEPARATOR, postfix = "$RIGHT_ROW_SEPARATOR\n") {
		ROW_SEPARATOR.repeat(it)
	}

	val rowsPadded = rows.map {
		it.mapIndexed { index, cell ->
			cell.padAround(maxSizes[index])
		}
	}

	append("```md\n")
	append(rowsPadded.joinToString(
		lineSeparator,
		prefix = lineSeparator.replace(LEFT_ROW_SEPARATOR, TOP_LEFT_BORDER).replace(RIGHT_ROW_SEPARATOR, TOP_RIGHT_BORDER).replace(CELL_SEPARATOR, TOP_CELL_SEPARATOR),
		postfix = lineSeparator.replace(LEFT_ROW_SEPARATOR, BOTTOM_LEFT_BORDER).replace(RIGHT_ROW_SEPARATOR, BOTTOM_RIGHT_BORDER).replace(CELL_SEPARATOR, BOTTOM_CELL_SEPARATOR),
	) {
		it.joinToString(COLUMN_SEPARATOR, prefix = COLUMN_SEPARATOR, postfix = "$COLUMN_SEPARATOR\n")
	})
	append("```")
}

data class Column(val header: String, val generateContentForCharacter: (Pair<String, Character>) -> String)

private fun String.padAround(length: Int, character: Char = ' ', biggerPaddingToLeft: Boolean = true) = buildString {
	val totalPadToApply = length - this@padAround.length
	val smallerPad = totalPadToApply / 2
	val biggerPad = totalPadToApply - smallerPad
	append(character.toString().repeat(if (biggerPaddingToLeft) biggerPad else smallerPad))
	append(this@padAround)
	append(character.toString().repeat(if (biggerPaddingToLeft) smallerPad else biggerPad))
}
