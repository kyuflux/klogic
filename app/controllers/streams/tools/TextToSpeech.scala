package controllers.streams.tools

import play.api.Play.current
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import scala.concurrent.Future
import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.codec.binary._
import play.api.libs.iteratee._


object TextToSpeech{
	def apply(lang:String) = new TextToSpeech(lang)
}
import TextToSpeech._
class TextToSpeech(lang:String) {
	val consume = Iteratee.consume[Array[Byte]]()
	def audioToBase64(w:String):Future[String] = {
	WS.url("http://translate.google.com/translate_tts")
		.withQueryString("tl" -> lang)
		.withQueryString("q" -> w).getStream().flatMap{
			case (headers,body) =>{
				body(consume) flatMap {
					i => i.run map ( ba =>	Base64.encodeBase64String(ba))
				}
			}
		}
	}
}
