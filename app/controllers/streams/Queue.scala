package controllers.streams

import play.api._
import libs.json._
import scredis._
import akka.actor._
import libs.concurrent.Akka
import Play.current
import scala.util.{ Success, Failure }
import scala.concurrent._
import ExecutionContext.Implicits.global
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Broadcast,Source, Flow, Sink, FlowGraph}
import akka.stream.OverflowStrategy.dropHead
import libs.ws.WS
import Queue._
import tools.TextToSpeech

class Queue(redisHost:String, klHost:String){
	implicit val system = Akka.system
	val client = Client(host=redisHost)
	implicit val materializer = ActorMaterializer()
	val inSrc = Source.actorRef[Enqueue](999,dropHead)
	import Queue.CustomerWriter._
	val inEnd = Sink.foreachParallel[Enqueue](10){em => 
		val channel = s"${em.org}:${em.branch}:${em.queue}"
	client.rPush[Customer](channel,em.data) }
	implicit val klurl = klHost
	val tts = Flow[Enqueue].mapAsync(10)(toTts)
	val pipe = Flow(){ implicit b =>
  		import FlowGraph.Implicits._
		val bcast = b.add(Broadcast[Enqueue](2))
		val back = Sink.foreachParallel[(String,JsValue)](10)
			{o => WS.url(o._1).post(o._2)} 
		bcast ~> tts ~> back
		(bcast.in, bcast.out(1))
		
	}	
	//val in = Flow[Enqueue].to(inEnd).runWith(inSrc)
	val in = pipe.runWith(inSrc,inEnd)._1
	import Queue.CustomerReader._
	val pop = Flow[Dequeue].mapAsync(10){ dq =>
			client.lPop[Customer](s"${dq.org}:${dq.branch}:${dq.queue}") map { cm =>
				Some((s"$klHost/${dq.org}/${dq.branch}",cm.get))  
			}
	}
	val sourceOut:Source[Some[(String,Customer)],ActorRef] = 
			Source.actorRef[Dequeue](999,dropHead).via(pop)

	val outEnd = Sink.foreach[Some[(String,Customer)]](
		o => WS.url(o.get._1).post(Json.toJson(o.get._2)))

	val out = outEnd.runWith(sourceOut)

}

import scredis.serialization._
object Queue{
	def apply(redisHost:String, klHost:String):Queue = new Queue(redisHost,klHost)
	implicit val cusFmt:Format[Customer] = Json.format[Customer]
	implicit object CustomerWriter extends Writer[Customer]{
	override def writeImpl(c:Customer):Array[Byte] = {
	   	Json.stringify(Json.toJson(c)).getBytes
	  }
	}
	implicit object CustomerReader extends Reader[Customer]{
  		override def readImpl(cus:Array[Byte]): Customer = {
  			Json.parse(new String(cus)).as[Customer]
		}
	}
	def toTts(eq:Enqueue)(implicit klurl:String):Future[(String,JsValue)] = { 
		val url = s"$klurl/${eq.org}/${eq.branch}" 
		val name = eq.data.name.trim+"_"+eq.data.lname.trim
		TextToSpeech("es").audioToBase64(eq.data.name+" "+eq.data.lname).
		map{ s => (url,Json.toJson(Map("name"->name,"audio"->s)))}
	}
	
	case class Customer(name:String, lname:String, doc:String, audio:Option[String] = None)
	case class Enqueue(org:String, branch:String, queue:String, data:Customer)
	case class Dequeue(org:String, branch:String, queue:String)
}

//tts
//eq =>TextToSpeech("es").audioToBase64(eq.data.name+" "+eq.data.lname).
//	map{ s => eq.copy(data=eq.data.copy(audio=Some(s)))}
	
