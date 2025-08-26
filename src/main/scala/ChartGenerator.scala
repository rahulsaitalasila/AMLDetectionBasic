//import org.knowm.xchart.{CategoryChartBuilder, BitmapEncoder}
//import org.knowm.xchart.style.Styler
//import scala.io.Source
//import scala.collection.JavaConverters._
//
//
//object ChartGenerator {
//  def main(args: Array[String]): Unit = {
//    val source = Source.fromFile("data/transactions.csv")
//    val lines = source.getLines().drop(1).toList
//    source.close()
//
//    val amounts = lines.map(_.split(",")(1).toDouble)
//    val suspiciousCount = amounts.count(_ > 100000)
//    val normalCount = amounts.size - suspiciousCount
//
//    val categories = List("Suspicious", "Normal")
//    val values = List(suspiciousCount, normalCount)
//
//    val chart = new CategoryChartBuilder()
//      .width(800)
//      .height(600)
//      .title("Suspicious vs Normal Transactions")
//      .xAxisTitle("Status")
//      .yAxisTitle("Count")
//      .build()
//
//    chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNW)
//    chart.addSeries("Transactions", categories.asJava, values.map(_.asInstanceOf[Number]).asJava)
//
//    BitmapEncoder.saveBitmap(chart, "dashboard/status_chart", BitmapEncoder.BitmapFormat.PNG)
//    println("Chart saved to dashboard/status_chart.png")
//  }
//}

import org.knowm.xchart.{CategoryChartBuilder, BitmapEncoder}
import org.knowm.xchart.style.Styler
import scala.io.Source
import scala.collection.JavaConverters._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
object ChartGenerator {
  def main(args: Array[String]): Unit = {
    val source = Source.fromFile("data/transactions.csv")
    val lines = source.getLines().drop(1).toList
    source.close()

    val formatter = DateTimeFormatter.ISO_DATE_TIME
    val highRiskCountries = Set("Nigeria", "Panama", "Russia", "North Korea")

    case class Transaction(id: String, account: String, amount: Double, country: String, timestamp: LocalDateTime)

    val transactions = lines.map { line =>
      val parts = line.split(",")
      Transaction(
        parts(0),
        parts(1),
        parts(2).toDouble,
        parts(3),
        LocalDateTime.parse(parts(4), formatter)
      )
    }

    // Rule 1: High Amount
    val highAmountCount = transactions.count(_.amount > 100000)

    // Rule 2: Round Amount
    val roundAmountCount = transactions.count(tx => tx.amount % 10000 == 0)

    // Rule 3: Frequent Transactions
    val groupedByAccount = transactions.groupBy(_.account)
    val frequentTxCount = groupedByAccount.count { case (_, txs) =>
      import java.time.LocalDateTime
      implicit val localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(java.time.ZoneOffset.UTC))
      val sorted = txs.sortBy(_.timestamp)

      sorted.sliding(2).exists {
        case List(a, b) => java.time.Duration.between(a.timestamp, b.timestamp).toMinutes <= 10
        case _ => false
      }
    }

    // Rule 4: High-Risk Country
    val highRiskCount = transactions.count(tx => highRiskCountries.contains(tx.country))

    val categories = List("High Amount", "Round Amount", "Frequent Transactions", "High-Risk Country")
    val values = List(highAmountCount, roundAmountCount, frequentTxCount, highRiskCount)

    val chart = new CategoryChartBuilder()
      .width(800)
      .height(600)
      .title("Suspicious Transaction Patterns")
      .xAxisTitle("Detection Rule")
      .yAxisTitle("Count")
      .build()

    chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart.addSeries("Transactions", categories.asJava, values.map(_.asInstanceOf[Number]).asJava)

    BitmapEncoder.saveBitmap(chart, "dashboard/status_chart1", BitmapEncoder.BitmapFormat.PNG)
    println("Chart saved to dashboard/status_chart1.png")
  }
}

