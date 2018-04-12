package chvik.scalacrawler

import akka.typed.{ActorRef, ActorSystem}
import akka.typed.scaladsl.Actor
;

object Main extends App {

  val root = Actor.deferred[Nothing] { ctx =>
    val orchestrator: ActorRef[Orchestrator.Command] = ctx.spawn(Orchestrator.behavior, "orchestrator")
    orchestrator ! Orchestrator.Feed(Seq(Orchestrator.CrawlTarget("http://index.hu/")))
    orchestrator ! Orchestrator.Feed(Seq(Orchestrator.CrawlTarget("http://444.hu/")))
    orchestrator ! Orchestrator.Feed(Seq(Orchestrator.CrawlTarget("http://444.hu/1/")))
    orchestrator ! Orchestrator.Feed(Seq(Orchestrator.CrawlTarget("http://444.hu/2/")))
    Actor.empty
  }

  val system = ActorSystem[Nothing](root, "ScalaCrawler")

}
