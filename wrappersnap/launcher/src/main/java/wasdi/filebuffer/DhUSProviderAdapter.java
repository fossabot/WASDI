package wasdi.filebuffer;

import java.io.IOException;

import org.apache.log4j.Logger;

import wasdi.shared.business.ProcessWorkspace;

/**
 * Donwload File Utility Class for DhUS
 * @author p.campanella
 *
 */
public class DhUSProviderAdapter extends ProviderAdapter {
	
    public DhUSProviderAdapter() {
		super();
	}
    
    public DhUSProviderAdapter(Logger logger) {
		super(logger);
	}

    @Override
	public long getDownloadFileSize(String sFileURL)  throws Exception  {
    	// Get File size using http
    	return getDownloadFileSizeViaHttp(sFileURL);
    }

    @Override
    public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws IOException {
    	// Download using HTTP 
    	setProcessWorkspace(oProcessWorkspace);
    	return downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
    }

    @Override
    public String getFileName(String sFileURL) throws IOException {
    	// Get File Name via http
    	return getFileNameViaHttp(sFileURL);
    }
}
