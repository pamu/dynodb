package database

import schema.Entity
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by pnagarjuna on 01/06/15.
 */
object DAO {
  def entry(entity: Entity): Future[Int] = {
    get(entity.key).flatMap { innerEntity =>
      val q = for(entity <- Tables.entities.filter(_.key === innerEntity.key)) yield entity.value
      val p = q.update(entity.value)
      DB.db.get.run(p.transactionally)
    }.recoverWith { case throwable =>
      val q = Tables.entities += entity
      DB.db.get.run(q.transactionally)
    }
  }
  def get(key: String): Future[Entity] = {
    val q = for(entity <- Tables.entities.filter(_.key === key)) yield entity
    DB.db.get.run(q.result).map(_ head)
  }
  def evict(key: String): Future[Int] = {
    val q = for(entity <- Tables.entities.filter(_.key === key)) yield entity
    DB.db.get.run(q.delete)
  }
  def init: Future[Unit] = {
    DB.db.get.run(DBIO.seq(Tables.entities.schema.create))
  }
}
