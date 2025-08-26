# MoneyLaundaryDetectionBasic-v1

💸**Money Laundering Detection System**
This project is a simple batch-mode data pipeline built using Scala, Apache Spark, and PostgreSQL, designed to detect potentially suspicious financial transactions from a local CSV file. It also includes a basic dashboard that visualizes suspicious transactions using Spark XChart, with graphs saved as images.

🔧 **Tech Stack**

Scala (developed in IntelliJ IDEA),
Apache Spark (Batch Processing),
PostgreSQL (for storing suspicious transactions),
Spark XChart (for dashboard visualization),
Local CSV files (as data source).

📊 **Features**

Loads transaction data from a local CSV file.
Applies business rules to detect suspicious transactions:
Transactions over ₹100,000.
Round-number transactions.
Transactions from high-risk countries.
Frequent transactions from the same account in short intervals.
Stores flagged transactions in a PostgreSQL database.
Generates visual graphs of suspicious activity using Spark XChart.
Saves dashboard graphs as image files for reporting.
