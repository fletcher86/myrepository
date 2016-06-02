import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest

Logger sLogger = LoggerFactory.getLogger("ProcessRequest")

HttpServletRequest req = request
sLogger.info( "**** Processing request ...: ${req.getContentLength()}" )

//def input = req.getInputStream()
//def output =  new ByteArrayOutputStream( 1024 )
//
//output << input
//input.close()
//output.close()

//sLogger.info( "**** Processing request done ... \n ${output.toString("UTF-8")}" )
sLogger.info( "**** Processing request done ... \n ${params}" )
sLogger.info( "**** Processing request done ... \n ${req.dump()}" )
