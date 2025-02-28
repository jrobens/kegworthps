import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import java.math.BigDecimal


/*data class CsvPerson(
    @CsvProperty("Order Total")
    val orderTotal: Number,
    @CsvProperty("Recipient Name")
    val recipientName: String,
    @CsvProperty("Recipient Email")
    val recipientEmail: String,
)*/

fun main(args: Array<String>) {
    readValidCsv()
}

fun isNumericToXCeleb(toCheck: String): Boolean {
    return toCheck.toDoubleOrNull() != null
}

private fun readValidCsv() {
    val writer = csvWriter().openAndGetRawWriter("wayward.csv")

    val validCategories = listOf("Autumn Fair", "None");
    val validProducts = listOf("Autumn Fair RSVP", "Autumn Fair Party")
    var rowCount = 0
    var orderCount = 0
    var ticketCount = 0

    csvReader().open("/Users/jrobens/NetBeansProjects/kegRaffle/items-2024-05-01-2024-07-01.csv") {
        // csvReader().open("/Users/jrobens/NetBeansProjects/kegRaffle/test-multiple.csv") {
        readAllAsSequence().forEach { row ->
            row.map { println(it) }
            val newRow = listOf(row.get(0),row.get(1),row.get(9), row.get(14), row.get(22), row.get(23))
            val categoryName = row.get(3)
            val productName = row.get(4)
            val total = row.get(9).replace("$", "")
            // $10 a ticket
            if (validProducts.contains(productName) && validCategories.contains(categoryName)) {
                orderCount++
                if (productName.equals("Autumn Fair RSVP", ignoreCase = true)) {
                    writer.writeRow(newRow)
                }
                val tickets = total.toBigDecimal().div(80.toBigDecimal());
                for (i in 1..tickets.toInt()) {
                    ticketCount++
                    writer.writeRow(newRow)
                }
            } else {
                if (rowCount == 0) {
                    writer.writeRow(newRow)
                }
            }
            rowCount++
        }
        println("""Total orders found $orderCount. Tickets found $ticketCount""")
    }

    writer.close()
}
