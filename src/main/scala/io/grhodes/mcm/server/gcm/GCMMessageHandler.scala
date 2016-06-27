package io.grhodes.mcm.server.gcm

import java.util.concurrent.ThreadLocalRandom

import com.gilt.gfc.logging.Loggable
import com.typesafe.config.ConfigFactory
import io.circe.{Cursor, Json}
import org.apache.vysper.xmpp.addressing.Entity
import org.apache.vysper.xmpp.modules.core.base.handler.{DefaultMessageHandler, XMPPCoreStanzaHandler}
import org.apache.vysper.xmpp.protocol.NamespaceURIs
import org.apache.vysper.xmpp.server.{ServerRuntimeContext, SessionContext}
import org.apache.vysper.xmpp.stanza._

import scala.util.control.NonFatal
import scala.collection.JavaConverters._

object GCMMessageHandler {
  private val ServerConfig = ConfigFactory.load().getConfig("io.grhodes.mcm-server.xmpp")

  // Ack delay in milliseconds (randomized)
  val ACK_DELAY_MS = 100L
  // delivery receipt delay in milliseconds (randomized)
  val DELIVERY_RECEIPT_DELAY_MS = 200L

  val JSON_MESSAGE_TYPE = "message_type"
  val JSON_MESSAGE_ID = "message_id"
  val JSON_FROM = "from"
  val JSON_TO = "to"
  val JSON_RECEIPT = "receipt"
  val JSON_CATEGORY = "category"
  val JSON_ACK = "ack"
  val JSON_NACK = "nack"
  val JSON_CONTROL = "control"
  val JSON_CONTROL_TYPE = "control_type"
  val JSON_MESSAGE_STATUS = "message_status"
  val JSON_MESSAGE_STATUS_SENT_TO_DEVICE = "MESSAGE_SENT_TO_DEVICE"
  val JSON_ORIGINAL_MESSAGE_ID = "original_message_id"
  val JSON_REGISTRATION_ID = "REGISTRATION_ID"
  val JSON_DEVICE_REG_ID = "device_registration_id"
  val JSON_SENT_TIMESTAMP = "message_sent_timestamp"
  val JSON_ERROR = "error"
  val JSON_ERROR_BAD_REGISTRATION = "BAD_REGISTRATION"
  val JSON_ERROR_DESCRIPTION = "error_description"

  val badRegistrationTag = ServerConfig.getString("message.registration.id.bad_registration")
  val drainingTag = ServerConfig.getString("message.registration.id.draining")
}

class GCMMessageHandler(serviceContext: ServiceContext, moduleDomain: Entity) extends DefaultMessageHandler with Loggable {
  import GCMMessageHandler._

  override protected def verifyNamespace(stanza: Stanza): Boolean = {
    verifyInnerNamespace(stanza, Constants.NAMESPACE)
  }

  override protected def executeCore(coreStanza: XMPPCoreStanza, serverRuntimeContext: ServerRuntimeContext, isOutboundStanza: Boolean, sessionContext: SessionContext): Stanza = {
    val stanza = coreStanza.asInstanceOf[MessageStanza]

    // seems to be a Vysper bug. isOutbounceStanza really means the opposite
    if (isOutboundStanza)
      return executeMessageLogic(stanza, serverRuntimeContext, sessionContext)

    coreStanza
  }

