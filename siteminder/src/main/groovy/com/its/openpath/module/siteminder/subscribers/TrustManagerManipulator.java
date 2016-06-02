package com.its.openpath.module.siteminder.subscribers;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrustManagerManipulator
{
  private static final Logger sLogger = LoggerFactory.getLogger( TrustManagerManipulator.class.getCanonicalName ()  );

  private static TrustManager[] trustAllCerts;
  
  public static void allowAllSSL()
  {
    
    if ( trustAllCerts == null )
    {
      trustAllCerts = new TrustManager[] {
        new X509TrustManager () {
          public java.security.cert.X509Certificate[] getAcceptedIssuers()
          {
            return null;
          }
          
          public void checkClientTrusted(
            java.security.cert.X509Certificate[] certs, String authType )
          {
          }
          
          public void checkServerTrusted(
            java.security.cert.X509Certificate[] certs, String authType )
          {
          }
        }
      };
      
      // Install the all-trusting trust manager
      try
      {
        SSLContext sc = SSLContext.getInstance ( "SSL" );
        sc.init ( null, trustAllCerts, new java.security.SecureRandom () );
        HttpsURLConnection.setDefaultSSLSocketFactory ( sc.getSocketFactory () );
        
     // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
              return true;
            }
        };
        
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

      }
      catch ( Exception e )
      {
        sLogger.error ( "*Error trying to trust all certs*", e );
      }
    }
  }
}