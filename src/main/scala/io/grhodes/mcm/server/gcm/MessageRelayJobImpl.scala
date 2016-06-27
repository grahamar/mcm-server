package io.grhodes.mcm.server.gcm

import com.gilt.gfc.logging.Loggable
import org.apache.vysper.xml.fragment.XMLSemanticError
import org.apache.vysper.xmpp.addressing.Entity
import org.apache.vysper.xmpp.delivery.failure.{DeliveryException, IgnoreFailureStrategy}
import org.apache.vysper.xmpp.server.ServerRuntimeContext
import org.apache.vysper.xmpp.stanza.Stanza

class MessageRelayJobImpl(delay: Long, receiver: Entity, stanza: Stanza, serverRuntimeContext: ServerRuntimeContext, regId: Option[String] = None, incomingMessage: Option[String] = None) extends ScheduledJob(delay) with Loggable {
  override def execute(messageRelayManager: MessageRelayManager): Boolean = {
    try {
      incomingMessage.foreach { im =>
        debug(s"storing message into message store. regId=${regId.get}, incomingMessage=$im")
        messageRelayManager.gcmMessageStore.storeMessage(regId.get, im)
      }

      debug(s"relaying message: ${stanza.getInnerElements.get(0).getSingleInnerText.getText}")
      serverRuntimeContext.getStanzaRelay.relay(receiver, stanza, new IgnoreFailureStrategy())

      true
    } catch {
      case e @ (_: DeliveryException | _: XMLSemanticError) =>
        warn("failed to relay message", e)
        false
    }
  }
}
