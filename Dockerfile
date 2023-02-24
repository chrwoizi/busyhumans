FROM tomcat:9.0.71-jdk17-corretto

# setup tomcat dns cache timeout (otherwise it's infinite which is bad for recaptcha)
#COPY ./deploy/setenv.sh /usr/local/tomcat/bin/setenv.sh

# setup tomcat admin credentials
#COPY ./deploy/tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml

COPY src/out/ROOT.war /usr/local/tomcat/webapps/

EXPOSE 8080
