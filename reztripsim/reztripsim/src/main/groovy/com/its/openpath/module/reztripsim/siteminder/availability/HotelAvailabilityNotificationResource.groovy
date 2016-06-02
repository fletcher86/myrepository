package com.its.openpath.module.reztripsim.siteminder.availability
import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.Source
import com.its.openpath.module.opscommon.model.messaging.ops.StatusApplicationControl
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
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityStatusInfo
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilitySearchType
import com.its.openpath.module.opscommon.model.messaging.ops.Search
import com.its.openpath.module.opscommon.model.messaging.ops.Source
import com.its.openpath.module.opscommon.model.messaging.ops.StatusApplicationControl
import com.its.openpath.module.opscommon.model.messaging.ops.Stay
import com.its.openpath.module.reztripsim.siteminder.AbstractReztripSimulatorResource

/**
 * <code>HotelAvailabilityNotificationResource</code>
 * <p/>
 * The REST Resource that can be used to POST JSON Hotel Availability Update Notifications to the OPSARI Endpoint. This simulates
 * the RezTrip sending the same type of messages whenever Rate updates take place.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("HotelAvailabilityNotificationResource")
@ManagedResource('OPENPATH:name=/module/reztripsim/siteminder/availablility/HotelAvailabilityNotificationResource')
@Path("/hotel")
class HotelAvailabilityNotificationResource extends AbstractReztripSimulatorResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( HotelAvailabilityNotificationResource.class.name )
  
  private @Value("#{runtimeProperties['opsari.base.url']}")
  String mOpsAriBaseURL
  
  private @Value("#{runtimeProperties['opsari.hotelAvailabilityNotification.uri']}")
  String mOpsAriHotelAvailabilityNotificationURI
  
  
  /**
   * Constructor
   */
  HotelAvailabilityNotificationResource( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  /**
   * Generate a Hotel Availability Notification with test data and POST it to OPSARI module's REST Endpoint.
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/hotel/availablity
   */
  @POST
  @Path('/availability')
  @ManagedOperation()
  def Response generateHotelAvailabilityNotification( )
  {
    
    sLogger.info 'Generating a hotel availability notification request ...'
    
    String endpoint = "${mOpsAriBaseURL}${mOpsAriHotelAvailabilityNotificationURI}"
    
    String requestJSON
    AvailabilityRequest availabilityNotificationRequest = new AvailabilityRequest()
    Source source = new Source()
    source.id = 'ABC'
    source.type = 'HOTEL'
    source.description = 'HOTEL AVAILABILITY NOTIFICATION'
    availabilityNotificationRequest.source = source
    availabilityNotificationRequest.productType = ProductType.HOTEL_ROOM
    
    List<AvailabilityStatusInfo> asis = new ArrayList<AvailabilityStatusInfo> ()
    AvailabilityStatusInfo asi1 = new AvailabilityStatusInfo()
    StatusApplicationControl sac1 = new StatusApplicationControl()
    sac1.inventoryTypeCode = 'A1K'
    sac1.ratePlanCode = 'GLD'
    sac1.start = "10-15-2012"
    sac1.end = "10-31-2012"
    sac1.mon= '1'
    sac1.tue = '1'
    sac1.weds = '1'
    sac1.thur = '1'
    sac1.fri = '1'
    sac1.sat = '0'
    sac1.sun = '0'
    asi1.statusApplicationControl = sac1
    asi1.restrictionStatus = 'Close'
    
    AvailabilityStatusInfo asi2 = new AvailabilityStatusInfo()
    StatusApplicationControl sac2 = new StatusApplicationControl()
    sac2.inventoryTypeCode = 'M1A1'
    sac2.ratePlanCode='PLAT'
    sac2.start = "10-17-2012"
    sac2.end = "10-22-2012"
    sac2.mon = '0'
    sac2.tue = '1'
    sac2.weds = '0'
    sac2.thur = '1'
    sac2.fri = '1'
    sac2.sat = '0'
    sac2.sun = '0'
    asi2.statusApplicationControl = sac2
    asi2.restrictionStatus = 'OPEN'
    
    asis.add(asi1)
    asis.add(asi2)
    availabilityNotificationRequest.availabilityStatusInfos = asis
    
    RoomAvailabilityRequest requestData = new RoomAvailabilityRequest()
    requestData.searchType = RoomAvailabilitySearchType.AREA_AVAILABILITY
    requestData.extSysRefId = "1234"
    requestData.extSysTimestamp = new Date().time.toString ()
    requestData.multipartRequest = "false"
    requestData.requireRateDetails = "false"
    requestData.stay = new Stay()
    requestData.stay.roomCount = "3"
    requestData.stay.start = "next week"
    requestData.search = new Search()
    requestData.search.isReturnUnavailableItems = "false"
    
    availabilityNotificationRequest.requestData = requestData
    try
    {
      Writer writer = new StringWriter()
      JsonIOUtil.writeTo( writer, availabilityNotificationRequest, availabilityNotificationRequest.cachedSchema(), false )
      requestJSON = writer.toString()
      sLogger.info "*** REZTRIPSIM - built Hotel Availability Notification JSON message to be POSTed to OPSARI module ..."
    }
    catch ( Throwable e )
    {
      this.logAndHandlePOSTerror( "*** REZTRIPSIM - Couldn't create a AvailabilityRequest to be POSTed to OPS", e )
      return
    }
    
    Response responseObj = Response.serverError().build()
    
    postRequest( requestJSON, endpoint )
    { String responseJSON ->
      sLogger.info "*** REZTRIPSIM - Received the response below from OPSARI REST Endpoint as a response for a Hotel Availability Update Notification"
      sLogger.info "*************************************************************************************************"
      sLogger.info "${responseJSON}"
      responseObj = Response.ok( responseJSON ).status( 200 ).build()
    }
    
    return responseObj
  }
}