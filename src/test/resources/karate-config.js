function fn() {
    var config = {};
    
    // Base URL (your org's pattern)
    config.baseUrl = karate.properties.baseUrl;
    
    // External service URL - changes based on environment
    // INT: Points to WireMock (service virtualization)
    // PREPROD: Points to real external services
    config.externalServiceUrl = karate.properties['external.service.url'];
    
    return config;
}
