# mcm-server
Mock Cloud Messaging Server - Mocks GCM &amp; APN servers



```
keytool -genkey -alias mockgcm -keysize 2048 -validity 365 -keyalg RSA -dname "CN=mockgcm.com,O=push,L=SF,ST=CA,C=US" -keypass password -storepass password -keystore mockgcm.keystore
keytool -genkey -alias mockapn -keysize 2048 -validity 365 -keyalg RSA -dname "CN=push.com,O=push,L=SF,ST=CA,C=US" -keypass password -storepass password -keystore mockapn.keystore -storetype pkcs12

```