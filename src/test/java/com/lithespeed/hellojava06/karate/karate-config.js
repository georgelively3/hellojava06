function fn() {
  var env = karate.env; // get system property 'karate.env'
  karate.log('karate.env system property was:', env);
  if (!env) {
    env = 'dev';
  }
  var config = {
    env: env,
    myVarName: 'someValue'
  }
  if (env == 'dev') {
    // customize
    config.baseUrl = 'http://localhost:' + karate.properties['local.server.port'];
  } else if (env == 'test') {
    config.baseUrl = 'http://localhost:' + karate.properties['local.server.port'];
  }
  return config;
}