  override protected def executeMessageLogic(stanza: MessageStanza, serverRuntimeContext: ServerRuntimeContext, sessionContext: SessionContext): Stanza = {
    try {
      val incomingMessage = stanza.getInnerElements.get(0).getSingleInnerText.getText
      val from = XMPPCoreStanzaHandler.extractSenderJID(stanza, sessionContext)

      val `type` = stanza.getMessageType
      if(MessageStanzaType.NORMAL.equals(`type`)) {
        val gcmMessage = GCMMessage.fromStanza(stanza)
        val jsonObject = gcmMessage.getJsonObject
        val jsonTo = jsonObject.get[String](JSON_TO).getOrElse(null)

        if(badRegistrationTag == jsonTo) {
          info(s"[ANDROID] - [$jsonTo] - Got notification -> Responding with Nack-BadRegistration")
          val outboundStanza = createNackBadRegIdMessageStanza(stanza, jsonObject)
          val messageRelayJob = new MessageRelayJobImpl(ThreadLocalRandom.current().nextLong(ACK_DELAY_MS) + 1, from, outboundStanza, serverRuntimeContext)
          serviceContext.messageRelayManager.addJob(messageRelayJob)
        } else {
          serviceContext.messageRelayManager.addJob(new MessageRelayJobImpl(
            ThreadLocalRandom.current().nextLong(ACK_DELAY_MS) + 1,
            from,
            createAckMessageStanza(stanza, jsonObject),
            serverRuntimeContext)
          )
//          serviceContext.messageRelayManager.addJob(new MessageRelayJobImpl(
//            ThreadLocalRandom.current().nextLong(DELIVERY_RECEIPT_DELAY_MS) + 1,
//            from,
//            createDeliveryReceiptMessageStanza(stanza, jsonObject),
//            serverRuntimeContext,
//            Some(jsonTo),
//            Some(incomingMessage)
//          ))

          // draining control message test
          if(drainingTag == jsonTo) {
            info(s"[ANDROID] - [$jsonTo] - Got notification -> Responding with Nack-Draining")
            serviceContext.messageRelayManager.addJob(new MessageRelayJobImpl(
              500,
              from,
              createDrainingMessageStanza(stanza, jsonObject),
              serverRuntimeContext,
              Some(jsonTo),
              Some(incomingMessage)
            ))
          } else {
            info(s"[ANDROID] - [$jsonTo] - Got notification -> Responding with Ack")
          }
        }
      }
    } catch {
      case NonFatal(e) => error("got exception: ", e)
    }
    null
  }

  /**
    * Create Ack Message
   <message id="">
     <gcm xmlns="google:mobile:data">
     {
       "from":"REGID",
       "message_id":"m-1366082849205"
       "message_type":"ack"
     }
     </gcm>
   </message>
    */
  private def createAckMessageStanza(original: Stanza, jsonObject: Cursor): Stanza = {
    val payload = Json.obj(
      JSON_MESSAGE_TYPE -> Json.fromString(JSON_ACK),
      JSON_FROM -> Json.fromString(jsonObject.get[String](JSON_TO).getOrElse("")),
      JSON_MESSAGE_ID -> Json.fromString(jsonObject.get[String](JSON_MESSAGE_ID).getOrElse(""))
    ).noSpaces

    // no from & to
    val builder = new StanzaBuilder("message")
    builder.addAttribute("id", Option(original.getAttributeValue("id")).getOrElse(""))
    val gcmMessageBuilder = new GCMMessage.Builder()
    gcmMessageBuilder.addText(payload)
    builder.addPreparedElement(gcmMessageBuilder.build())
    builder.build()
  }

  /**
    * Create Bad RegId Nack Message Stanza
    *
  <message>
     <gcm xmlns="google:mobile:data">
     {
       "message_type":"nack",
       "message_id":"msgId1",
       "from":"SomeInvalidRegistrationId",
       "error":"BAD_REGISTRATION",
       "error_description":"Invalid token on 'to' field: SomeInvalidRegistrationId"
     }
     </gcm>
   </message>
    */
  private def createNackBadRegIdMessageStanza(original: Stanza, jsonObject: Cursor): Stanza = {
    val payload = Json.obj(
      JSON_MESSAGE_TYPE -> Json.fromString(JSON_NACK),
      JSON_FROM -> Json.fromString(jsonObject.get[String](JSON_TO).getOrElse("")),
      JSON_MESSAGE_ID -> Json.fromString(jsonObject.get[String](JSON_MESSAGE_ID).getOrElse("")),
      JSON_ERROR -> Json.fromString(JSON_ERROR_BAD_REGISTRATION),
      JSON_ERROR_DESCRIPTION -> Json.fromString(s"Invalid token on 'to' field: ${jsonObject.get(JSON_TO).getOrElse("")}")
    ).noSpaces

    val builder = new StanzaBuilder("message")
    builder.addAttribute("id", Option(original.getAttributeValue("id")).getOrElse(""))
    val gcmMessageBuilder = new GCMMessage.Builder()
    gcmMessageBuilder.addText(payload)
    builder.addPreparedElement(gcmMessageBuilder.build())
    builder.build()
  }

