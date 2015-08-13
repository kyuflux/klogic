package controllers.streams

import play.api._
import play.api.libs.json._
import scredis._
import akka.actor._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.util.{ Success, Failure }
import scala.concurrent._
import ExecutionContext.Implicits.global
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Flow, Sink}
import akka.stream.OverflowStrategy.dropHead
import play.api.libs.ws.WS
import akka.stream.ActorMaterializer
import Queue._

class Queue(redisHost:String, klHost:String){
implicit val system = Akka.system
	val client = Client(host=redisHost)
	implicit val materializer = ActorMaterializer()
	val inSrc = Source.actorRef[Enqueue](999,dropHead)
	import Queue.CustomerWriter._
	val inEnd = Sink.foreach[Enqueue](em => client.rPush[Customer](em.channel,em.data) )
	
	val in = Flow[Enqueue].to(inEnd).runWith(inSrc)
	val outSrc = Source.actorRef[Dequeue](999,dropHead)
	import Queue.CustomerReader._
	val lpop = Flow[Dequeue].mapAsync(10){ dq =>
			Future{
				client.lPop[String](s"${dq.org}:${dq.branch}:${dq.queue}") map { cm =>
					Some((s"$klHost/${dq.org}/${dq.branch}",cm))  
				}
			}
		}
	val filter = Flow[Some[(String,Customer)]].filter( o => !o.isEmpty   )
	val outEnd = Sink.foreach[Some[(String,Customer)]](o => WS.url(o._1).post(o._2))
	val out = lpop.join(filter).to(outEnd).runWith(outSrc)

}

import scredis.serialization._
object Queue{
	def apply(redisHost:String, klHost:String):Queue = new Queue(redisHost,klHost)
	implicit val cusFmt = Json.format[Customer]
	
	implicit object CustomerWriter extends Writer[Customer]{
	override def writeImpl(c:Customer):Array[Byte] = {
	   	Json.toJson(c).toString.getBytes
	  }
	}
	implicit object CustomerReader extends Reader[Customer]{
  		override def readImpl(cus:Array[Byte]): Customer = {
  			Json.parse(new String(cus)).as[Customer]
		}
	}
	case class Customer(name:String, lname:String, doc:String, audio:Option[String] = None)
	case class Enqueue(channel:String, data:Customer)
	case class Dequeue(org:String, branch:String, queue:String)
}
