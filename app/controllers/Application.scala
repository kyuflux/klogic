package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Configuration
import controllers.streams.Queue

class Application extends Controller {
  implicit val klHost = current.configuration.getString("kcller.url").getOrElse("none") 
  implicit val redisHost = current.configuration.getString("redis.host").getOrElse("none")
  val qs:Queue = Queue(redisHost,klHost)
  
  def index = Action {
    Ok(views.html.index("klogic"))
  }
 
  def enqueue(org:String, branch:String, queue:String) = Action { implicit request =>
	val channel = s"$org:$branch:$queue"
	request.body.asJson match {
		case Some(obj) => { qs.in ! Queue.Enqueue(channel,obj.as[Queue.Customer])
					Results.Accepted }
		case None => Results.NotAcceptable
	}
  }
  def next(org:String, branch:String, queue:String) = Action { implicit request =>
	qs.out ! Queue.Dequeue(org,branch,queue)
	Results.Accepted
  }
}


