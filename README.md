# peppol-lime
An up-to-date implementation of the PEPPOL LIME protocol. 
It is based on the discontinued CIPA libraries.
No new functionality is added, but the existing dependencies are kept up-to-date.

This version of the LIME server uses AS2 to forward messages to foreign APs. START is no longer supported!

Latest version: **3.0.0** as of 2015-10-09

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
  * **`sml.id`**: the ID of the SML to use. Maybe one of the following: `digitprod` for the DIGIT production SML, `digittest` for the DIGIT test SMK or `local` for a locally running SML on `http://localhost:8080`. If not specified, the default is `digitprod`.
  * **`as2.keystore.path`**: the path to the keystore for the AS2 message sending. Must be of type PKCS12 and must be a writable path, as the keystore is modified during runtime. If it is a relative path, it is relative to the web application (relative to `src/main/resources` in development mode, depending on the application server in production mode). It is preferred that this is an **absolute path to the keystore file**.
  * **`as2.keystore.password`**: the password needed to access the keystore.
  * **`as2.sender.keyalias`**: the alias of the sender key in the key store. The password for the key must be the same as the key for the whole keystore.
  * **`as2.sender.id`**: the AS2 ID of the sender. For PEPPOL this MUST be the common name contained in the certificate (`APP_....`).
  * **`as2.sender.email`**: the AS2 email address of the sender.
  * **`as2.sign.algorithm`**: the signing algorithm to use. Must be one of the following: `md5`, `sha1`, `sha-256`, `sha-384` or `sha-512`. If none of these values is specified, the value defaults to `sha1`.
  * **`lime.storage.path`**: the absolute directory where the LIME inbox directory should be created. If this property is not defined the path is defaulted to the value of `ServletContext.getRealPath ("/")`.
  * **`lime.service.url`**: the absolute URL of LIME service how it is publicly accessible. If this property is not defined or empty the URL is dynamically build based on the current `HttpServletContext`.  If this property is defined, it must end with `/limeService` as this is the Webservice local name.
   

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
