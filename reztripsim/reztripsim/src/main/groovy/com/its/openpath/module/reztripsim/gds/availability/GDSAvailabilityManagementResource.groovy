package com.its.openpath.module.reztripsim.gds.availability

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired

/**
 * <code>GDSAvailabilityManagementResource</code>
 * <p/>
 * The RESTful Resource that handles Availability related requests such as Area Availability and Single Property
 * Availability Searches. Delegates processing to respective handler classes and returns
 * the {@link Response} received.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("GDSAvailabilityManagementResource")
@ManagedResource('OPENPATH:name=/module/reztripsim/gds/availabiity/GDSAvailabilityManagementResource')
@Path("/availability")
class GDSAvailabilityManagementResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( GDSAvailabilityManagementResource.class.name )

  @Autowired(required = true)
  private GDSAreaAvailabilityResourceHandler mAreaAvailabilityHandler

  @Autowired(required = true)
  private GDSSinglePropertyAvailabilityResourceHandler mSinglePropertyAvailabilityHandler


  /**
   * Constructor
   * <p />
   * @return
   */
  def GDSAvailabilityManagementResource( )
  {
    sLogger.info "instantiated ..."
  }

  /**
   * Handle Area Availability search requests. This Resource can be accessed from:
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/availability/areaAvailability
   * <p />
   * @param servletRequest - Contains the incoming service request
   * @return Response - The Availability Search response built
   */
  @POST
  @Path('/areaAvailability')
  @Consumes("application/json")
  @Produces("application/json")
  def Response areaAvailabilitySearch( @Context HttpServletRequest servletRequest )
  {
    return mAreaAvailabilityHandler.search( servletRequest )
  }


  /**
   * Handle Single Property Availability search requests. This Resource can be accessed from:
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/availability/singleProperty
   * <p />
   * @param servletRequest - Contains the incoming service request
   * @return Response - The Availability Search response built
   */
  @POST
  @Path('/singleProperty')
  @Consumes("application/json")
  @Produces("application/json")
  def Response singlePropertyAvailabilitySearch( @Context HttpServletRequest servletRequest )
  {
    return mSinglePropertyAvailabilityHandler.search( servletRequest )
  }

}
