package io.grhodes.mcm.server.gcm

import java.util

import com.google.common.cache.CacheBuilder
import com.typesafe.config.ConfigFactory

class InMemoryGCMMessageStoreImpl extends GCMMessageStore {
  private val ServerConfig = ConfigFactory.load().getConfig("io.grhodes.mcm-server.xmpp")

  private val StoredMessages = CacheBuilder.newBuilder().maximumSize(1000).build[String, String]()
  private val isActive = new util.concurrent.atomic.AtomicBoolean(ServerConfig.getBoolean("message.store.enabled"))

  override def shutdown(): Unit = {}

  override def storeMessage(regId: String, message: String): Boolean = {
    if(isActive.get()) {
      StoredMessages.put(regId, message)
      true
    } else {
      false
    }
  }
}
