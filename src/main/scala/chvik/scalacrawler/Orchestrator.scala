package chvik.scalacrawler

import java.net.URL

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}

import scala.collection.immutable.{HashMap, Queue}

object Orchestrator {
  sealed trait Command
  final case class Feed(targets: Seq[CrawlTarget]) extends Command

  final case class CrawlTarget(url: String)
  type HostWorkers = Map[String, ActorRef[Worker.Command]]
  private case class State(queue: Queue[CrawlTarget],
                           hostWorkers: HostWorkers)

  val behavior: Behavior[Command] = behaviorCreator(State(
    queue = Queue(),
    hostWorkers = HashMap())
  )

  private def behaviorCreator(state: State): Behavior[Command] = {
    val State(queue, hostWorkers) = state
    Actor.immutable[Command] { (ctx, msg) =>
      msg match {
        case Feed(targets) =>
          val newQueue = queue ++ targets
          Logging.debug(s"new queue ${newQueue}", ctx)
          val (nextTarget, dequeued) = newQueue.dequeue
          val newHostWorkers = feedWorkers(ctx, nextTarget, hostWorkers)
          behaviorCreator(state.copy(queue = dequeued, hostWorkers = newHostWorkers))
      }
    }
  }

  private def feedWorkers(ctx: ActorContext[Command], target: CrawlTarget, hostWorkers: HostWorkers): HostWorkers = {
    val host = (new URL(target.url)).getHost()
    val hostWorker = hostWorkers.get(host).getOrElse(startWorker(ctx, host))
    hostWorker ! Worker.Feed(target.url)
    hostWorkers + (host -> hostWorker)
  }

  private def startWorker(ctx: ActorContext[Command], host: String): ActorRef[Worker.Command] = {
    Logging.debug(s"starting new worker for ${host}", ctx)
    ctx.spawn(Worker.behavior, "worker " + host)
  }
}
