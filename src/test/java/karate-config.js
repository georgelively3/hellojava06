function fn() {
  var config = {
    baseUrl: 'http://localhost:8080'
  };
  
  // Check if we have a dynamic port from Spring Boot test
  var serverPort = java.lang.System.getProperty('karate.server.port');
  if (serverPort) {
    config.baseUrl = 'http://localhost:' + serverPort;
  }
  
  karate.log('Base URL configured as:', config.baseUrl);
  
  return config;
}
