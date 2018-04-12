package chvik.scalacrawler

import scala.collection.JavaConverters._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.jsoup.parser.Parser

import scala.concurrent.{ExecutionContext, Future}

object UrlProcessor {

  final case class UrlProcessResult(links: Seq[String])

  implicit val untypedActorSystem = ActorSystem("http")
  implicit val materializer = ActorMaterializer()

  def processUrl(url: String, executionContext: ExecutionContext): Future[UrlProcessResult] = {
    implicit val ec = executionContext
    requestUrl(url, executionContext).flatMap {
      case entity =>
        if (entity.contentType.mediaType == MediaTypes.`text/html`) {
          entity.dataBytes.runFold(ByteString(""))(_ ++ _).map {
            bytes: ByteString =>
              UrlProcessResult(extractLinks(url, bytes.utf8String))
          }
        } else {
          Future {
            UrlProcessResult(Seq())
          }
        }
    } recoverWith {
      case _ =>
        Future {
          UrlProcessResult(Seq())
        }
    }
  }

  def extractLinks(baseUrl: String, html: String): Seq[String] = {
    val document = Parser.parse(html, baseUrl)
    val anchors = document.select("a")
    Logging.debug(s"anchors num ${anchors.size()}")
    anchors.asScala.map {
      e => e.attr("href")
    }.toSeq
  }

  def requestUrl(url: String, executionContext: ExecutionContext): Future[ResponseEntity] = {
    implicit val ec = executionContext
    val responseFuture = Http(untypedActorSystem).singleRequest(HttpRequest(uri = url))
    responseFuture.flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Future {
          entity
        }
      case HttpResponse(StatusCodes.Redirection(_), headers, _, _) =>
        val location = headers.filter(hdr => hdr.name == "Location").map(hdr => hdr.value).head
        Logging.debug(s"${url} redirects to ${location}")
        requestUrl(location, executionContext)
      case resp =>
        Logging.error(s"${url} failed: ${resp}")
        throw new Exception(s"${url}")
    }
  }
}
