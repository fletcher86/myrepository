package com.its.openpath.module.opsari.reztrip.resource
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.model.messaging.ops.OpsTxnType

/**
 * <code>ReservationsManagementResource.groovy</code>
 * <p/>
 * REST Resource handles reservation pull request from site minder issued by reztrip
 * <p/>
 * @author kent
 * @since Aug 21, 2012
 */
@Service("ReservationsManagementResource")
@ManagedResource('OPENPATH:name=/module/opsari/reztrip/resource/ReservationsManagementResource')
@Path("/reservations")
class ReservationsManagementResource
extends AbstractResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReservationsManagementResource.class.name )
  
  /**
   * Constructor
   */
  ReservationsManagementResource ()
  {
    sLogger.info 'Instantiated ...'
  }
  
  /* (non-Javadoc)
   * @see com.its.openpath.module.opsari.reztrip.resource.AbstractResource#processRequest(javax.servlet.http.HttpServletRequest)
   */
  @POST
  @Path('/pull')
  @Consumes("application/json")
  @Produces("application/json")
  def Response processRequest( @Context HttpServletRequest servletRequest )
  {
    OpsTxnType type = OpsTxnType.RESERVATION_MGMT
    def responseJSON = super.processRequest ( servletRequest, type, false)
    javax.ws.rs.core.Response responseObj = Response.ok( responseJSON ).status( 200 ).build()
    return responseObj
  }
}