  /**
    * Create Delivery Receipt
   <message id="">
     <gcm xmlns="google:mobile:data">
     {
       "category":"com.example.yourapp", // to know which app sent it
       "data":
       {
         “message_status":"MESSAGE_SENT_TO_DEVICE",
         “original_message_id”:”m-1366082849205”,
         “device_registration_id”: “REGISTRATION_ID”,
         "message_sent_timestamp": "1430277821658"
       },
       "message_id":"dr2:m-1366082849205",
       "message_type":"receipt",
       "time_to_live": 0,
       "from":"gcm.googleapis.com"
     }
     </gcm>
   </message>
    */
  private def createDeliveryReceiptMessageStanza(original: Stanza, jsonObject: Cursor): Stanza = {
    val payload = Json.obj(
      JSON_MESSAGE_TYPE -> Json.fromString(JSON_RECEIPT),
      JSON_FROM -> Json.fromString("gcm.googleapis.com"),
      JSON_CATEGORY -> Json.fromString("io.grhodes.client"),
      JSON_MESSAGE_ID ->Json.fromString(s"dr2:${jsonObject.get(JSON_MESSAGE_ID).getOrElse("")}"),
      JSON_ERROR -> Json.fromString(JSON_ERROR_BAD_REGISTRATION),
      JSON_ERROR_DESCRIPTION -> Json.fromString(s"Invalid token on 'to' field: ${jsonObject.get(JSON_TO).getOrElse("")}"),
      "data" -> Json.obj(
        JSON_MESSAGE_STATUS -> Json.fromString(JSON_MESSAGE_STATUS_SENT_TO_DEVICE),
        JSON_ORIGINAL_MESSAGE_ID -> Json.fromString(jsonObject.get[String](JSON_MESSAGE_ID).getOrElse("")),
        JSON_DEVICE_REG_ID -> Json.fromString(jsonObject.get[String](JSON_TO).getOrElse("")),
        JSON_SENT_TIMESTAMP -> Json.fromString(System.currentTimeMillis.toString)
      )
    ).noSpaces

    val builder = new StanzaBuilder("message")
    builder.addAttribute("id", Option(original.getAttributeValue("id")).getOrElse(""))
    val gcmMessageBuilder = new GCMMessage.Builder()
    gcmMessageBuilder.addText(payload)
    builder.addPreparedElement(gcmMessageBuilder.build())
    builder.build()
  }

  /**
    * Create Draining Control Message
   <message>
     <data:gcm xmlns:data="google:mobile:data">
     {
       "message_type":"control"
       "control_type":"CONNECTION_DRAINING"
     }
     </data:gcm>
   </message>
    */
  private def createDrainingMessageStanza(original: Stanza, jsonObject: Cursor): Stanza = {
    val payload = Json.obj(
      JSON_MESSAGE_TYPE -> Json.fromString(JSON_CONTROL),
      JSON_CONTROL_TYPE -> Json.fromString("CONNECTION_DRAINING")
    ).noSpaces

    val builder = new StanzaBuilder("message")
    builder.addAttribute("id", Option(original.getAttributeValue("id")).getOrElse(""))
    val gcmMessageBuilder = new GCMMessage.Builder()
    gcmMessageBuilder.addText(payload)
    builder.addPreparedElement(gcmMessageBuilder.build())
    builder.build()
  }

  protected def createErrorReply(originalStanza: Stanza, typ: StanzaErrorType, error: StanzaErrorCondition): Stanza = {
    val builder = new StanzaBuilder(originalStanza.getName, originalStanza.getNamespaceURI)
    builder.addAttribute("from", originalStanza.getTo.getFullQualifiedName)
    builder.addAttribute("to", originalStanza.getFrom.getFullQualifiedName)
    builder.addAttribute("id", originalStanza.getAttributeValue("id"))
    builder.addAttribute("type", "error")
    originalStanza.getInnerElements.asScala.foreach(builder.addPreparedElement)

    builder.startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", typ.value())
    builder.startInnerElement(error.value(), NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement()
    builder.endInnerElement()

    builder.build()
  }

}
