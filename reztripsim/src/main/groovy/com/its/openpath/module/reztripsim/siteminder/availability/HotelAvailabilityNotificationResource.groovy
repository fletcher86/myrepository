package com.its.openpath.module.reztripsim.siteminder.availability
import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.Source
import com.its.openpath.module.opscommon.model.messaging.ops.StatusApplicationControl
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
  
  private @Value("#{runtimeProperties['your.property.key']}")
  String yourPropertyValue;
  
  private @Value("#{runtimeProperties['opsari.hotelAvailabilityNotification.uri']}")
  String mOpsAriHotelAvailabilityNotificationURI
  
  
  /**
   * Constructor
   */
  HotelAvailabilityNotificationResource( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  AvailabilityStatusInfo getAvailabilityRequest(def inventoryTypeCode, def ratePlanCode, def start, def end, def mon, def tue, def weds, def thur, def fri, def sat, def sun, def restrictionStatus, def lengthOfStayMin, def setMinLOS, def bookingLimit)
  {
    
    AvailabilityStatusInfo asi = new AvailabilityStatusInfo()
    StatusApplicationControl sac = new StatusApplicationControl()
    sac.inventoryTypeCode = inventoryTypeCode
    sac.ratePlanCode=ratePlanCode
    sac.start = start
    sac.end = end
    sac.mon = mon
    sac.tue = tue
    sac.weds = weds
    sac.thur = thur
    sac.fri = fri
    sac.sat = sat
    sac.sun = sun
    asi.statusApplicationControl = sac
    asi.restrictionStatus = restrictionStatus
    asi.lengthOfStayTime = lengthOfStayMin
    asi.lengthOfStayMinMaxMessageType = setMinLOS
    asi.bookingLimit = bookingLimit
    return asi
  }
  /**
   * Generate a Hotel Availability Notification with test data and POST it to OPSARI module's REST Endpoint.
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/hotel/availablity
   */
  @POST
  @Path('/availability')
  @ManagedOperation()
  def Response generateHotelAvailabilityNotification( @Context HttpServletRequest servletRequest  )
  {
    sLogger.info 'Generating a hotel availability notification request ...'
    
    String endpoint = "${mOpsAriBaseURL}${mOpsAriHotelAvailabilityNotificationURI}"
    
    String requestJSON
    
    AvailabilityRequest availabilityNotificationRequest = new AvailabilityRequest()
    Source source = new Source()
    source.id = 'REZTRIP218'
    source.type = 'HOTEL'
    source.description = 'HOTEL AVAILABILITY NOTIFICATION'
    availabilityNotificationRequest.source = source
    availabilityNotificationRequest.productType = ProductType.HOTEL_ROOM
    
    availabilityNotificationRequest.availabilityStatusInfos = new ArrayList<AvailabilityStatusInfo> ()
    
    String text = servletRequest.inputStream.text
    List<String> input = text.tokenize("|")
    
    for(String status : input)
    {
      List<String> vals = status.tokenize(",")
      def  inventoryTypeCode = vals.get(0)
      def ratePlanCode  = vals.get(1)
      def start = vals.get(2)
      def end = vals.get(3)
      def mon = vals.get(4)
      def tue = vals.get(5)
      def weds = vals.get(6)
      def thur = vals.get(7)
      def fri = vals.get(8)
      def sat = vals.get(9)
      def sun = vals.get(10)
      def restrictionStatus = vals.get(11)
      def lengthOfStayMin = vals.get(12)
      def setMinLOS  = vals.get(13)
      def bookingLimit = vals.get(14)
      availabilityNotificationRequest.availabilityStatusInfos.add(getAvailabilityRequest(inventoryTypeCode, ratePlanCode,start,end,mon,tue,weds,thur,fri,sat,sun,restrictionStatus,lengthOfStayMin,setMinLOS,bookingLimit))
    }
    
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