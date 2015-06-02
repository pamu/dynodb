package storage

import akka.actor.{RootActorPath, RelativeActorPath, Actor, ActorLogging}
import akka.cluster.ClusterEvent._
import akka.cluster.{MemberStatus, Cluster, UniqueAddress}
import client.Client.{SuccessMessage, FailureMessage, Data}

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

class Storage extends Actor with ActorLogging {
  var nodes = Set.empty[UniqueAddress]
  var cache = Map.empty[String, String]
  val cluster = Cluster(context.system)
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberEvent], classOf[UnreachableMember], classOf[ReachabilityEvent])
  override def postStop(): Unit = cluster.unsubscribe(self)
  import Storage._
  override def receive = {
    case Get(key) =>
      log.info("{} message", Get(key))
      if (cache contains key)
        sender() ! Data(key, cache(key))
      else sender() ! FailureMessage(key, "Key does not exist.")
    case Entry(key, value) =>
      log.info("{} message", Entry(key, value))
      log.info(cache.mkString("\n", "\n", "\n"))
      log.info("Replicating ...")
      nodes.filter(_ != cluster.selfUniqueAddress).map(ua => context.actorSelection(RootActorPath(ua.address) / ("/user/Storage" match {case RelativeActorPath(elements) => elements})) ! ReplicaEntry(key, value))
      log.info("Replication Done .")
      cache += (key -> value)
      sender() ! SuccessMessage(key, "Successfully added.")
    case Evict(key) =>
      log.info("{} message", Evict(key))
      if (cache contains key) {
        sender() ! SuccessMessage(key, "Successfully removed.")
      } else {
        sender() ! FailureMessage(key, "Key does not exist.")
      }
    case ReplicaEntry(key, value) =>
      log.info("{} message", ReplicaEntry(key, value))
      cache += (key -> value)

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
