FROM tomcat:8.5-jdk11
ENV CATALINA_HOME /usr/local/tomcat
COPY karma-web-0.0.1-SNAPSHOT.war $CATALINA_HOME/webapps/ROOT.war
# Additional services
COPY cleaningService.war $CATALINA_HOME/webapps/cleaningService.war
COPY cluster-1.0.war $CATALINA_HOME/webapps/clusterService.war
COPY openrdf-workbench.war $CATALINA_HOME/webapps/openrdf-workbench.war
COPY openrdf-sesame.war $CATALINA_HOME/webapps/openrdf-sesame.war
COPY ckan-client.war $CATALINA_HOME/webapps/ckan-client.war
COPY batch-mode.war $CATALINA_HOME/webapps/batch-mode.war
# Additional scripts
COPY pyAddons /root/pyAddons
# Configuration
RUN sed -i 's/8080/7000/' $CATALINA_HOME/conf/server.xml
EXPOSE 7000
CMD mkdir -p /root/karma/python/ && cp -a /root/pyAddons/. /root/karma/python/ && catalina.sh run
