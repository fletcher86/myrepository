package com.its.openpath.module.opscommon.event.persistence

import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import com.its.openpath.module.opscommon.comm.bus.IMessageBus
import com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.OpsStatuses
import com.its.openpath.module.opscommon.util.TimeUUIDUtils
import com.netflix.astyanax.Keyspace
import com.netflix.astyanax.MutationBatch
import com.netflix.astyanax.connectionpool.OperationResult
import com.netflix.astyanax.model.Column
import com.netflix.astyanax.model.ColumnFamily
import com.netflix.astyanax.model.ColumnList
import com.netflix.astyanax.model.CqlResult
import com.netflix.astyanax.model.Row
import com.netflix.astyanax.model.Rows
import com.netflix.astyanax.serializers.StringSerializer
import com.netflix.astyanax.serializers.UUIDSerializer

/**
 * EventPersistenceHandler - Class persists a given event payload represented as a String. The argument is persisted in cassandra
 * in the openpath keyspace, and the column family is 'txnlog'
 * User: kent
 * Date: 7/3/12
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class AbstractEventPersistenceHandler
extends AbstractOpenPathPersistenceContext
implements  IMessageBusSubscriber
{
  
  /*
   * TXNLOG is the columnfamily this class performs CRUD operations on  
   */
  static final ColumnFamily<UUID, String> TXNLOG = new ColumnFamily<UUID, String>(
  "txnlog",                // Column Family Name
  UUIDSerializer.get(),    // Key Serializer
  StringSerializer.get())  // Column Serializer
  
  /*
   * ERRLOG is the columnfamily this class performs CRUD operations on  
   */
  static final ColumnFamily<UUID, String> ERRLOG = new ColumnFamily<UUID, String>(
  "errlog",                // Column Family Name
  UUIDSerializer.get(),    // Key Serializer
  StringSerializer.get())  // Column Serializer
  
  /**
   * Message bus consumer.  onMessage implemented in concrete classes
   */
  @Autowired(required = true)
  protected IMessageBus mOpsMessageBus
  
  
  /**
   * Columns in the errlog time to live
   */
  private @Value("#{runtimeProperties['errlog.ttl']}")
  Integer errLogTTL
  
  /**
   * Columns in the txnlog time to live
   */
  private @Value("#{runtimeProperties['txnlog.ttl']}")
  Integer txnLogTTL
  
  /**
   * Max number of retries for a transaction
   */
  private @Value("#{runtimeProperties['txnlog.maxretries']}")
  Integer maxRetries
  
  /**
   * Get list of UUID row identifies given the status
   *
   * @param responsibleNodeId
   *          - The node responsible for sending the message. The saf queue will be queried based on responsible node id
   *          for delivering the message. Only one thread should call this method to prevent race conditions
   */
  public List<UUID> getRowIdsWithStatus(String status)
  {
    /*
     * HARDCODE maxRetries to 5 for now, property injection isn't working.  Needs some OSGI trouble shooting
     */
    maxRetries = 5
    String cql="select * from txnlog where status='${status}' and numretries<${maxRetries}"
    sLogger.debug(cql)
    Keyspace keyspace = super.context.getEntity()
    OperationResult<CqlResult<UUID, String>> result =  keyspace.prepareQuery(TXNLOG)
        .withCql ( cql )
        .execute()
    Rows rows= result.getResult ().getRows ()
    
    List<UUID> rtnList = new ArrayList()
    
    int size = rows.size()
    for (int i=0;i< size; i++)
    {
      Row r = rows.getRowByIndex(i)
      ColumnList<String> cols =  r.getColumns()
      UUID uuid = r.getKey()
      logInfoStatement ( "row of status = [${status}] retrieved", uuid )
      rtnList.add(uuid)
    }
    return rtnList
  }
  
  public void persistError(UUID uuid, String source, String destination, Integer messageType, String messageSubType, byte [] errorPayload)
  {
    logInfoStatement("PERSIST ERROR CONDITION source: [" + source + "] destination: [" + destination + "]", uuid)
    
    try
    {
      Keyspace keyspace = context.getEntity()
      
      MutationBatch m = keyspace.prepareMutationBatch()
      
      m.withRow(ERRLOG, uuid)
          .putColumn("source", source?:"", errLogTTL)
          .putColumn("destination", destination?:"", errLogTTL)
          .putColumn("msgtype", messageType?:"", errLogTTL)
          .putColumn("msgsubtype", messageSubType?:"", errLogTTL )
          .putColumn("time", TimeUUIDUtils.getTimeFromUUID (uuid ), errLogTTL)
          .putColumn("errormsgtrace", errorPayload, errLogTTL)
      
      OperationResult<Void> result = m.execute()
      
      if(sLogger.isDebugEnabled ())
        logDebugUpdateStatement ( " PERSISTING ERROR ", uuid,  new String(errorPayload), true )
    }
    catch (Throwable x)
    {
      logErrorStatement ( "**EXCEPTION THROWN** PERSISTING ERROR", uuid,  new String(errorPayload), x , true)
    }
  }
  
  /**
   * Persist the xml request payload
   *<p />
   *@param uuid
   *@param source
   *@param destination
   *@param messageType
   *@param payload
   */
  public void persistRequest(UUID uuid, String source, String destination, Integer messageType, String messageSubType, payloadColumnName, byte [] payload)
  {
    logInfoStatement("PERSIST REQUEST XML source: [" + source + "] destination: [" + destination + "]", uuid)
    try
    {
      Keyspace keyspace = context.getEntity()
      
      MutationBatch m = keyspace.prepareMutationBatch()
      
      m.withRow(TXNLOG, uuid)
          .putColumn("source", source?:"", txnLogTTL)
          .putColumn("destination", destination?:"", txnLogTTL)
          .putColumn("txntype", messageType?:"", txnLogTTL)
          .putColumn("txnsubtype", messageSubType?:"", txnLogTTL)
          .putColumn(payloadColumnName, payload?:"".getBytes (), txnLogTTL)
          .putColumn ("numretries", 0, txnLogTTL )
          .putColumn("responsibleNodeId", "PUT-NODE-ID-HERE", txnLogTTL)
          .putColumn("time", TimeUUIDUtils.getTimeFromUUID (uuid ), txnLogTTL)
      
      OperationResult<Void> result = m.execute()
      
      if(sLogger.isDebugEnabled ())
        logDebugUpdateStatement ( " REQUEST ${payloadColumnName}", uuid, new String(payload), true )
    }
    catch (Throwable e)
    {
      logErrorStatement ( "PERSISTING ${payloadColumnName} REQUEST PAYLOAD", uuid, new String(payload), e , false)
    }
  }
  
  /**
   * Persist the payload byte array by the designated uuid and columnName
   *<p />
   *@param uuid
   *@param columnName
   *@param payload
   */
  public void persistPayload(UUID uuid, String columnName, byte [] payload)
  {
    logInfoStatement("PERSISTING PAYLOAD", uuid)
    try
    {
      Keyspace keyspace = context.getEntity()
      
      MutationBatch m = keyspace.prepareMutationBatch()
      
      m.withRow(TXNLOG, uuid)
          .putColumn(columnName, payload?:"".getBytes (), txnLogTTL)
      
      OperationResult<Void> result = m.execute()
      
      if(sLogger.isDebugEnabled ())
        logDebugUpdateStatement ( " PERSISTED PAYLOAD COLUMNNAME=${columnName}", uuid, new String(payload), true )
    }
    catch (Throwable e)
    {
      logErrorStatement ( "PERSISTING PAYLOAD COLUMNNAME=${columnName}", uuid, new String(payload), e , false)
    }
  }
  
  /**
   * Get payload as String given the named column.  Column name must be blob type, i.e., requestpayloadxml, responsepayloadxml, requestpayloadjson, responsepayloadjson
   *<p />
   *@param uuid UUID the row key
   *@param columnName String - one of the following strings requestpayloadxml, responsepayloadxml, requestpayloadjson, responsepayloadjson
   *@return String payload
   */
  public String getPayloadAsString(UUID uuid, String columnName)
  {
    String payloadStr = null
    try
    {
      payloadStr = new String (getPayloadAsByteArray(uuid, columnName))
      if(sLogger.isDebugEnabled ())
      {
        sLogger.debug("********  GET PAYLOAD AS STRING WHERE UUID=[${uuid}] COLUMN_NAME=[${columnName}] payloadStr = [${payloadStr}] ******" )
      }
    }
    catch (Throwable e)
    {
      logErrorStatement ( "UNABLE TO GET PAYLOAD AS STRING COLUMN_NAME=[${columnName}]", uuid, payloadStr, e , false)
      throw e
    }
    return payloadStr
  }
 
 
  /**
   * Increment the integer value of the column designated by the row key, uuid and the columnName
   *<p />
   *@param uuid
   *@param columnName
   */
  public void incrementIntColumn(UUID uuid, String columnName)
  {
    logInfoStatement("UPDATE INTEGER COLUMN=[${columnName}]", uuid)
    try
    {
      
      Integer currentVal = getIntegerColumn( uuid, columnName)
      Integer updatedVal = currentVal +1;
      
      Keyspace keyspace = context.getEntity()
      
      MutationBatch m = keyspace.prepareMutationBatch()
      
      m.withRow(TXNLOG, uuid)
          .putColumn(columnName, updatedVal, null)
      
      OperationResult<Void> result = m.execute()
      
      if(sLogger.isDebugEnabled ())
        logDebugUpdateStatement ( " INCR=[${columnName}] TO [${updatedVal}] PREV [${currentVal}]", uuid, columnName, true )
    }
    catch (Throwable e)
    {
      logErrorStatement ( "UNABLE TO INCREMENT INTEGER COLUMN", uuid, columnName, e, true )
    }
  }
  
  
  
  /**
   * Get the integer value designated by the row key, uuid and the columnName 
   *<p />
   *@param uuid UUID row id
   *@param columnName String column name
   *@return Integer value of the column
   */
  public Integer getIntegerColumn(UUID uuid, String columnName)
  {
    Integer returnInt = null
    try
    {
      Keyspace keyspace = super.context.getEntity()
      OperationResult<ColumnList<String>> result = keyspace.prepareQuery(TXNLOG)
          .getKey(uuid)
          .execute()
      
      ColumnList<String> columns = result.getResult()
      Column<Integer> intValue = columns.getColumnByName(columnName)
      
      returnInt = intValue?.getIntegerValue ()?:0
      if(sLogger.isDebugEnabled ())
        sLogger.debug("GET INTEGER VALUE FOR ROW UUID = [${uuid}] COLUMN = [${columnName}] INTVAL = ${returnInt}" )
      return returnInt?:0
    }
    catch (Throwable e )
    {
      logErrorStatement ( "UNABLE TO GET INTEGER FROM COLUMN", uuid, columnName, e , true)
      throw e
    }
  }
  
  /**
   * Get payload as byte[] given the named column.  Column name must be blob type, i.e., requestpayloadxml, responsepayloadxml, requestpayloadjson, responsepayloadjson
   *<p />
   *@param uuid UUID the row key
   *@param columnName String - one of the following strings requestpayloadxml, responsepayloadxml, requestpayloadjson, responsepayloadjson
   *@return byte [] payload
   */
  public byte [] getPayloadAsByteArray(UUID uuid, String columnName)
  {
    Keyspace keyspace = super.context.getEntity()
    byte [] payloadBytes = null
    try
    {
      OperationResult<ColumnList<String>> result = keyspace.prepareQuery(TXNLOG)
          .getKey(uuid)
          .execute()
      
      ColumnList<String> columns = result.getResult()
      Column<byte []> payloadBytesCol = columns.getColumnByName(columnName)
      payloadBytes = payloadBytesCol.getByteArrayValue()
      if(sLogger.isDebugEnabled ())
        sLogger.debug("GET PAYLOAD AS BYTE [] where UUID = [${uuid}] payloadBytes of type byte [] = ${payloadBytes}" )
      return payloadBytes;
    }
    catch (Throwable e)
    {
      logErrorStatement ( "UNABLE TO GET PAYLOAD AS BYTE ARRAY", uuid, payloadBytes, e , false)
      throw e
    }
  }
 
  /**
   * Update transaction status 
   *<p />
   *@param uuid the transaction aka correlationid
   *@param status The status of the txn
   */
  public void updateTransactionStatus(UUID uuid, String status)
  {
    logInfoStatement("UPDATE TRANSACTION STATUS=[${status}]", uuid)
    try
    {
      Keyspace keyspace = context.getEntity()
      
      MutationBatch m = keyspace.prepareMutationBatch()
      
      Integer ttl = null;
      if(!status.equals(OpsStatuses.SUCCESS))
      {
        ttl = txnLogTTL
      }
      
      m.withRow(TXNLOG, uuid)
          .putColumn("status", status, ttl)
      
      OperationResult<Void> result = m.execute()
      
      if(sLogger.isDebugEnabled ())
        logDebugUpdateStatement ( " STAT=${status}", uuid, status, true )
    }
    catch (Throwable e)
    {
      logErrorStatement ( "UPDATING TRANSACTION STATUS", uuid, status, e, false )
    }
  }
  
  /**
   * Log Info Statement using a method designed for reusablity throughout this class
   *<p />
   *@param msg
   *@param uuid
   */
  public void logInfoStatement(String msg, UUID uuid)
  {
    sLogger.info "${msg} where correlationId: [${uuid}]"
  }
  
  /**
   * Log Debug Statement using a method designed for reusablity throughout this class
   *<p />
   *@param msg
   *@param uuid
   *@param payload
   *@param logPayload
   */
  public void logDebugUpdateStatement(String msg, UUID uuid, String payload, boolean logPayload)
  {
    String p = "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
    String str = "+++Updated "+msg+" Event where uuid = ["+uuid.toString ()+"] timestamp = ["+new Date(TimeUUIDUtils.getTimeFromUUID (uuid ))+"] ++++++++++++++"
    if(logPayload)
      str = str + " payload = ["+payload+"]"
    sLogger.debug p
    sLogger.debug str
    sLogger.debug p
  }
  
  /**
   * Log Error Statement using a method designed for reusablity throughout this class
   *<p />
   *@param msg
   *@param uuid
   *@param payload
   *@param e
   *@param logPayload
   */
  public void logErrorStatement(String msg, UUID uuid, String payload, Throwable e, boolean logPayload)
  {
    String p = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    String str = "XXXXXXXXXXX  ERROR  [${msg}] Event  XXXXXXXXXXX where uuid = [${uuid}] timestamp = ["+new Date(TimeUUIDUtils.getTimeFromUUID (uuid ))+"] XXXXXX"
    if(logPayload)
      str = str + " payload = ["+payload+"]"
    sLogger.info p
    sLogger.error str, e
    sLogger.info p
  }
  
  @Override
  public String getSubscriberId()
  {
    return this.class.name
  }
  
}