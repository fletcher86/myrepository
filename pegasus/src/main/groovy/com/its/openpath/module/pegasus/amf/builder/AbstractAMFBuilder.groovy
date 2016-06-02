package com.its.openpath.module.pegasus.amf.builder

import com.its.openpath.module.opscommon.model.messaging.ops.ExtraBedInfo
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ExtraPersonInfo
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.pegasus.ErrorCodeMap
import com.its.openpath.module.pegasus.ErrorCodeMapping
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * <code>AbstractAMFBuilder</code>
 * <p/>
 * Base class of {@link AMFReservationManagementMessageBuilder} and {@link AMFRateManagementMessageBuilder}. Contains
 * methods common to both subclasses
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

abstract class AbstractAMFBuilder
{
  private static Logger sLogger = LoggerFactory.getLogger( AbstractAMFBuilder.class.name )

  @Autowired(required = true)
  protected ErrorCodeMap mErrorCodeMap


  /**
   * Helper function to decide whether to add a field to add a field to a message or not. This is used by subclasses
   * when building response message to be sent to USW.
   * <p />
   * @param fieldName - name of the field to add
   * @param fieldValue - value of the field to add
   * @param writer - object to add field name and value
   */
  def appendField( String fieldName, String fieldValue, StringBuilder writer )
  {
    if ( !fieldValue )
    {
      return
    }
    writer << fieldName << fieldValue
  }

  /**
   * Iterate over all properties of the given Service Request object and remove fields/properties that have empty values
   * <p />
   * @param requestObjJSON - Request object to remove fields from
   */
  def removeEmptyListsAndFieldsFromRequest( Object requestObjJSON )
  {
    // Remove empty Lists and objects whose all elements are null
    requestObjJSON.metaClass.properties.each { MetaProperty metaProperty ->
      if ( metaProperty.name.equals( 'schema' ) || metaProperty.name.equals( 'class' ) || metaProperty.name.equals( 'defaultInstance' ) )
      {
        return
      }
      if ( metaProperty.type.name.equals( List.class.name ) )
      {
        List list = (List) requestObjJSON.metaClass.getProperty( requestObjJSON.class, requestObjJSON, metaProperty.name, false, false )
        if ( list?.isEmpty() )
        {
          metaProperty = null
        }
      }
      else
      {
        Object prop = requestObjJSON.metaClass.getProperty( requestObjJSON.class, requestObjJSON, metaProperty.name, false, false )
        def interfacesB = [] as Set
        boolean empty = true
        prop?.class?.interfaces?.each { interfacesB << it.name }
        // Pick on the Protobuff generated classes
        if ( !interfacesB.contains( 'java.io.Externalizable' ) )
        {
          return
        }
        prop.metaClass.properties.each { MetaProperty meta ->
          if ( meta.name.equals( 'schema' ) || meta.name.equals( 'class' ) || meta.name.equals( 'defaultInstance' ) )
          {
            return
          }
          if ( meta.type.name.equals( List.class.name ) )
          {
            List list = (List) prop.metaClass.getProperty( prop.class, prop, meta.name, false, false )
            if ( !list?.isEmpty() )
            {
              list.each {
                empty = !it ?: false
              }
            }
            return
          }
          Object childProp = prop.metaClass.getProperty( prop.class, prop, meta.name, false, false )
          if ( childProp )
          {
            empty = false
          }
        }
        if ( empty )
        {
          requestObjJSON.metaClass.setProperty( requestObjJSON.class, requestObjJSON, metaProperty.name, null, false, false )
          prop = null
        }
      }
    } // Iterate each property/field
  }

  /**
   * @see {@link com.its.openpath.module.pegasus.AbstractBaseBuilder#buildErrorResponse}
   */
  def buildErrorResponse( OpsErrorCode errorCode, String optionalText )
  {
    List<AMFErrorMessage> amfErrorMessageList = new ArrayList<AMFErrorMessage>( 1 )
    AMFErrorMessage amfErrorMessage = new AMFErrorMessage( code: errorCode.number, optionalText: optionalText )
    amfErrorMessageList << amfErrorMessage
    this.buildAMFErrorResponse( amfErrorMessageList, null )
  }

  /**
   * Build an Error Response message to be sent back to Pegasus USW and set it in the InvocationContext.
   * <p />
   * @param errorMessageList - List of AMF error messages to include in the Response to be sent to USW
   * @param additionalText - Any additional text/error code to set in the Error Response to be sent back
   */
  def String buildAMFErrorResponse( List<AMFErrorMessage> errorMessageList, String additionalText )
  {
    InvocationContext context = InvocationContext.instance
    context.success = false
    def writer = new StringBuilder()

    try
    {
      writer << getSoapCrapBegin()
      writer << (String) InvocationContext.instance.getSessionDataItem( "MESSAGE_HDR" )
      writer << '||BOOKRP'
      writer << '|BST'
      writer << (additionalText ?: 'NO')
      errorMessageList.each {
        ErrorCodeMapping mapping = mErrorCodeMap.getAMFErrorCodeMapping( it.code )
        String errCode = mapping?.amfcode
        writer << "||ERRREP|DEE${errCode.substring( 0, 3 )}|ERC${errCode.substring( 3, errCode.length() )}|ETX${it.optionalText}"
      }
      writer << getSoapCrapEnd()
    }
    catch ( Throwable e )
    {
      throw new IllegalStateException( "PEGASUS - Couldn't build an AMF ERROR response to be sent to Pegasus: [${context.getSessionDataItem( "TXN_REF" )}] ", e )
    }
    context.externalSystemResponseData = writer.toString()
  }

