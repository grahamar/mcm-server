package io.grhodes.mcm.server

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import io.grhodes.mcm.server.apn.ApnService
import io.grhodes.mcm.server.gcm.XmppBuilder
import org.http4s.server.SSLSupport.StoreInfo
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.{Server, ServerApp}

import scalaz.concurrent.Task

object Main extends ServerApp {

  //System.setProperty("javax.net.debug", "all")
  System.setProperty("file.encoding", "UTF8")

  val ServerConfig = ConfigFactory.load().getConfig("io.grhodes.mcm-server")
  val KeyStoreConfig = ServerConfig.getConfig("apn.keystore")
  val KeyPath = Paths.get(KeyStoreConfig.getString("path")).toAbsolutePath.toString

  val ApnServer = BlazeBuilder.enableHttp2(true).withSSL(
      StoreInfo(KeyPath, KeyStoreConfig.getString("password")),
      keyManagerPassword = KeyStoreConfig.getString("manager-password"),
      trustStore = Some(StoreInfo(KeyPath, KeyStoreConfig.getString("password")))
    ).mountService(McmService.service, "/").bindHttp(ServerConfig.getInt("apn.port"))

  override def server(args: List[String]): Task[Server] = {
    XmppBuilder.start()
    ApnServer.start
  }

  override def shutdown(server: Server) = {
    XmppBuilder.shutdown()
    server.shutdown
  }

}
