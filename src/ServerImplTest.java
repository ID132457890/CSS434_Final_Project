/**
 * For debugging and testing ServerImpl class
 *
 */
public class ServerImplTest {

	public static void main(String[] args) throws Exception {

		ServerImpl server = new ServerImpl();

		// first call retrieves from filesystem
		HostedFile a = server.getFile("C:/temp/industrial_fpy_04_05_2016.pdf");
		
		// second call to get from cache
		HostedFile b = server.getFile("C:/temp/industrial_fpy_04_05_2016.pdf");
		
		// request bad file
		HostedFile c = server.getFile("foo");
		
	}
		
}