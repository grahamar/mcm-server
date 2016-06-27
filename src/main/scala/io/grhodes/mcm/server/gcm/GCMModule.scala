package io.grhodes.mcm.server.gcm

import java.util
import java.util.concurrent.atomic.AtomicReference

import com.gilt.gfc.logging.Loggable
import org.apache.vysper.xmpp.addressing.{Entity, EntityUtils}
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule
import org.apache.vysper.xmpp.modules.servicediscovery.management._
import org.apache.vysper.xmpp.protocol.{HandlerDictionary, NamespaceHandlerDictionary, StanzaProcessor}
import org.apache.vysper.xmpp.server.ServerRuntimeContext
import org.apache.vysper.xmpp.server.components.{Component, ComponentStanzaProcessor}

class GCMModule(serviceContext: ServiceContext) extends DefaultDiscoAwareModule
  with Component with ComponentInfoRequestListener with ItemRequestListener with Loggable {

  private val Subdomain = Constants.ELEMENT_NAME
  private val messageHandler = new AtomicReference[Option[GCMMessageHandler]](None)
  private var serverRuntimeContext: ServerRuntimeContext = _
  private var stanzaProcessor: ComponentStanzaProcessor = _
  private var fullDomain: Entity = _

  override def initialize(serverRuntimeContext: ServerRuntimeContext) = {
    super.initialize(serverRuntimeContext)
    this.serverRuntimeContext = serverRuntimeContext

    this.fullDomain = EntityUtils.createComponentDomain(Subdomain, serverRuntimeContext)

    this.stanzaProcessor = new ComponentStanzaProcessor(serverRuntimeContext)
    this.stanzaProcessor.addHandler(getMessageHandler)

    info("GCMModule is initialized")
  }

  override def getComponentInfosFor(request: InfoRequest): util.List[InfoElement] = {
    if (fullDomain.getDomain.equals(request.getTo.getDomain))
      info(s"GCMModule.getComponentInfosFor(): ${request.toString}")

    // TODO
    null
  }

  override def getItemsFor(request: InfoRequest): util.List[Item] = {
    // TODO
    null
  }

  override val getName: String = "GCM"
  override val getVersion: String = "1.0"
  override val getSubdomain: String = Subdomain
  override val getStanzaProcessor: StanzaProcessor = stanzaProcessor

  override protected def addItemRequestListeners(itemRequestListeners: java.util.List[ItemRequestListener]): Unit = {
    itemRequestListeners.add(this)
  }

  override protected def addComponentInfoRequestListeners(componentInfoRequestListeners: java.util.List[ComponentInfoRequestListener]): Unit = {
    componentInfoRequestListeners.add(this)
  }

  override protected def addHandlerDictionaries(dictionary: java.util.List[HandlerDictionary]): Unit = {
    info("GCMModule.addHandlerDictionaries()")
    dictionary.add(new NamespaceHandlerDictionary(Constants.NAMESPACE, getMessageHandler))
  }

  private def getMessageHandler: GCMMessageHandler = this.synchronized {
    this.messageHandler.get().getOrElse {
      val newHandler = new GCMMessageHandler(serviceContext, fullDomain)
      this.messageHandler.getAndSet(Some(newHandler))
      newHandler
    }
  }

}
