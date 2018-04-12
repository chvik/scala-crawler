package chvik.scalacrawler

import akka.typed.scaladsl.ActorContext
import com.typesafe.scalalogging.Logger

object Logging {
  val logger = Logger("scalacrawler")

  sealed trait OptParam[+A] { def toOption: Option[A] }
  case class Param[+A](value: A) extends OptParam[A] { def toOption = Some(value) }
  case object NoParam extends OptParam[Nothing] { def toOption = None }

  object OptParam {
    implicit def any2optParam[A](x: A): OptParam[A] = Param(x)
  }

  def error(msg: String, ctx: OptParam[ActorContext[_]] = NoParam) = {
    logger.error(fmt(ctx, msg))
  }

  def info(msg: String, ctx: OptParam[ActorContext[_]] = NoParam) = {
    logger.info(fmt(ctx, msg))
  }

  def debug(msg: String, ctx: OptParam[ActorContext[_]] = NoParam) = {
    logger.debug(fmt(ctx, msg))
  }

  private def fmt(ctxOptParam: OptParam[ActorContext[_]], msg: String) = {
    ctxOptParam.toOption match {
      case Some(ctx) => s"${ctx.self} ${msg}"
      case None => msg
    }
  }
}
