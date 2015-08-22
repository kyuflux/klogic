import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.mvc._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import controllers.streams.tools.TextToSpeech._
import controllers.streams.tools.TextToSpeech
import akka.testkit._
import scala.concurrent.duration._
import akka.stream.testkit.scaladsl._
/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class TextToSpeechSpec extends Specification {

  "TextToSpeech" should {

    "convert a stream in audio base65 string" in new WithApplication{
	val resultFuture:Future[String] = TextToSpeech("es").audioToBase64("hola")
	val result:String = Await.result(resultFuture, 1000.millis)
	(result.length() > 0) must beTrue 

     }
}
}

