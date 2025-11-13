package dataframe

import org.jetbrains.kotlinx.jupyter.api.DisplayResult
import org.jetbrains.kotlinx.jupyter.api.HTML
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.Renderable

class MyTable(val headers: List<String>, val rows: List<List<Any>>) : Renderable {
	override fun render(notebook: Notebook): DisplayResult {
		val html = buildString {
			append("""
				<style>
					th{
					background-color: red;
					}
					@media (prefers-color-scheme: dark) {
					th{
					background-color: blue;
					}
					}
				</style>
			""".trimIndent())

			append("<table class='datatable'>")

			// Header
			append("<thead><tr>")
			headers.forEach { header ->
				append("<th>")
				append(header)
				append("</th>")
			}
			append("</tr></thead>")

			// Body
			append("<tbody>")
			rows.forEach { row ->
				append("<tr>")
				row.forEach { cell ->
					append("<td>")
					append(cell.toString())
					append("</td>")
				}
				append("</tr>")
			}
			append("</tbody>")

			append("</table>")
		}

		return HTML(html)
	}
}