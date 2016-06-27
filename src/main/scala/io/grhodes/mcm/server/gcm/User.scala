package io.grhodes.mcm.server.gcm

import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe._

case class User(name: String, password: String)
object User {
  implicit val loginEntityDecoder: EntityDecoder[User] = jsonOf[User]
}