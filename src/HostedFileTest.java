/**
 * For debugging and testing HostedFile class
 *
 */
public class HostedFileTest {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		// test constructor and byte array loading
		HostedFile testFile = new HostedFile("C:/temp/industrial_fpy_04_05_2016.pdf");
		
		// test bad filename handling
		HostedFile badFilenameFile = new HostedFile("blah");
		
		// test zero-length file reading
		HostedFile zeroLengthFile = new HostedFile("C:/temp/zero_length.txt");
		
		
	}
	
}
