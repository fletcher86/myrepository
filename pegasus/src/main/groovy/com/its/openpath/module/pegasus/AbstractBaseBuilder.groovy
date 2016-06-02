package com.its.openpath.module.pegasus

import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.util.InvocationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * <code>AbstractBaseBuilder</code>
 * <p/>
 * Base class for all 'Builder' classes. Contains methods common for all subclasses.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */
abstract class AbstractBaseBuilder
{
  private static Logger sLogger = LoggerFactory.getLogger( AbstractBaseBuilder.class.name )


  /**
   *  Subclasses implement this method to build the Service Request Message that will be submitted to the OpenPath
   *  Service Broker. This method is responsible for building the Service Request in JSON based on a service request
   *  received from an external entity in XML or other formats. The built message is expected to be set in the
   * {@link InvocationContext#setOpenPathRequestData}
   *  <p />
   * @return boolean - TRUE = successfully built the Service Request and set in the context
   */
  abstract boolean buildRequestToOpenPathFromExternal( )

  /**
   * Subclasses must implement this method to build a Service Response Message to be sent to an external entity. The
   * Response Message (XML or other formats) is constructed using the OpenPath Response Message received from the
   * Service Broker. The built message is expected to be set in the
   * {@link InvocationContext#setExternalSystemResponseData}
   * <p />
   * @return boolean - TRUE = successfully built the Service Response and set in the context
   */
  abstract boolean buildResponseToExternalFromOpenPath( )

  /**
   * Build an Error Response message to be sent back to Pegasus USW and set it in the InvocationContext.
   * <p />
   * @param code - Unique OPS Error Code
   * @param optionalText - Optional text to include
   * @throws IllegalStateException - If failed to build an error response
   */
  abstract buildErrorResponse( OpsErrorCode errorCode, String optionalText )
  throws IllegalStateException

}