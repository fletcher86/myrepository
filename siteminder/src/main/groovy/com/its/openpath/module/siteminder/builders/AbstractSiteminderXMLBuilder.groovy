package com.its.openpath.module.siteminder.builders

import groovy.xml.MarkupBuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.dyuproject.protostuff.Message
import com.its.openpath.module.opscommon.model.messaging.ops.OpsTxnType

/**
 * <code>AbstractSiteminderXMLBuilder</code>
 * <p/>
 * Base class for all 'Builder' classes. Contains methods common for all subclasses.
 * <p />
 * @author Kent Fletcher
 * @since June 2012
 */
abstract class AbstractSiteminderXMLBuilder
{
  private static final Logger sLogger = LoggerFactory.getLogger( AbstractSiteminderXMLBuilder.class.name )
  
  protected String buildSOAPEnvelope(Closure body)
  {
    sLogger.info ("INSIDE ABSTRACT CLASS 'AbstractSiteminderXMLBuilder' AND METHOD 'buildSOAPEnvelope' BUILDING SOAP XML MARKUP")
    Writer writer = new StringWriter()
    MarkupBuilder builder = new MarkupBuilder( writer )
    builder.'SOAP-ENV:Envelope'( 'xmlns:SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/', 'xmlns:soapenv': 'http://schemas.xmlsoap.org/soap/envelope/', 'soapenv:mustUnderstand': '1' )
    {
      'SOAP-ENV:Header'(){
        'wsse:Security'('xmlns:wsse': 'http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd'){
         'wsse:UsernameToken'() {
           'wsse:Username'('reztrip')
           'wsse:Password'(Type: 'http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText', '7GZ896233clvKqy' )
         }  
        }
      }
      builder.'SOAP-ENV:Body'()
      { body(builder) }
    }
    return writer.toString()
  }
  public abstract String buildSoapXMLMessage(Message request)
}
