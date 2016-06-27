package io.grhodes.mcm.server.gcm

object XmppBuilder {

  private lazy val Store = new InMemoryGCMMessageStoreImpl()
  private lazy val Server = new MockGCMXmppServer(new ServiceContext(new MessageRelayManager(Store), Store))

  def start() = {
    Server.run()
  }

  def shutdown() = {
    Server.XmppServer.stop()
  }

  def addUser(usr: User) = {
    Server.addUser(usr)
  }

}
