FROM jetty:9.3.11-jre8

RUN mkdir -p /opt/logs && chmod 777 /opt/logs

COPY target/fedgov-cv-webapp-websocket-1.0.0-SNAPSHOT.war /opt/fedgov-cv-webapp-websocket.war

COPY fedgov-cv-webapp-websocket.context.xml $JETTY_BASE/webapps/

COPY libper-xer-codec.so /usr/lib/libper-xer-codec.so
