package com.its.openpath.module.opscommon.event.persistence


import java.util.UUID;

import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.its.openpath.module.opscommon.comm.bus.local.OpsLocalMessageBus
import com.its.openpath.module.opscommon.event.persistence.RequestEventXMLPersistenceHandler;
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * Created with IntelliJ IDEA.
 * User: kent
 * Date: 7/3/12
 * Time: 8:43 PM
 * To change this template use File | Settings | File Templates.
 */


class RequestResponseEventPersistenceHandlerTest
{
  
  private static Logger sLogger = LoggerFactory.getLogger(RequestResponseEventPersistenceHandlerTest.getName())
  
  public static RequestEventXMLPersistenceHandler eventPersistenceHandler
  
  public static UUID uuid = null;
  
  @BeforeClass
  public static void init()
  {
    eventPersistenceHandler = new RequestEventXMLPersistenceHandler()
    eventPersistenceHandler.mOpsMessageBus = new OpsLocalMessageBus ()
    eventPersistenceHandler.init()
    uuid =  TimeUUIDUtils.getUniqueTimeUUIDinMicros ();
  }
  
  @Test
  def void persistRequest()
  {
    String requestPayload = """
<OTA_NotifReportRQ xmlns="http://www.opentravel.org/OTA/2003/05" Version="1.0" EchoToken="echo">
  <Success/>
  <NotifDetails>
    <HotelNotifReport>
      <HotelReservations>
        <HotelReservation CreateDateTime="2010-01-01T12:00:00" ResStatus="Book">
          <UniqueID Type="14" ID="WTF-123456"/>
          <ResGlobalInfo>
            <HotelReservationIDs>
              <HotelReservationID ResID_Type="14" ResID_Value="PMS-1234567"/>
            </HotelReservationIDs>
          </ResGlobalInfo>
        </HotelReservation>
      </HotelReservations>
    </HotelNotifReport>
  </NotifDetails>
</OTA_NotifReportRQ>"""
    eventPersistenceHandler.persistRequest ( uuid, "test", "test", 100, "MESSAGE_SUBTYPE", requestPayload.getBytes ())
    
    String req =  eventPersistenceHandler.getPayloadAsString ( uuid, "requestpayloadxml" )
    
    assert requestPayload.equals ( req )
  }
  
  @Test
  def void persistError()
  {
    eventPersistenceHandler.persistError ( uuid, "source", null, 450, "Exception", new Throwable("This is an error"))
  }
  
  @Test
  def void persistResponse()
  {
    eventPersistenceHandler.persistResponseXml ( uuid ,  "<payload response/>".getBytes ())
  }
}