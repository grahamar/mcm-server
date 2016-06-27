package io.grhodes.mcm.server

import io.grhodes.mcm.server.apn.ApnService
import io.grhodes.mcm.server.gcm.GcmService
import org.http4s._
import org.http4s.dsl.{->, /, Root, _}
import org.http4s.server.Router

import scala.concurrent.ExecutionContext

object McmService {
  def service(implicit executionContext: ExecutionContext = ExecutionContext.global): HttpService = Router(
    "/" -> ApnService.apnService,
    "/gcm/" -> GcmService.gcmService,
    "/mcm" -> mcmService
  )

  def mcmService(implicit executionContext: ExecutionContext): HttpService = HttpService {
    case GET -> Root => Ok()
  }
}
