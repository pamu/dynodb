package database

import schema.Entities
import slick.driver.MySQLDriver.api._

/**
 * Created by pnagarjuna on 01/06/15.
 */
object Tables {
  val entities = TableQuery[Entities]
}
