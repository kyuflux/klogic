import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must beSome.which (status(_) == NOT_FOUND)
    }

    "enqueue a customr and call them later" in new WithApplication{
	val viejito = Json.obj("name" -> "javier",
		 "lname" -> "salcedo",
		"doc" -> "17547608")

      val enqueue = route(new FakeRequest(POST, "/mercantil/castellana/3eraedad", 
		Headers(("Content-Type","application/json")),viejito.toString)).get.
		onSuccess{
			  case res => {
			  res must equalTo(ok)
			  val next = route(FakeRequest(GET, "/mercantil/castellana/3eraedad")).get
			  contentAsString(next) must contain ("17547608")
			}
		}

         }
  }
}
