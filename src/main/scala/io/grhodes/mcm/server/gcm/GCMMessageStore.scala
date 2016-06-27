package io.grhodes.mcm.server.gcm

trait GCMMessageStore {
  def shutdown(): Unit
  def storeMessage(regId: String, message: String): Boolean
}
