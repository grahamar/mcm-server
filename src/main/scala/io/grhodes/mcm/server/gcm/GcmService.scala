package io.grhodes.mcm.server.gcm

import org.http4s.{HttpService, MediaType}
import org.http4s.dsl._

import scala.concurrent.ExecutionContext

object GcmService {
  def gcmService(implicit executionContext: ExecutionContext): HttpService = HttpService {
    case req @ POST -> Root / "user" =>
      req.decode[User] { data =>
        XmppBuilder.addUser(data)
        Ok().withType(MediaType.`application/json`)
      }
  }
}
