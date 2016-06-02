package com.its.openpath.module.reztripsim.siteminder.rate

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.BookingChannel
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.Source
import com.its.openpath.module.opscommon.model.messaging.ops.StatusApplicationControl
import com.its.openpath.module.opscommon.model.messaging.ops.rate.IDSRatePlanInfoRequest
import com.its.openpath.module.opscommon.model.messaging.ops.rate.Rate
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateAmountInfo
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateRequest
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RoomRateManagementRequest
import com.its.openpath.module.reztripsim.siteminder.AbstractReztripSimulatorResource
/**
 * <code>RateUpdateResource</code>
 * <p/>
 * The REST Resource that can be used to POST JSON Rate Update Notifications to the OPSARI Endpoint. This simulates
 * the RezTrip sending the same type of messages whenever Rate updates take place.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("RateUpdateResource")
@ManagedResource('OPENPATH:name=/module/reztripsim/siteminder/rate/RateUpdateResource')
@Path("/siteminder")
class RateUpdateResource  extends AbstractReztripSimulatorResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( RateUpdateResource.class.name )
  
  private @Value("#{runtimeProperties['opsari.base.url']}")
  String mOpsAriBaseURL
  
  private @Value("#{runtimeProperties['opsari.rateUpdate.uri']}")
  String mOpsAriRateUpdateURI
  
  /**
   * Constructor
   */
  RateUpdateResource( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  
  def String getRateNotification(def id, def type, def description, def externalId, def roomCode, def rateCode, def currencyCode, def start, def end, def amountAfterTax, def mon, def tue, def weds, def thur, def fri, def sat, def sun, def incl)
  {
    RateRequest rateRequest = new RateRequest()
    Source source = new Source()
    source.id = id
    source.type = type
    source.description = description
    
    rateRequest.source = source
    rateRequest.productType = ProductType.HOTEL_ROOM
    
    rateRequest.requestData = new RoomRateManagementRequest()
    
    rateRequest.requestData.extSysRefId = externalId
    rateRequest.requestData.extSysTimestamp = new Date().time
    rateRequest.requestData.idsRatePlanInfo = new IDSRatePlanInfoRequest()
    rateRequest.requestData.rateManagementType = RateManagementType.RATE_UPDATE_NOTIF
    
    List<RateAmountInfo> rateAmountMessages = new ArrayList<RateAmountInfo>()
    
    StatusApplicationControl statusApplicationControl = new StatusApplicationControl()
    statusApplicationControl.inventoryTypeCode = roomCode
    statusApplicationControl.ratePlanCode = rateCode
    
    String inclusions = (String) incl
    if(inclusions!=null && inclusions.trim().length() > 0)
    {
      statusApplicationControl.bookingChannelList = new ArrayList<BookingChannel> ()
      
      List<String> inclusionList = inclusions.tokenize ( "," )
      for(String tok : inclusionList)
      {
        BookingChannel bc = new BookingChannel()
        bc.code = tok
        statusApplicationControl.bookingChannelList.add(bc)
      }
    }
    
    RateAmountInfo rai = new RateAmountInfo()
    rai.statusApplicationControl = statusApplicationControl
    
    List<Rate> rateList = new ArrayList<Rate>()
    
    if (amountAfterTax!=null && amountAfterTax.trim().length()>0)
    {
      List<String> amounts = amountAfterTax.tokenize(",")
      List<String> starts = start.tokenize(",")
      List<String> ends = end.tokenize(",")
      int i = 0
      for(String amount : amounts)
      {
        Rate rate = new Rate()
        rate.currencyCode = currencyCode
        rate.start =  starts.get ( i )
        rate.end = ends.get(i)
        rate.mon = mon
        rate.tue = tue
        rate.weds = weds
        rate.thur = thur
        rate.fri = fri
        rate.sat = sat
        rate.sun = sun
        rate.rateDescription = description
        rate.amountAfterTax = amount
        rateList.add( rate )
        i++
      }
    }
    
    rai.rates = rateList
    
    rateAmountMessages.add(rai)
    rateRequest.requestData.idsRatePlanInfo.rateAmountMessagesList = rateAmountMessages
    
    String requestJSON = null
    try
    {
      Writer writer = new StringWriter()
      JsonIOUtil.writeTo( writer, rateRequest, rateRequest.cachedSchema(), false )
      requestJSON = writer.toString()
      sLogger.info "*** REZTRIPSIM - built Rate Update Notification JSON message to be POSTed to OPSARI module ..."
    }
    catch ( Throwable e )
    {
      this.logAndHandlePOSTerror( "*** REZTRIPSIM - Couldn't create a RateManagementRequet to be POSTed to OPS", e )
    }
    
    return requestJSON
  }
  
  
  /**
   * Generate a Rate Update Notification with test data and POST it to OPSARI module's REST Endpoint.
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/rate/update
   */
  @POST
  @Path('/scenario3')
  @ManagedOperation()
  def Response scenario3( @Context HttpServletRequest servletRequest  )
  {
    String text = servletRequest.inputStream.text
    List<String> input = text.tokenize("|")
    sLogger.info 'Generating a rate update request ...'
    
    def sourceId = input.getAt ( 0)
    def sourceType = input.getAt ( 1)
    def description = input.getAt (2)
    def externalId = input.getAt ( 3)
    def roomCode = input.getAt ( 4)
    def rateCode = input.getAt ( 5)
    def currencyCode = input.getAt ( 6)
    def start = input.getAt ( 7)
    def end = input.getAt ( 8)
    def amount = input.getAt ( 9)
    def mon = input.getAt ( 10)
    def tues = input.getAt ( 11)
    def wed = input.getAt ( 12)
    def thur = input.getAt ( 13)
    def fri = input.getAt ( 14)
    def sat = input.getAt ( 15 )
    def sun = input.getAt ( 16 )
    def incl = input.getAt ( 17 )
    
    String endpoint = "${mOpsAriBaseURL}${mOpsAriRateUpdateURI}"
    
    String requestJSON = getRateNotification(sourceId, sourceType, description, externalId, roomCode, rateCode, currencyCode, start, end, amount, mon, tues, wed, thur, fri, sat, sun, incl)
    Response responseObj = Response.serverError().build()
    // POST to OPSARI
    postRequest( requestJSON, endpoint ) { String responseJSON ->
      // Not doing anything with the response yet
      sLogger.info "*** REZTRIPSIM - Received the response below from OPSARI REST Endpoint as a response for Rate Update Notification"
      sLogger.info "*************************************************************************************************"
      sLogger.info "${responseJSON}"
      responseObj = Response.ok( responseJSON ).status( 200 ).build()
    }
    
    return responseObj
  }
  
}