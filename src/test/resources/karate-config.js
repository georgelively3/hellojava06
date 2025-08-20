function fn() {
  var env = karate.env; // get system property 'karate.env'
  karate.log('karate.env system property was:', env);
  
  if (!env) {
    env = 'dev';
  }
  
  // Get server port from system property (set by Spring Boot test)
  var serverPort = karate.properties['karate.server.port'] || '8080';
  var baseUrl = 'http://localhost:' + serverPort;
  
  var config = {
    env: env,
    baseUrl: baseUrl,
    wireMockUrl: 'http://localhost:8089'
  };
  
  if (env == 'dev' || env == 'mountebank') {
    config.baseUrl = baseUrl;
    config.wireMockUrl = 'http://localhost:8089';
  }
  
  karate.log('karate config:', config);
  karate.log('using baseUrl:', config.baseUrl);
  return config;
}
