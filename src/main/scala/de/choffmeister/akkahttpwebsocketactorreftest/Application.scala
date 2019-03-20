package de.choffmeister.akkahttpwebsocketactorreftest

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import akka.cluster.sharding.ClusterSharding
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

final case class Ping(pong: Any)

class ReplyActor extends Actor {
  override def receive = {
    case Ping(pong) => sender() ! pong
  }
}

object Application {
  implicit val logging = PrintlnLoggingAdapter()

  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem()
    implicit val executor = ExecutionContext.Implicits.global
    implicit val materializer = ActorMaterializer()

    val cluster = Cluster(actorSystem)
    cluster.join(cluster.selfAddress)

    val sharding = ClusterSharding(actorSystem)
    val reply = sharding.start(
      typeName = "reply",
      entityProps = Props(new ReplyActor),
      extractEntityId = {
        case msg => ("entity", msg)
      },
      extractShardId = {
        case _ => "shard"
      }
    )

    val routes = path("ws") {
      val sink = Flow[Message]
        .log("sink")
        .to(Sink.ignore)
      val source = Source
        .actorRef[Message](10, OverflowStrategy.dropHead)
        .mapMaterializedValue { ref =>
          actorSystem.scheduler.scheduleOnce(1.second, reply, Ping(PoisonPill))(executor, ref)
        }
        .log("source")
      val flow = Flow.fromSinkAndSourceCoupled(sink, source)
      handleWebSocketMessages(flow)
    }

    Http().bindAndHandle(routes, "0.0.0.0", 8080)
  }
}
