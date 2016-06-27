package io.grhodes.mcm.server.gcm

import io.circe.{Cursor, Json}
import io.circe.jawn._
import org.apache.vysper.xml.fragment.{XMLElement, XMLElementBuilder}
import org.apache.vysper.xmpp.stanza.Stanza

import scala.collection.JavaConverters._

case class GCMMessage(element: XMLElement) {
  def getJsonObject: Cursor = parse(element.getSingleInnerText.getText).getOrElse(Json.Null).cursor
}
object GCMMessage {
  def fromStanza(stanza: Stanza) = {
    val xElms = stanza.getInnerElementsNamed(Constants.ELEMENT_NAME)
    val xElm = xElms.asScala.find(elm => elm.getNamespaceURI != null && elm.getNamespaceURI.startsWith(Constants.NAMESPACE))
    xElm.map(new GCMMessage(_)).orNull
  }

  class Builder extends XMLElementBuilder(Constants.ELEMENT_NAME, Constants.NAMESPACE)

}
