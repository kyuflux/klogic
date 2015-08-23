import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import controllers.streams._
import akka.stream.scaladsl.{Source, Flow, Sink, FlowGraph}
import Queue._
import akka.testkit._
import akka.stream._
import scala.concurrent.duration._
import akka.stream.testkit.scaladsl._
import akka.stream.testkit.TestSubscriber._
import akka.pattern.pipe
import java.lang.Thread
/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class QueueSpec extends Specification {

  "Queue" should {

    "works without streams, just redis client" in new WithApplication{
	 val q = Queue("10.0.0.99","") 
    
	q.client.rPush[String]("google:california:oldies","javier")
	val future:Future[Option[String]] = q.client.lPop[String]("google:california:oldies")
     	val result:Option[String] = Await.result(future, 100.millis)
       	 result.getOrElse("no") must contain("javier") 
	}
    
    "Enqueue and dequeue customer from google:california:oldies queue" in new WithApplication{
	 val q = Queue("10.0.0.99","") 
	val javier = Customer("Javier","Salcedo","17547608")
	q.in ! Enqueue("google","california","oldies",javier)
	Thread.sleep(800)
	implicit val system = q.system
	import q.materializer	
	val probe = TestProbe()
	val source:Source[Some[(String, Customer)],Unit] = Source.single[Dequeue](
		Dequeue("google","california","oldies")).via(q.pop)
	source.runWith(Sink.head).pipeTo(probe.ref)
	probe.expectMsg(800.millis, Some(("/google/california",javier)))
 }
  }
}
