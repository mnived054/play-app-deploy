package controllers

import javax.inject._
import play.api._
import play.api.libs.json.Json
import play.api.mvc._

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  // Your secret key from Meta (replace with your actual key)
  private val SECRET_KEY = "052a4dea87154d68e8567a9d25535ba8"
  private val SIGNATURE_HEADER = "X-Hub-Signature-256"
  private val HMAC_SHA256 = "HmacSHA256"

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
  def getQuery(name:String) = Action {
    implicit request :Request[AnyContent] =>
      Ok(name)
  }
  // Helper to calculate HMAC-SHA256
  private def calculateHmac(payload: String, secret: String): String = {
    val secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), HMAC_SHA256)
    val mac = Mac.getInstance(HMAC_SHA256)
    mac.init(secretKey)
    val hashBytes = mac.doFinal(payload.getBytes("UTF-8"))
    val cgh=hashBytes.map("%02x".format(_)).mkString // Convert to hex string
    println(s"The generated is $cgh")
    cgh
  }

  def validateLead() = Action.async(parse.raw) { request =>
    val rawBody = request.body.asBytes().map(_.utf8String).getOrElse("")

    val receivedSignatureOpt = request.headers.get(SIGNATURE_HEADER)
    println(s"The recieved is ${receivedSignatureOpt.get}")

    receivedSignatureOpt match {
      case Some(signature) if signature.startsWith("sha256=") =>
        val receivedHash = signature.stripPrefix("sha256=")
        val calculatedHash = calculateHmac(rawBody, SECRET_KEY)

        if (calculatedHash == receivedHash) {
          val jsonBody = Json.parse(rawBody)
          println(s"Valid lead received: $jsonBody")
          Future.successful(Ok("Lead validated"))
        } else {
          Future.successful(Unauthorized("Invalid signature"))
        }

      case _ =>
        Future.successful(BadRequest("Missing or invalid signature header"))
    }
  }
  def verifyToken: Action[AnyContent] = Action.async { request =>
    val parms = request.queryString.map{ case (k,v)=> k-> v.mkString}
    println(s"The params are $parms")
    val token = "12345"
    if(parms.get("hub.verify_token").contains(token)){
      Future.successful(Ok(parms.getOrElse("hub.challenge","abc")))
    }else{
      throw new Exception("No access Tken")
    }
  }
}
