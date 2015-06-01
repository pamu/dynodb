package database

import schema.Entity
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future

/**
 * Created by pnagarjuna on 01/06/15.
 */
object DAO {
  def entry(entity: Entity): Future[Int] = {
    get(entity.key).flatMap { innerEntity =>
      val q = for(entity <- Tables.entities.filter(_.key === innerEntity.key)) yield entity.value
      val p = q.update(entity.value)
      DB.db.run(p.transactionally)
    }.recoverWith { case throwable =>
      val q = Tables.entities += entity
      DB.db.run(q.transactionally)
    }
  }
  def get(key: String): Future[Entity] = {
    val q = for(entity <- Tables.entities.filter(_.key === key)) yield entity
    DB.db.run(q.result).map(_ head)
  }
  def evict(key: String): Future[Int] = {
    val q = for(entity <- Tables.entities.filter(_.key === key)) yield entity
    DB.db.run(q.delete)
  }
}
