package com.its.openpath.module.pegasus

import org.codehaus.groovy.control.CompilerConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import org.springframework.jmx.export.annotation.ManagedResource
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode

/**
 * <code>ErrorCodeMap</code>
 * <p/>
 * Maps OPS error codes to Pegasus USW AMF and OTA XML error codes. This is injected to Handlers and Builders and they
 * use the {@link ErrorCodeMap#getAMFErrorCodeMapping} and {@link ErrorCodeMap#getOTAErrorCodeMapping} methods to
 * lookup error code mappings.
 * <p />
 * Uses the DSL script file /resources/conf/errorCodeMapping.txt to define error code mappings
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service
@ManagedResource('OPENPATH:name=/module/pegasus/ErrorCodeMap')
class ErrorCodeMap
{
  private static Logger sLogger = LoggerFactory.getLogger( ErrorCodeMap.class.name )

  private HashMap<Integer, ErrorCodeMapping> mAMFErrorCodeMap = new LinkedHashMap<Integer, ErrorCodeMapping>()
  private HashMap<Integer, ErrorCodeMapping> mOTAErrorCodeMap = new LinkedHashMap<Integer, ErrorCodeMapping>()


  /**
   * Constructor
   * @return
   */
  def ErrorCodeMap( )
  {
    sLogger.info "Instantiated ..."
  }

  /**
   * Invoked by the SpringFramework after the BeanFactory is created and all bean wiring is completed.
   */
  @PostConstruct
  def init( )
  {
    CompilerConfiguration compilerConfiguration = new CompilerConfiguration()
    compilerConfiguration.scriptBaseClass = ErrorMessageMappingScript.class.name

    def mappingDSLScript = this.class.getResourceAsStream( '/WEB-INF/classes/conf/errorCodeMapping.txt' )
    GroovyShell shell = new GroovyShell( this.class.classLoader,
      new Binding( [amfErrorCodeMap: mAMFErrorCodeMap, otaErrorCodeMap: mOTAErrorCodeMap] ),
      compilerConfiguration )

    shell.evaluate( mappingDSLScript )
    sLogger.info "*** Added [${mAMFErrorCodeMap.size()} AMF error code mappings and [${mOTAErrorCodeMap.size()} XML error code mappings ..."
  }

  /**
   * Return the AMF error code mapping for the requested OPS error code.
   * <p />
   * @param opsErrorCode - unique OPS error code (defined in opsCommon.proto)
   * @return ErrorCodeMapping - object that represents the mapping
   */
  def ErrorCodeMapping getAMFErrorCodeMapping( int opsErrorCode )
  {
    ErrorCodeMapping mapping = mAMFErrorCodeMap[opsErrorCode]
    if ( !mapping )
    {
      sLogger.error "*******************************************************************************************"
      sLogger.error "******* Couldn't find an AMF Error Code mapping for given OPS error code: [${opsErrorCode}]"
      sLogger.error "*******************************************************************************************"
      return mAMFErrorCodeMap[OpsErrorCode.ERROR_CODE_MAPPING_NOT_FOUND.number]
    }
    return mapping
  }

  /**
   * Return the OTA error code mapping for the requested OPS error code.
   * <p />
   * @param opsErrorCode - unique OPS error code (defined in opsCommon.proto)
   * @return ErrorCodeMapping - object that represents the mapping
   */
  def ErrorCodeMapping getOTAErrorCodeMapping( int opsErrorCode )
  {
    ErrorCodeMapping mapping = mOTAErrorCodeMap[opsErrorCode]
    if ( !mapping )
    {
      sLogger.error "*******************************************************************************************"
      sLogger.error "******* Couldn't find an OTA Error Code mapping for given OPS error code: [${opsErrorCode}]"
      sLogger.error "*******************************************************************************************"
      return mOTAErrorCodeMap[OpsErrorCode.ERROR_CODE_MAPPING_NOT_FOUND.number]
    }
    return mapping
  }

  /**
   * JMX method to return the contents of the internal Map that contains AMF error code mappings.
   * <p />
   * @return String - contents of the Map
   */
  @ManagedOperation
  def String dumpAMFMapping( )
  {
    sLogger.info "*** AMF Error Code Mappings: \n ${mAMFErrorCodeMap}"
    mAMFErrorCodeMap
  }

  /**
   * JMX method to return the contents of the internal Map that contains OTA error code mappings.
   * <p />
   * @return String - contents of the Map
   */
  @ManagedOperation
  def String dumpOTAMapping( )
  {
    sLogger.info "*** OTA Error Code Mappings: \n ${mOTAErrorCodeMap}"
    mAMFErrorCodeMap
  }
}

