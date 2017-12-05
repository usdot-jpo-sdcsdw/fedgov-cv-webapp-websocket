FROM jetty:9.3.11-jre8

COPY target/fedgov-cv-webapp-websocket-1.0.0-SNAPSHOT.war /opt/fedgov-cv-webapp-websocket.war

COPY fedgov-cv-webapp-websocket.context.xml $JETTY_BASE/webapps/
