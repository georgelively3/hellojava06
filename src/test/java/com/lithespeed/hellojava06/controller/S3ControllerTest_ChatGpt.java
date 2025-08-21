@ExtendWith(MockitoExtension.class)
class S3ControllerTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private S3Controller s3Controller;

    @Test
    void testUpload() {
        String fileName = "test.txt";

        doNothing().when(s3Service).uploadFile(fileName);

        ResponseEntity<String> response = s3Controller.upload(fileName);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("File uploaded successfully: " + fileName, response.getBody());
        verify(s3Service, times(1)).uploadFile(fileName);
    }

    @Test
    void testListFiles() {
        List<String> files = Arrays.asList("file1.txt", "file2.txt");
        when(s3Service.listFiles()).thenReturn(files);

        ResponseEntity<List<String>> response = s3Controller.listFiles();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(files, response.getBody());
        verify(s3Service, times(1)).listFiles();
    }

    @Test
    void testHealthCheck() {
        ResponseEntity<Map<String, String>> response = s3Controller.healthCheck();

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().containsKey("status"));
        assertEquals("UP", response.getBody().get("status"));
    }
}
