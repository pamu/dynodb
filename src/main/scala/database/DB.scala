package database

import slick.driver.MySQLDriver.api._
/**
 * Created by pnagarjuna on 01/06/15.
 */
object DB {
  lazy val db = Database.forURL(
    url = "jdbc:mysql://localhost/dynodb",
    driver = "com.mysql.jdbc.Driver",
    user="root",
    password="root")
}