  /**
   * Return the start of the crappy SOAP Envelope where AMF messages need to be embedded.
   * @return String - SOAP crap
   */
  def getSoapCrapBegin( )
  {
    return '''
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
<SOAP-ENV:Header xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
<wsa:MessageID>MESSAGE ID</wsa:MessageID>
<wsa:To>CUSTOMER API URL</wsa:To>
<wsa:Action>ACTION VALUE</wsa:Action>
<wsa:ReplyTo>
<wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>
</wsa:ReplyTo>
<wsse:Security SOAP-ENV:mustUnderstand="true">
<wsse:UsernameToken>
  <wsse:Username>USERNAME</wsse:Username>
  <wsse:Password>PASSWORD</wsse:Password>
  <wsse:PartnerID>ADS CHANNEL</wsse:PartnerID>
</wsse:UsernameToken>
</wsse:Security>
</SOAP-ENV:Header>
<SOAP-ENV:Body>
<amf>'''
  }

  /**
   * Return the end of the crappy SOAP Envelope where AMF messages need to be embedded.
   * @return String - SOAP crap
   */
  def getSoapCrapEnd( )
  {
    return '''\n</amf>\n</SOAP-ENV:Body></SOAP-ENV:Envelope>'''
  }

  /**
   * Helper class to represent field names and their values in an AMF Service Request body.
   */
  class AMFRequestBodyElementMap
  extends LinkedHashMap<String, Object>
  {
    /**
     * Add the provided field name and its value to the Map
     * <p />
     * @param fieldName - Unique name of the field
     * @param fieldValue - value of the field
     */
    def addField( String fieldName, Object fieldValue )
    {
      this.put( fieldName, fieldValue )
    }

    /**
     * Return the value of the field identified by the given name
     * <p />
     * @param fieldName - Unique field name
     * @return Object - value of the field
     */
    def Object getField( String fieldName )
    {
      this.get( fieldName )
    }

    /**
     * Add the supplied extra bed info object to the internal List maintained in the Map.
     * <p />
     * @param extraBedInfo - object to add to the List
     */
    def addExtraBedInfo( ExtraBedInfo extraBedInfo )
    {
      List<ExtraBedInfo> extraBedInfoList = (List<ExtraBedInfo>) this.get( 'extraBedInfo' )
      if ( !extraBedInfoList )
      {
        extraBedInfoList = new LinkedList<ExtraBedInfo>()
        this.put( 'extraBedInfo', extraBedInfoList )
      }
      extraBedInfoList.add( extraBedInfo )
    }

    /**
     * Return the extra bed info object List from the internal Map.
     * <p />
     * @return List - internal List of extra bed info
     */
    def List<ExtraBedInfo> getExtraBedInfoList( )
    {
      return (List<ExtraBedInfo>) this.get( 'extraBedInfo' )
    }

    /**
     * Add the supplied List of meal plans to the internal Map
     * @param mealPlanList - List of meal plans
     */
    def addMealPlanList( List<String> mealPlanList )
    {
      this.put( 'mealPlanList', mealPlanList )
    }

    /**
     * Return the List of meal plans from the internal Map
     * @return List - meal plans
     */
    def List<String> getMealPlanList( )
    {
      (List<String>) this['mealPlanList']
    }

    /**
     * Add the supplied List of child age to the internal Map
     * @param childAgeList - List of child ages
     */
    def addChildAgeList( List<String> childAgeList )
    {
      this.put( 'childAgeList', childAgeList )
    }

    /**
     * Return the List of child ages from the internal Map
     * @return List - child ages
     */
    def List<String> getChildAgeList( )
    {
      (List<String>) this['childAgeList']
    }

    /**
     * Add the supplied special requests List to the internal Map
     * @param specialReqList - List of special requests
     */
    def addSpecialReqList( List<String> specialReqList )
    {
      this.put( 'specialReqList', specialReqList )
    }

    /**
     * Return the List of special requests from the internal Map
     * @return List - special requests
     */
    def List<String> getSpecialRequestsList( )
    {
      (List<String>) this['specialReqList']
    }

    /**
     * Add the supplied extra person info List to the internal Map
     * @param extraPersonInfo - extra person info object to add
     */
    def addExtraPersonInfo( ExtraPersonInfo extraPersonInfo )
    {
      List<ExtraPersonInfo> extraPersonInfoList = (List<ExtraPersonInfo>) this.get( 'extraPersonInfo' )
      if ( !extraPersonInfoList )
      {
        extraPersonInfoList = new LinkedList<ExtraPersonInfo>()
        this.put( 'extraPersonInfo', extraPersonInfoList )
      }
      extraPersonInfoList.add( extraPersonInfo )
    }

    /**
     * Return the List of special requests from the internal Map
     * @return List - special requests
     */
    def List<ExtraPersonInfo> getExtraPersonInfoList( )
    {
      return (List<ExtraPersonInfo>) this.get( 'extraPersonInfo' )
    }
  }

}
