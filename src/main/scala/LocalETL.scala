
//import org.apache.spark.sql.{SparkSession, Row}
//import java.sql.DriverManager
//
//object LocalETL {
//  def main(args: Array[String]): Unit = {
//    val spark = SparkSession.builder
//      .appName("Local AML Detection")
//      .master("local[*]")
//      .getOrCreate()
//
//    import spark.implicits._
//
//    // Read CSV file
//    val df = spark.read.option("header", "true").csv("data/transactions.csv")
//
//    // Convert amount column to Double
//    val dfTyped = df.withColumn("amount", df("amount").cast("double"))
//
//    // Filter suspicious transactions (amount > 100000)
//    val suspicious = dfTyped.filter($"amount" > 100000)
//
//    // Write to PostgreSQL
//    suspicious.foreachPartition { partition: Iterator[Row] =>
//      val conn = DriverManager.getConnection(
//        "jdbc:postgresql://localhost:5432/aml_db", "postgres", "root")
//      val stmt = conn.prepareStatement(
//        "INSERT INTO suspicious_transactions (transaction_id, amount, status) VALUES (?, ?, ?)")
//
//      partition.foreach { row =>
//        stmt.setString(1, row.getAs[String]("transaction_id"))
//        stmt.setDouble(2, row.getAs[Double]("amount"))
//        stmt.setString(3, "suspicious")
//        stmt.executeUpdate()
//      }
//      stmt.close()
//      conn.close()
//    }
//
//    spark.stop()
//  }
//}

import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.sql.functions._
import java.sql.DriverManager

object LocalETL {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("Local AML Detection")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Read CSV file
    val df = spark.read.option("header", "true").csv("data/transactions.csv")
      .withColumn("amount", $"amount".cast("double"))
      .withColumn("timestamp", to_timestamp($"timestamp"))

    // Define high-risk countries
    val highRiskCountries = Seq("Nigeria", "Panama", "Russia", "North Korea")

    // Rule 1: Amount > ₹100,000
    val rule1 = df.filter($"amount" > 100000)

    // Rule 2: Round-number transactions
    val rule2 = df.filter($"amount" % 10000 === 0)

    // Rule 3: Multiple transactions from same account within 10 minutes
    val windowSpec = org.apache.spark.sql.expressions.Window
      .partitionBy("account_id")
      .orderBy("timestamp")
    val dfWithLag = df.withColumn("prev_timestamp", lag("timestamp", 1).over(windowSpec))
      .withColumn("time_diff", unix_timestamp($"timestamp") - unix_timestamp($"prev_timestamp"))
    val rule3 = dfWithLag.filter($"time_diff".isNotNull && $"time_diff" <= 600)

    // Rule 4: Transactions to high-risk countries
    val rule4 = df.filter($"country".isin(highRiskCountries: _*))

    // Select matching columns before union
    val baseCols = Seq("transaction_id", "account_id", "amount", "country", "timestamp")
    val rule1Clean = rule1.select(baseCols.map(col): _*)
    val rule2Clean = rule2.select(baseCols.map(col): _*)
    val rule3Clean = rule3.select(baseCols.map(col): _*)
    val rule4Clean = rule4.select(baseCols.map(col): _*)

    // Union all suspicious transactions
    val suspicious = rule1Clean
      .union(rule2Clean)
      .union(rule3Clean)
      .union(rule4Clean)
      .dropDuplicates("transaction_id")

    // Write to PostgreSQL
    suspicious.foreachPartition { partition: Iterator[Row] =>
      val conn = DriverManager.getConnection(
        "jdbc:postgresql://localhost:5432/aml_db", "postgres", "root")
      val stmt = conn.prepareStatement(
        "INSERT INTO suspicious_transactions (transaction_id, amount, status) VALUES (?, ?, ?)")

      partition.foreach { row =>
        stmt.setString(1, row.getAs[String]("transaction_id"))
        stmt.setDouble(2, row.getAs[Double]("amount"))
        stmt.setString(3, "suspicious")
        stmt.executeUpdate()
      }

      stmt.close()
      conn.close()
    }

    spark.stop()
  }
}



