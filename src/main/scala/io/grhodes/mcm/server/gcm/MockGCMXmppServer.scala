package io.grhodes.mcm.server.gcm

import java.io.File

import com.gilt.gfc.logging.Loggable
import com.typesafe.config.ConfigFactory
import org.apache.vysper.mina.TCPEndpoint
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry
import org.apache.vysper.xmpp.addressing.EntityImpl
import org.apache.vysper.xmpp.authorization.AccountManagement
import org.apache.vysper.xmpp.server.XMPPServer

import scala.util.control.NonFatal

class MockGCMXmppServer(serviceContext: ServiceContext) extends Loggable {

  val ServerConfig = ConfigFactory.load().getConfig("io.grhodes.mcm-server")
  val XmppDomain = ServerConfig.getString("xmpp.domain")
  val XmppPort = ServerConfig.getInt("xmpp.port")
  val GcmCertFile = ServerConfig.getString("xmpp.tls-cert.path")
  val KeyStorePassword = ServerConfig.getString("xmpp.tls-cert.password")
  val ProviderRegistry = new MemoryStorageProviderRegistry()
  val AccountManagement = ProviderRegistry.retrieve(classOf[AccountManagement]).asInstanceOf[AccountManagement]
  val XmppServer = {
    val s = new XMPPServer(XmppDomain)
    val tcpEndpoint = new TCPEndpoint()
    tcpEndpoint.setPort(XmppPort)
    s.addEndpoint(tcpEndpoint)
    s.setStorageProviderRegistry(ProviderRegistry)
    s.setTLSCertificateInfo(new File(GcmCertFile), KeyStorePassword)
    s
  }

  def run() = {
    XmppServer.start()
    XmppServer.addModule(new GCMModule(serviceContext))

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        try {
          XmppServer.stop()
          serviceContext.messageRelayManager.shutdown()
          info("Mock GCM XMPP server is shut down.");
        } catch {
          case NonFatal(e) => error("Caught an exception during shutdown", e);
        }
      }
    })
    // Default GCM mock user
    AccountManagement.addUser(EntityImpl.parse(s"888888888888@$XmppDomain"), "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
  }

  def addUser(usr: User) = {
    AccountManagement.addUser(EntityImpl.parse(s"${usr.name}@$XmppDomain"), usr.password)
  }

}
