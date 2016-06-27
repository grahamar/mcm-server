package io.grhodes.mcm.server.apn

import java.util.Date

import com.gilt.gfc.logging.Loggable
import io.circe.Json
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.{HttpService, MediaType}

import scala.concurrent.ExecutionContext

object ApnService extends Loggable {
  def apnService(implicit executionContext: ExecutionContext) = HttpService {
    case req @ POST -> Root / "3" / "device" / token =>
      val r = scala.util.Random
      val n = r.nextInt(50)

//      if(n == 7) {
//        info(s"[IOS] - [$token] - Got notification -> Responding with BadDeviceToken")
//        BadRequest(Json.obj("reason" -> Json.fromString("BadDeviceToken")))
//      } else if(n == 5) {
//        info(s"[IOS] - [$token] - Got notification -> Responding with Unregistered")
//        Gone(Json.obj("reason" -> Json.fromString("Unregistered"), "timestamp" -> Json.fromLong(new Date().getTime)))
//      } else {
        info(s"[IOS] - [$token] - Got notification -> Responding with OK")
        Ok().withType(MediaType.`application/json`)
//      }
  }
}
