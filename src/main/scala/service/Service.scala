package service

import akka.actor.{ActorSystem, Props, Actor, ActorLogging}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory
import storage.Storage

/**
 * Created by pnagarjuna on 01/06/15.
 */
class Service extends Actor with ActorLogging {
  val router = context.actorOf(FromConfig.props(Props[Storage]), name = "Router")
  import Storage._
  override def receive = {
    case Get(key) => router forward ConsistentHashableEnvelope(message = Get(key), hashKey = key)
    case Entry(key, value) => router forward  ConsistentHashableEnvelope(message = Entry(key, value), hashKey = key)
    case Evict(key) => router forward  ConsistentHashableEnvelope(message = Evict(key), hashKey = key)
    case ex => log.info("message {} of type {}", ex, ex.getClass)
  }
}

object Starter {
  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [storage]"))
    .withFallback(ConfigFactory.load("router"))
    val system = ActorSystem("ClusterSystem", config)
    system.actorOf(Props[Storage], "Storage")
    system.actorOf(Props[Service], "Service")
  }
}
