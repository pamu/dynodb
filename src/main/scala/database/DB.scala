package database

import slick.driver.MySQLDriver.api._
/**
 * Created by pnagarjuna on 01/06/15.
 */
object DB {
  def apply(num: Int): Unit = {
    db = Some(
      Database.forURL(
      url = s"jdbc:mysql://localhost/dynodb$num",
      driver = "com.mysql.jdbc.Driver",
      user="root",
      password="root")
    )
  }
  var db: Option[Database] = None
}
