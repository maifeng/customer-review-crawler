import java.io.IOException;
import java.net.MalformedURLException;


public interface GetID {
	public void getIDList() throws MalformedURLException, IOException;
	public void updateCSV(String oldfile) throws MalformedURLException, IOException;
	public void writeIDsToCSV(String filePath) throws IOException;
}