/**
 * Represents OPS to AMF and OTA error code mapping
 * <p />
 * Only the opscode and text properties/class attributes are defined, all others are dynamically added
 */
class ErrorCodeMapping
{
  def dynamicMethodsMap = [:]
  def opscode
  def text


  /**
   * Called by the Groovy runtime to handle properties not defined. Provides 'setter' functionality
   * <p />
   * @param name - name of the property
   * @param value - value of the property
   */
  def propertyMissing( String name, value )
  {
    dynamicMethodsMap[name] = value
  }

  /**
   * Called by the Groovy runtime to handle properties not defined. Provides 'getter' functionality
   * <p />
   * @param name - name of the property
   */
  def propertyMissing( String name )
  {
    dynamicMethodsMap[name]
  }

  /**
   * Custom method to format and print all the static and dynamic properties of the class.
   * <p />
   * @return String - Formatted string
   */
  @Override
  def String toString( )
  {
    StringBuilder str = new StringBuilder()
    str << "OPS Code: $opscode,"
    dynamicMethodsMap.each { String key, String val ->
      str << " field: ${key}, value: ${val}\n"
    }
    str.toString()
  }
}

/**
 * Custom DSL script representation
 */
abstract class ErrorMessageMappingScript
extends Script
{
  def logger
  def errorCodeMap


  /**
   * Constructor
   * <p />
   */
  def ErrorMessageMappingScript( )
  {
    logger = LoggerFactory.getLogger( ErrorMessageMappingScript.class.name )
  }

  /**
   * Invoked from the DSL script to start a new error code mapping definition category -- AMF or OTA
   * <p />
   * Sets up the correct internal Map to use so that the mappings are added to the correct internal Map when the DSL
   * scrip invokes the {@link #map} method.
   * <p />
   * @param mappingCategory - defines the error code category
   * @param closure - to invoke processing the DSL
   * @throws ScriptException - If the category is unrecognized
   */
  def mapErrorCode( mappingCategory, Closure closure )
  throws ScriptException
  {
    if ( mappingCategory.equalsIgnoreCase( "AMF" ) )
    {
      errorCodeMap = binding.amfErrorCodeMap
    }
    else if ( mappingCategory.equalsIgnoreCase( "OTA" ) )
    {
      errorCodeMap = binding.otaErrorCodeMap
    }
    else
    {
      throw new ScriptException( "The value of Error Code category can only be AMF or OTA" )
    }
    closure.delegate = this
    closure()
  }

  /**
   * Invoked by the DSL script to define an new error code mapping - AMF or OTA
   * <p />
   * @param mapping - named parameter Map that represents an error code mapping
   * @throws ScriptException - If the mandatory elements are not defined in the DSL script; if invalid elements are
   *                           defined in the DSL
   */
  def map( mapping )
  throws ScriptException
  {
    // Add all properties defined to the ErrorCodeMapping class as dynamic properties/class attributes
    mapping.each {
      ErrorCodeMapping.metaClass."$it.key"
    }

    if ( !mapping.opscode )
    {
      throw new ScriptException(
        "Couldn't parse the supplied error code mapping: [${mapping}], missing the mandatory 'opscode|amfcode|otacode' element(s)" )
    }
    errorCodeMap.put( new Integer( mapping.opscode ), new ErrorCodeMapping( mapping ) )
  }
}

