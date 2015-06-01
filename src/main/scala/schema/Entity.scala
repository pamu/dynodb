package schema

import java.sql.Timestamp

/**
 * Created by pnagarjuna on 01/06/15.
 */
case class Entity(key: String, value: String, timestamp: Timestamp, id: Option[Long] = None)