package com.its.openpath.module.siteminder.builders
import groovy.xml.MarkupBuilder

import org.codehaus.groovy.runtime.DateGroovyMethods
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.Message
import com.its.openpath.module.opscommon.model.messaging.ops.StatusApplicationControl
import com.its.openpath.module.opscommon.model.messaging.ops.rate.Rate
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateAmountInfo
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateRequest
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>RateManagementXMLBuilder</code>
 * <p/>
 * Concrete class for building RateManagementXMLBuilder and inherits common functions from AbstractSiteminderXMLBuilder
 * <p />
 * @author Lyle Fletcher
 * @since June 2012
 */
@Service("RateManagementXMLBuilder")
@ManagedResource('OPENPATH:name=/module/siteminder/builder/RateManagementXMLBuilder')
class RateManagementXMLBuilder extends AbstractSiteminderXMLBuilder
{
  private static final Logger sLogger = LoggerFactory.getLogger( RateManagementXMLBuilder.class.name )
  
  /**
   * Constructor
   */
  RateManagementXMLBuilder( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  @Override
  public String buildSoapXMLMessage( Message request )
  {
    RateRequest rateRequest = (RateRequest) request
    
    Closure buildRateAmountMessages =
    { builder ->
      UUID uuid = InvocationContext.instance.correlationId
      long time = TimeUUIDUtils.getTimeFromUUID(uuid)
      
      builder.OTA_HotelRateAmountNotifRQ( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: DateGroovyMethods.format(new Date(time),"yyyy-MM-dd'T'HH:mm:ss'-05:00'"), Version: '1.0' )
      { buildRateAmountMessages(builder, rateRequest) }
    }
    
    return super.buildSOAPEnvelope(buildRateAmountMessages)
  }
  
  /**
   * Build rate amount messages xml fragments
   *<p />
   *@param builder MarkupBuilder - need a reference to append xml fragment
   *@param requestObj RateRequest proto object
   *@return
   */
  def buildRateAmountMessages(MarkupBuilder builder, RateRequest requestObj)
  {
    List<RateAmountInfo> rateAmountMessages = requestObj.requestData.idsRatePlanInfo.rateAmountMessages
    if(!rateAmountMessages?.isEmpty())
    {
      builder.RateAmountMessages(HotelCode: requestObj.source.id)
      {
        rateAmountMessages.each
        {  RateAmountInfo rateAmountMessage ->
          RateAmountMessage()
          {
            StatusApplicationControl(InvTypeCode: rateAmountMessage.statusApplicationControl.inventoryTypeCode, RatePlanCode: rateAmountMessage.statusApplicationControl.ratePlanCode)
            {
              StatusApplicationControl sac = rateAmountMessage.statusApplicationControl
              boolean isEmptyChannelList = sac.bookingChannelList==null ? true : sac.bookingChannelList.isEmpty()
              if(!isEmptyChannelList)
              {
                String codes = sac.bookingChannelList.get ( 0 )
                if(codes.trim().length() > 0)
                {
                  DestinationSystemCodes
                  {
                    rateAmountMessage.statusApplicationControl.bookingChannelList.each
                    { channel ->
                      DestinationSystemCode( channel.code )
                    }
                  }
                }
              }
            }
            
            Rates
            {
              rateAmountMessage.rates.each
              { Rate rate ->
                Rate(CurrencyCode: rate.currencyCode, Start: rate.start, End: rate.end, Mon: rate.mon, Tue: rate.tue, Weds: rate.weds, Thur: rate.thur, Fri: rate.fri, Sat: rate.sat, Sun: rate.sun)
                {
                  BaseByGuestAmts
                  {
                    BaseByGuestAmt(AmountAfterTax: rate.amountAfterTax)
                  }
                  RateDescription
                  { Text (rate.rateDescription) }
                }
              }
            }
          }
        }
      }
    }
  }
}
