# peppol-lime
An up-to-date implementation of the PEPPOL LIME protocol. 
It is based on the discontinued CIPA libraries.
No new functionality is added, but the existing dependencies are kept up-to-date.

This version of the LIME server uses AS2 to forward messages to foreign APs. START is no longer supported!

# Project layout
This project has the following sub-projects
  * `peppol-lime-api` with the most generic API and a lot of JAXB generated classes (JAR library)
  * `peppol-lime-client` is the client API for LIME. See the test class `MainLimeClient` for a fully working example application.
  * `peppol-lime-server` is the LIME server application. It is a web application and requires an application server like Tomcat or Jetty to run. 
  
# Building
To build the project you need at least Java 1.7.
Simply call `mvn clean install` on the commandline to build all projects.

#Configuration
The LIME server uses the file `lime-server.properties` for configuration. The default file resides in the folder `src/main/resources` of the `peppol-lime-server` project. You can change the path of the properties file by setting the system property `lime.server.properties.path` to the absolute path of the configuration file (e.g. by specifying `-Dlime.server.properties.path=/var/www/limeserver.properties` on Java startup). The name of the file does not matter, but if you specify a different properties file please make sure that you also specify an absolute path to the keystore!

Details of the configuration items:
*TODO*

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
