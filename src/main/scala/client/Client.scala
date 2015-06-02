package client

import akka.actor._
import akka.cluster.{MemberStatus, UniqueAddress, Cluster}
import akka.cluster.ClusterEvent._
import storage.Storage

/**
 * Created by pnagarjuna on 01/06/15.
 */
object Client {
  trait ClientMessage
  case class Data(key: String, value: String) extends ClientMessage
  case class SuccessMessage(key: String, msg: String) extends ClientMessage
  case class FailureMessage(key: String, msg: String) extends ClientMessage
}

class Client extends Actor with ActorLogging {
  import Client._
  import Storage._
  var nodes = Set.empty[UniqueAddress]
  val cluster = Cluster(context.system)
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp], classOf[UnreachableMember], classOf[ReachabilityEvent])
  override def postStop(): Unit = cluster.unsubscribe(self)
  override def receive = {
    case msg: StorageMessage =>
      if (nodes.size == 0) {
        log.info("Cannot send message no nodes registered.")
      } else {
        val addr = nodes.toIndexedSeq(0)
        val service = context.actorSelection(RootActorPath(addr.address) / ("user/Service" match {case RelativeActorPath(elements) => elements}))
        service ! msg
      }
    case msg: ClientMessage =>
      msg match {
        case Data(key, value) => log.info("Got Data {}", Data(key, value))
        case SuccessMessage(key, msg) => log.info("Message for {} {}", key, msg)
        case FailureMessage(key, msg) => log.info("Message for {} {}", key, msg)
      }
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

object Starter {
  val system = ActorSystem("ClusterSystem")
  val client = system.actorOf(Props[Client], "Client")
}
