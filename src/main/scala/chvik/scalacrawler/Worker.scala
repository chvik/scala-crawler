package chvik.scalacrawler

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.typed.Behavior
import akka.typed.scaladsl.{Actor, ActorContext}

import scala.collection.immutable.Queue
import scala.util.{Failure, Success}

object Worker {
  sealed trait Command
  final case class Feed(url: String) extends Command
  final case class Ready() extends Command

  val behavior: Behavior[Command] = inactive()

  private def inactive(): Behavior[Command] = {
    Actor.immutable[Command] { (ctx, msg) =>
      msg match {
        case Feed(url) =>
          Logging.debug("feed url", ctx)
          val newQueue = processNext(Queue(url), ctx)
          waitingForResult(newQueue)
        case Ready() =>
          Logging.debug("we were already ready", ctx)
          Actor.same
      }
    }
  }

  private def waitingForResult(queue: Queue[String] = Queue()): Behavior[Command] = {
    Actor.immutable[Command] { (ctx, msg) =>
      msg match {
        case Ready() =>
          if (queue.isEmpty) {
            inactive()
          } else {
            val newQueue = processNext(queue, ctx)
            waitingForResult(newQueue)
          }
        case Feed(url) =>
          Logging.debug(s"I'm busy, deferring ${url}", ctx)
          waitingForResult(queue.enqueue(url))
      }
    }
  }

  private def processNext(queue: Queue[String], ctx: ActorContext[Command]): Queue[String] = {
    if (queue.isEmpty) {
      Logging.debug("everything is processed", ctx)
      queue
    } else {
      val (next, newQueue) = queue.dequeue
      Logging.debug(s"processing ${next}, remaining ${newQueue}", ctx)
      processUrl(next, ctx)
      newQueue
    }
  }

  private def processUrl(url: String, ctx: ActorContext[Command]): Unit = {
    val resultFuture = UrlProcessor.processUrl(url, ctx.executionContext)
    implicit val ec = ctx.executionContext
    resultFuture.onComplete {
      case Success(res) =>
        Logging.debug(s"${url} DONE ${res}", ctx)
        ctx.self ! Ready()
      case Failure(_) =>
        Logging.debug(s"${url} ERROR", ctx)
        ctx.self ! Ready()
    }
  }
}
