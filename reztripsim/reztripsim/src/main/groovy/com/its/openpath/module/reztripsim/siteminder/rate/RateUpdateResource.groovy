package com.its.openpath.module.reztripsim.siteminder.rate

import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.JsonIOUtil
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
@Path("/siteminderrate")
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
 
  /**
   * Generate a Rate Update Notification with test data and POST it to OPSARI module's REST Endpoint.
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/rate/update
   */
  @POST
  @Path('/update')
  @ManagedOperation()
  def Response generateRateUpdateNotification( )
  {
    sLogger.info 'Generating a rate update request ...'
    
    String endpoint = "${mOpsAriBaseURL}${mOpsAriRateUpdateURI}"
    
    String requestJSON
    RateRequest rateRequest = new RateRequest()
    
    Source source = new Source()
    source.id = 'ABC'
    source.type = 'HOTEL'
    source.description = 'RATE UPDATE NOTIFICATION'
    
    rateRequest.source = source
    rateRequest.productType = ProductType.HOTEL_ROOM
    
    rateRequest.requestData = new RoomRateManagementRequest()
    
    try
    {
      rateRequest.requestData.extSysRefId = "1234"
      rateRequest.requestData.extSysTimestamp = new Date().time
      rateRequest.requestData.idsRatePlanInfo = new IDSRatePlanInfoRequest()
      rateRequest.requestData.rateManagementType = RateManagementType.RATE_UPDATE_NOTIF
      
      List<RateAmountInfo> rateAmountMessages = new ArrayList<RateAmountInfo>()
      
      StatusApplicationControl statusApplicationControl = new StatusApplicationControl()
      statusApplicationControl.inventoryTypeCode = '4SF'
      statusApplicationControl.ratePlanCode = '3PW'
      
      RateAmountInfo rai = new RateAmountInfo()
      rai.statusApplicationControl = statusApplicationControl
      
      List<Rate> rateList = new ArrayList<Rate>()
      
      Rate rate = new Rate()
      rate.currencyCode = "QUID"
      rate.start = "2012-09-15"
      rate.end = "2012-09-15"
      rate.amountAfterTax = "43.00"
      rate.sun = "1"
      rate.mon = "1"
      rate.tue = "1"
      rate.weds = "1"
      rate.thur = "1"
      rate.fri = "1"
      rate.sat = "1"
      rateList.add( rate )
      rai.rates = rateList
      
      rateAmountMessages.add(rai)
      rateRequest.requestData.idsRatePlanInfo.rateAmountMessagesList = rateAmountMessages
      
      Writer writer = new StringWriter()
      JsonIOUtil.writeTo( writer, rateRequest, rateRequest.cachedSchema(), false )
      requestJSON = writer.toString()
      sLogger.info "*** REZTRIPSIM - built Rate Update Notification JSON message to be POSTed to OPSARI module ..."
    }
    catch ( Throwable e )
    {
      this.logAndHandlePOSTerror( "*** REZTRIPSIM - Couldn't create a RateManagementRequet to be POSTed to OPS", e )
      return
    }
    
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