package storage

import java.sql.Timestamp
import java.util.Date

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.{MemberStatus, Cluster, UniqueAddress}
import client.Client.{SuccessMessage, FailureMessage, Data}
import database.{DB, DAO}
import schema.Entity
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by pnagarjuna on 01/06/15.
 */
object Storage {
  trait StorageMessage
  case class Get(key: String) extends StorageMessage
  case class Entry(key: String, value: String) extends StorageMessage
  case class Evict(key: String) extends StorageMessage

  trait ReplicaMessage
  case class ReplicaEntry(key: String, value: String) extends ReplicaMessage
  case class ReplicaEvict(key: String) extends ReplicaMessage
}

object InternalMessages {
  case class GetSuccess(entity: Entity, sender: ActorRef)
  case class GetFailure(key: String, msg: String, sender: ActorRef)
  case class EntrySuccess(key: String, status: Int, sender: ActorRef)
  case class EntryFailure(key: String, msg: String, sender: ActorRef)
  case class EvictSuccess(key: String, status: Int, sender: ActorRef)
  case class EvictFailure(key: String, msg: String, sender: ActorRef)
}

import akka.pattern.pipe

class Storage extends Actor with ActorLogging {
  var nodes = Set.empty[UniqueAddress]
  //var cache = Map.empty[String, String]
  val cluster = Cluster(context.system)
  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent], classOf[UnreachableMember], classOf[ReachabilityEvent])
    DB(cluster.selfUniqueAddress.address.port.get)
    DAO.init
  }
  override def postStop(): Unit = cluster.unsubscribe(self)
  import Storage._
  import InternalMessages._
  override def receive = {
    case Get(key) =>
      log.info("{} message", Get(key))
      /*
      if (cache contains key)
        sender() ! Data(key, cache(key))
      else sender() ! FailureMessage(key, "Key does not exist.")
      */
      val senderMan = sender()
      DAO.get(key).map(entity => GetSuccess(entity, senderMan))
        .recover{case th => GetFailure(key, "failed due to " + th.getMessage, senderMan)} pipeTo self
    case GetSuccess(entity, senderMan) =>
      senderMan ! Data(entity.key, entity.value)
    case GetFailure(key: String, msg: String, senderMan) =>
      senderMan ! FailureMessage(key, msg)
    case Entry(key, value) =>
      log.info("{} message", Entry(key, value))
      /*
      log.info(cache.mkString("\n", "\n", "\n"))
      log.info("Replicating ...")
      nodes.filter(_ != cluster.selfUniqueAddress).map(ua => context.actorSelection(RootActorPath(ua.address) / ("/user/Storage" match {case RelativeActorPath(elements) => elements})) ! ReplicaEntry(key, value))
      log.info("Replication Done .")
      cache += (key -> value)
      sender() ! SuccessMessage(key, "Successfully added.")
      */
      nodes.filter(_ != cluster.selfUniqueAddress)
        .map(ua => context.actorSelection(RootActorPath(ua.address)
        / ("/user/Storage" match {case RelativeActorPath(elements) => elements})) ! ReplicaEntry(key, value))

      val senderMan = sender()
      DAO.entry(Entity(key, value, new Timestamp((new Date().getTime))))
      .map { status => EntrySuccess(key, status, senderMan)}
      .recover {case th => EntryFailure(key, th.getMessage, senderMan)} pipeTo self
    case EntrySuccess(key, status, senderMan) =>
      senderMan ! SuccessMessage(key, "Success with status " + status)
    case EntryFailure(key, msg, senderMan) =>
      senderMan ! FailureMessage(key, msg)
    case Evict(key) =>
      log.info("{} message", Evict(key))
      /*
      if (cache contains key) {
        sender() ! SuccessMessage(key, "Successfully removed.")
      } else {
        sender() ! FailureMessage(key, "Key does not exist.")
      }
      */
      val senderMan = sender()
      DAO.evict(key).map {status => EvictSuccess(key, status, senderMan)}
      .recover {case th => EvictFailure(key, th.getMessage, senderMan)} pipeTo self
    case EvictSuccess(key, status, senderMan) =>
      senderMan ! SuccessMessage(key, "Evicted with status " + status)
    case EvictFailure(key, msg, senderMan) =>
      senderMan ! FailureMessage(key, msg)
    case ReplicaEntry(key, value) =>
      log.info("{} message", ReplicaEntry(key, value))
      //cache += (key -> value)
      DAO.entry(Entity(key, value, new Timestamp((new Date().getTime)))) pipeTo self

    case state: CurrentClusterState => state.members.collect {
      case member if member.hasRole("storage") && member.status == MemberStatus.Up =>
        log.info("Node with address {} is Up", member.address)
    }
    case MemberUp(member) => nodes += member.uniqueAddress
    case ReachableMember(member) => nodes += member.uniqueAddress
    case UnreachableMember(member) => nodes -= member.uniqueAddress
    case other: MemberEvent => nodes -= other.member.uniqueAddress
    case ex => log.info("Message {} of type {}", ex, ex getClass)
  }
}
