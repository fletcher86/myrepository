package com.its.openpath.module.siteminder.builders
import org.codehaus.groovy.runtime.DateGroovyMethods
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.Message
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityStatusInfo
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>HotelAvailabilityXMLBuilder</code>
 * <p/>
 * Concrete class for building HotelAvailablity XML and inherits common functions from AbstractSiteminderXMLBuilder
 * <p />
 * @author Lyle Fletcher
 * @since June 2012
 */
@Service("HotelAvailabilityXMLBuilder")
@ManagedResource('OPENPATH:name=/module/siteminder/builder/HotelAvailabilityXMLBuilder')
class HotelAvailabilityXMLBuilder extends AbstractSiteminderXMLBuilder
{
  private static final Logger sLogger = LoggerFactory.getLogger( HotelAvailabilityXMLBuilder.class.name )
  
  /**
   * Constructor
   */
  HotelAvailabilityXMLBuilder( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  @Override
  public String buildSoapXMLMessage(Message request)
  {
    AvailabilityRequest hotelAvailabilityNotif = (AvailabilityRequest) request
    
    Closure buildBody =
    { builder ->
      UUID uuid = InvocationContext.instance.correlationId
      long time = TimeUUIDUtils.getTimeFromUUID(uuid)
      
      List<AvailabilityStatusInfo> availabilityStatusInfos = hotelAvailabilityNotif.availabilityStatusInfos
      
      builder.OTA_HotelAvailNotifRQ( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: DateGroovyMethods.format(new Date(time),"yyyy-MM-dd'T'HH:mm:ss'-05:00'"), Version: '1.0' )
      {
        AvailStatusMessages(HotelCode: hotelAvailabilityNotif.source.id)
        {
          availabilityStatusInfos.each
          { AvailabilityStatusInfo asi ->
            AvailStatusMessage(BookingLimit: asi.bookingLimit)
            {
              StatusApplicationControl(Start: asi.statusApplicationControl.start, End: asi.statusApplicationControl.end, InvTypeCode: asi.statusApplicationControl.inventoryTypeCode, RatePlanCode: asi.statusApplicationControl.ratePlanCode)
              if(asi.lengthOfStayTime!=null && asi.lengthOfStayTime.trim ().length()>0)
              {
                LengthsOfStay
                {
                  LengthOfStay(MinMaxMessageType: asi.lengthOfStayMinMaxMessageType, Time: asi.lengthOfStayTime)
                }
              }
              if(asi.restrictionStatus!=null && asi.restrictionStatus.trim ().length()>0)
              {
                RestrictionStatus(Status: asi.restrictionStatus)
              }
            }
          }
        }
      }
    }
    return super.buildSOAPEnvelope(buildBody)
  }
}
