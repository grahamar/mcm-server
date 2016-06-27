# mcm-server
Mock Cloud Messaging Server - Mocks GCM &amp; APN servers, so you can throw whatever amount of garbage you so desire at it.

## Usage

    git clone git@github.com:grahamar/mcm-server.git
    cd mcm-server
    sbt "runMain io.grhodes.mcm.server.Main"

### Client Setup:

#### APN OkHttp Example

You'll need to use the `mockapn.keystore` self-signed cert for testing with APN, or generate your own, see below:

```
import java.io.{File, FileInputStream}
import java.security.KeyStore
import java.security.cert.{CertificateException, X509Certificate}
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl._

import okhttp3._
import org.jivesoftware.smack.util.TLSUtils.AcceptAllTrustManager
import resource.managed

...

val (sslCtx, trustMngr) = createSSLContext()

val builder = new OkHttpClient.Builder()
  .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
  .sslSocketFactory(sslCtx, trustMngr)
  .hostnameVerifier(new NoHostVerification())
  .build()


private def createSSLContext(): (SSLSocketFactory, X509TrustManager) = {
  val ks = KeyStore.getInstance("PKCS12")
  for(in <- managed(new FileInputStream(certificate))) {
    ks.load(in, password.toCharArray)
  }

  val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
  keyManagerFactory.init(ks, password.toCharArray)

  val trustManager = new AcceptAllTrustManager()

  val sslCtx = SSLContext.getInstance("TLS")
  sslCtx.init(keyManagerFactory.getKeyManagers, Array(trustManager), null)
  sslCtx.getSocketFactory -> trustManager
}
```

#### GCM Smack Example

```
import java.security.SecureRandom
import javax.net.ssl.{SSLContext, TrustManager}

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode
import org.jivesoftware.smack.provider.ProviderManager
import org.jivesoftware.smack.tcp.{XMPPTCPConnection, XMPPTCPConnectionConfiguration}
import org.jivesoftware.smack.util.TLSUtils
import org.jivesoftware.smack.util.TLSUtils.AcceptAllTrustManager

...

val context = SSLContext.getInstance(TLSUtils.PROTO_TLSV1_2)
context.init(null, Array[TrustManager](new AcceptAllTrustManager()), new SecureRandom())

val builder = XMPPTCPConnectionConfiguration.builder()
  .setServiceName("localhost")
  .setHost("localhost")
  .setPort(9001)
  .setSendPresence(false)
  .setSecurityMode(SecurityMode.ifpossible)
  .setCompressionEnabled(false)
  .setCustomSSLContext(context)
  .setDebuggerEnabled(false)

TLSUtils.disableHostnameVerificationForTlsCertificicates(builder)

val xmppClient = new XMPPTCPConnection(builder.build())
xmppClient.connect()
xmppClient.login(s"888888888888@localhost", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
xmppClient
```


### Default Settings

#### APN
Port: 9000

#### GCM
Port: 9001


### Generating your own self-signed certs

```
keytool -genkey -alias mockgcm -keysize 2048 -validity 365 -keyalg RSA -dname "CN=mockgcm.com,O=push,L=SF,ST=CA,C=US" -keypass password -storepass password -keystore mockgcm.keystore

keytool -genkey -alias mockapn -keysize 2048 -validity 365 -keyalg RSA -dname "CN=push.com,O=push,L=SF,ST=CA,C=US" -keypass password -storepass password -keystore mockapn.keystore -storetype pkcs12

```