The module can be used to create karma web plugins.
The architecture goes as follows:
    karma backend -> rest api -> angular or/and existing karma frontend

## limitation of plugins

1.  karma can interact with a plugin using ONLY restapi, and it doesn't share the same CONTEXT with the app.
2.  It has an isolated UI
3.  workflow doesn't involve executing karma commands.

Where each plugin can consist of rest api developed using jakarta and frontend using angular.
Rest apis can be used to host business logic for these plugins.
Depending on the requirement, existing karma frontend can be used to consume the api, or a separate angular application can be developed.
The angular app is build using maven plugin. have a look at batch-mode plugin for an example.

Once the plugin is ready, it can be added to the karma web both at build time or run time as it creates a war packaging.
Jetty or tomcat can be used to deploy the war when using standalone karmalinker.
docker can be used to build the app with plugins for dockerized version of karmalinker.

Example of adding plugin to karma-web:
1. karma-web/pom.xml: add the war file of plugin to the karma-web pom file
    - add to jetty-deploy profile (see existing added plugins)
    - add to tomcat-deploy profile (see existing added plugins)
2. karma-web/src/main/java/edu.isi.karma.webserver: initialize the plugin (see existing added plugins)
