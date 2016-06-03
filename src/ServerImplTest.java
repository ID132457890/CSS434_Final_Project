/**
 * For debugging and testing ServerImpl class
 *
 */
public class ServerImplTest {

	private static final String VALID_FILENAME = "C:/temp/industrial_fpy_04_05_2016.pdf";
	private static final String INVALID_FILENAME = "foo";
	
	public static void main(String[] args) throws Exception {

		ServerImpl server = new ServerImpl();

		// first call retrieves from filesystem
		HostedFile a = server.getFile(VALID_FILENAME);
		
		// second call to get from cache
		HostedFile b = server.getFile(VALID_FILENAME);
		
		// request bad file
		HostedFile c = server.getFile(INVALID_FILENAME);

		// request a file for download, read-only
		FileContents d = server.download("localhost", VALID_FILENAME, ServerInterface.READ_MODE);
		
		
	}
		
}