#!/bin/sh
if [ "$#" -ne 3 ];then
  PLAYAPP_HOME=..
  HTTP_PORT=9000
  HTTPS_PORT=9443
else
  PLAYAPP_HOME=$1
  HTTP_PORT=$2
  HTTPS_PORT=$3
fi

# Checking if we are enabling debug via port 8000
if [ "$DEBUG" == "1" ]; then
  JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
else
  JAVA_DEBUG_OPTS=""
fi

PASSWORD=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
JKS_FILE="$PLAYAPP_HOME/conf/playapp.jks"
SERVER_CERT_ALIAS="PlayApp-StreamServer-Certificate"
SECRETS_FOLDER="/tmp/playappcert"
if [ -d "$SECRETS_FOLDER" ]; then
  echo 'We have what we need to configure https'
  PRIVATE_KEY_PEM="$SECRETS_FOLDER/tls.key"
  HTTPS_CERT_PEM="$SECRETS_FOLDER/tls.crt"
  P12_FILE="$PLAYAPP_HOME/conf/playapp.p12"
  openssl pkcs12 -export -in "$HTTPS_CERT_PEM" -inkey "$PRIVATE_KEY_PEM" -name "$SERVER_CERT_ALIAS" -out "$P12_FILE" -password pass:$PASSWORD
  keytool -genkey -keyalg RSA -alias deleteme -keystore $JKS_FILE -storepass "$PASSWORD" -keypass "$PASSWORD" -dname "CN=deleteme"
  keytool -delete -alias deleteme -keystore $JKS_FILE -storepass $PASSWORD

  keytool -v -importkeystore -srckeystore $P12_FILE -srcstoretype pkcs12 -srcstorepass $PASSWORD -destkeystore $JKS_FILE -deststoretype jks -deststorepass $PASSWORD
  rm "$P12_FILE"
else
  echo 'Will generate a self-signed certifcate....'
  # EXTERNAL_HOSTNAME is an environment variable set by the Marathon App
  keytool -genkey -alias $SERVER_CERT_ALIAS -keyalg RSA -keysize 2048 -dname "CN=$EXTERNAL_HOSTNAME,O=SelfSignedCertificate" -keystore $JKS_FILE -storepass $PASSWORD -keypass $PASSWORD -validity 7300
fi
unset PRIVATE_KEY
export CLASSPATH="${PLAYAPP_HOME}/lib/*"

java $JAVA_DEBUG_OPTS -Dhttps.port=$HTTPS_PORT -Dhttp.port=$HTTP_PORT -Dplay.crypto.secret=$PASSWORD -Dplay.server.https.keyStore.path="$JKS_FILE" -Dplay.server.https.keyStore.password=$PASSWORD -Dplay.server.https.keyStore.type=jks -Dplayapp.home=$PLAYAPP_HOME -DAGSSERVER=$PLAYAPP_HOME play.core.server.ProdServerStart $PLAYAPP_HOME
