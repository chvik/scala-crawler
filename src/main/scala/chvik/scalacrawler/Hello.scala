package chvik.scalacrawler

import akka.typed.Behavior
import akka.typed.scaladsl.Actor

object Hello {
  sealed trait Command
  case object Greet extends Command
  final case class WhoToGreet(who: String) extends Command

  val helloBehavior: Behavior[Command] = behaviorCreator(greeting = "hello")

  private def behaviorCreator(greeting: String): Behavior[Command] = {
    Actor.immutable[Command] { (ctx, msg) =>
      msg match {
        case Greet =>
          println(greeting)
          Actor.same
        case WhoToGreet(who) =>
          behaviorCreator(s"hello $who")
      }
    }
  }
}
