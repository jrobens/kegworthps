import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter

fun main(args: Array<String>) {
    readValidCsv()
}

private fun readValidCsv() {
    val writer = csvWriter().openAndGetRawWriter("sausages220240624.csv")

    val validCategories = listOf("Athletics");
    val validProducts = listOf("Athletics sausage sizzle")
    var rowCount = 0
    var orderCount = 0
    var ticketCount = 0

    csvReader({quoteChar = '"'}).open("/Users/jrobens/NetBeansProjects/kegRaffle/items-2024-03-01-2024-07-01.csv") {
        // csvReader().open("/Users/jrobens/NetBeansProjects/kegRaffle/test-multiple.csv") {
        readAllAsSequence().forEach { row ->
            row.map { println(it) }
            val newRow = listOf(row.get(0),row.get(1),row.get(8),row.get(9), row.get(14), row.get(22), row.get(23))
            val categoryName = row[3]
            val productName = row[4]
            println("$categoryName - $productName")
            val total = row.get(9).replace("$", "")
            // $6 a ticket
            orderCount++
            if (isNumericToX(total) && validProducts.contains(productName) && validCategories.contains(categoryName)) {
                val tickets = total.toBigDecimal().div(6.toBigDecimal());
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
