package com.its.openpath.module.pegasus.xml.builder

import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.pegasus.AbstractBaseBuilder
import com.its.openpath.module.pegasus.ErrorCodeMap
import com.its.openpath.module.pegasus.ErrorCodeMapping
import groovy.xml.MarkupBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.springframework.beans.factory.annotation.Autowired
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode

/**
 * <code>AbstractXMLBuilder</code>
 * <p/>
 * Base class for all XML OTA Message 'Builder' classes. Contains methods common for all subclasses.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */
abstract class AbstractXMLBuilder
extends AbstractBaseBuilder
{
  private static Logger sLogger = LoggerFactory.getLogger( AbstractXMLBuilder.class.name )

  @Autowired(required = true)
  protected ErrorCodeMap mErrorCodeMap

  /**
   * Helper method to return a Map of Attributes and their values in the root Element of the original Request Message.
   * These Attributes are required to be set in the Response message to be sent back to Pegasus USW.
   * <p />
   * @param requestElementName - Name of the Request's root Element
   * @return - Map built
   */
  def Map<String, String> getOTARootElementAttributesInResponse( String requestElementName )
  {
    Document document = (Document) InvocationContext.instance.getSessionDataItem( "REQ_DOC" )
    org.w3c.dom.Node requestRootEle = document.getElementsByTagName( requestElementName ).item( 0 )

    Map<String, String> attribMap = this.getNodeAttributeMap( requestRootEle )
    attribMap.remove( 'xsi:schemaLocation' )
    attribMap.remove( 'RateDetailsInd' )
    attribMap.remove( 'TransactionStatusCode' )
    attribMap.remove( 'ExactMatchOnly' )
    attribMap.put( 'PrimaryLangID', 'en' )

    return attribMap
  }

  /**
   * Add the <POS /> Element and its child elements to the passed in MarkupBuilder. The elements and their values to be
   * added are taken from the original Service Request received. This method is used when building response messages to
   * Pegasus USW as it expects the <POS /> element sent in the Request to be echoed back in the Response.
   * <p />
   * @param markupBuilder - Builder to add Elements
   */
  def buildPosElementInResponse( MarkupBuilder markupBuilder )
  {
    Document document = (Document) InvocationContext.instance.getSessionDataItem( "REQ_DOC" )
    org.w3c.dom.Node pos = document.getElementsByTagName( 'POS' ).item( 0 )
    org.w3c.dom.NodeList sourceList = pos.getElementsByTagName( 'Source' )

    markupBuilder."${pos.nodeName}"( this.getNodeAttributeMap( pos ) ) {
      sourceList.each { org.w3c.dom.Node sourceNode ->
        "${sourceNode.nodeName}"( this.getNodeAttributeMap( sourceNode ) ) {
          sourceNode.childNodes.each { org.w3c.dom.Node sourceChild1 ->
            if ( sourceChild1.nodeType == org.w3c.dom.Node.TEXT_NODE || sourceChild1.nodeType == org.w3c.dom.Node.COMMENT_NODE )
            {
              return
            }
            "${sourceChild1.nodeName}"( this.getNodeAttributeMap( sourceChild1 ) ) {
              sourceChild1.childNodes.each { org.w3c.dom.Node sourceChild2 ->
                if ( sourceChild2.nodeType == org.w3c.dom.Node.TEXT_NODE || sourceChild2.nodeType == org.w3c.dom.Node.COMMENT_NODE )
                {
                  return
                }
                "${sourceChild2.nodeName}"( this.getNodeAttributeMap( sourceChild2 ) )
              }
            }
          }
        }
      }
    }
  }

  /**
   * @see {@link com.its.openpath.module.pegasus.AbstractBaseBuilder#buildErrorResponse}
   */
  def buildErrorResponse( OpsErrorCode errorCode, String optionalText )
  {
    InvocationContext context = InvocationContext.instance
    context.success = false
    def writer = new StringWriter()

    try
    {
      ErrorCodeMapping mapping = mErrorCodeMap.getOTAErrorCodeMapping( errorCode.number )
      if ( sLogger.debugEnabled )
      {
        sLogger.debug "*** OPS error code: ${errorCode.number} is mapped to OTA error code: [${mapping.otacode}]"
      }
      Document document = (Document) InvocationContext.instance.getSessionDataItem( "REQ_DOC" )
      org.w3c.dom.Node pos = document.getElementsByTagName( 'POS' ).item( 0 )
      org.w3c.dom.NodeList sourceList = pos.getElementsByTagName( 'Source' )

      def markupBuilder = new MarkupBuilder( writer )
      markupBuilder.'SOAP-ENV:Envelope'( 'xmlns:SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' ) {
        markupBuilder.'SOAP-ENV:Header'( 'xmlns:wsa': 'http://schemas.xmlsoap.org/ws/2004/08/addressing',
          'xmlns:wsse': 'http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd' )
        markupBuilder.'SOAP-ENV:Body'() {
          markupBuilder.OTA_HotelAvailRS( this.getOTARootElementAttributesInResponse( "OTA_HotelAvailRQ" ) ) {
            this.buildPosElementInResponse( markupBuilder )
            Errors() {
              Error( Type: mapping?.otatype, Code: mapping?.otacode, Language: mapping?.lang, ShortText: optionalText )
            }
          }
        }
      }
    }
    catch ( Throwable e )
    {
      throw new IllegalStateException( "PEGASUS - Couldn't build an ERROR XML response to be sent to Pegasus: [${context.getSessionDataItem( "TXN_REF" )}] ", e )
    }
    context.externalSystemResponseData = writer.toString()
  }

  /**
   * Helper method to return a Map of Attribute names and their values present in the supplied Node. This is used by
   * MarkupBuidlers when building Response Message to be sent back.
   * <p />
   * @param node - Node to iterate and fetch Attributes and values
   */
  def getNodeAttributeMap( org.w3c.dom.Node node )
  {
    Map<String, String> map = new HashMap<String, String>()

    for ( i in 0..node.attributes.length )
    {
      org.w3c.dom.Node attributeNode = node.attributes.item( i )
      if ( attributeNode == null )
      {
        continue
      }
      map.put( attributeNode.nodeName, attributeNode.nodeValue )
    }

    return map
  }

}