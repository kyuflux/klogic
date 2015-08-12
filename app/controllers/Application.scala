package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import scredis._
import akka.actor._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.util.{ Success, Failure }
import scala.concurrent.ExecutionContext.Implicits.global
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Flow, Sink}
import akka.stream.OverflowStrategy.dropHead
import play.api.Configuration
import play.api.libs.ws.WS
import akka.stream.ActorMaterializer

class Application extends Controller {
  implicit val system = Akka.system
  val client = Client(host="10.0.0.99")
  val wsUrl = current.configuration.getString("kcontroller.url").getOrElse("none")
  implicit val materializer = ActorMaterializer()
  val inSrc = Source.actorRef[Enqueue](999,dropHead)
  val inEnd = Sink.foreach[Enqueue](em => client.rPush(em.channel,em.data) )
  val inRef = Flow[Enqueue].to(inEnd).runWith(inSrc)

  val outSrc = Source.actorRef[Dequeue](999,dropHead)
  val outEnd = Sink.foreach[Dequeue]{ dq =>
		client.get(s"${dq.org}:${dq.branch}:${dq.queue}") onComplete {
			case Success(Some(cm)) => WS.url(s"$wsUrl/${dq.org}/${dq.branch}").post(cm)
			case Success(None) => ()
			case Failure(e) => ()
			}
		 }
  val outRef = Flow[Dequeue].to(outEnd).runWith(outSrc)

  def index = Action {
    Ok(views.html.index("klogic2"))
  }
 
  def enqueue(org:String, branch:String, queue:String) = Action { implicit request =>
	val data = request.body.asJson.get.toString
	val channel = s"$org:$branch:$queue"
	   inRef ! Enqueue(channel,data)
	Ok("Ok")
  }
  def next(org:String, branch:String, queue:String) = Action { implicit request =>
	outRef ! Dequeue(org,branch,queue)
	Ok("Ok")
  }
}

case class Enqueue(channel:String, data:String)
case class Dequeue(org:String, branch:String, queue:String)
