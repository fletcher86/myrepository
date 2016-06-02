package com.its.openpath.module.opscommon.event.persistence

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import com.its.openpath.module.opscommon.comm.bus.IMessageBus
import com.netflix.astyanax.AstyanaxContext
import com.netflix.astyanax.Keyspace
import com.netflix.astyanax.connectionpool.NodeDiscoveryType
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl
import com.netflix.astyanax.thrift.ThriftFamilyFactory

/**
 * <code>OpenPathPersistenceContext.groovy</code>
 * <p/>
 * Class that initializes the astyanax persistence context
 * <p/>
 * @author kent
 * @since Jul 18, 2012
 */
public abstract class AbstractOpenPathPersistenceContext
{
  protected static AstyanaxContext<Keyspace> context
  
  private static Logger sLogger = LoggerFactory.getLogger(AbstractOpenPathPersistenceContext.getName())
  private @Value("#{runtimeProperties['cassanadra.seeds']}")
  String seeds
  
  @PostConstruct
  public void init()
  {
    String keyspace = "openpath"
    String cluster = "openpath"
    
    if(seeds==null)
    {
      //seeds="cass-a.ttaws.com:9160,cass-b.ttaws.com:9160,cass-c.ttaws.com:9160"
      seeds="hvm1:9160,hvm2:9160,hvm3:9160"
    }
    
    
    sLogger.info "Initializing EventPersistenceHandler with cassandra db using astyanax api where cluster: [" + cluster +
        "] keyspace: [" + keyspace + "] seeds: [" + seeds + "]"
    
    context = new AstyanaxContext.Builder()
        .forCluster(cluster)
        .forKeyspace(keyspace)
        .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
        .setDiscoveryType(NodeDiscoveryType.NONE))
        .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("OpenPathConnectionPool")
        .setPort(9160)
        .setMaxConnsPerHost(1)
        .setSeeds(seeds))
        .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
        .buildKeyspace(ThriftFamilyFactory.getInstance())
    
    context.start()
    sLogger.info "Successfully initialized EventPersistenceHandler with cassandra db using astyanax api where cluster: [" +
        cluster +
        "] keyspace: [" + keyspace + "] seeds: [" + seeds + "]"
  }
}
