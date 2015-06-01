package schema

import java.sql.Timestamp

import slick.driver.MySQLDriver.api._
/**
 * Created by pnagarjuna on 01/06/15.
 */
class Entities(tag: Tag) extends Table[Entity](tag, "entities"){
  def key = column[String]("key")
  def value = column[String]("value")
  def timestamp = column[Timestamp]("timestamp")
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def * = (key, value, timestamp, id.?) <> (Entity.tupled, Entity.unapply)
}
