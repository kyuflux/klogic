import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import controllers.streams._
/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class QueueSpec extends Specification {

  "Queue" should {

    "Enqueue and dequeue customer" in new WithApplication{
    val q = Queue("10.0.0.99","") 
	q.in ! Queue.Customer("Javier","Salcedo","17547608")

	"11" must haveSize(2) 
    }
  }
}
