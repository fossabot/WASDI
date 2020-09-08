/**
 * Created by Cristiano Nattero on 2018-12-18
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.io.Util;
import org.esa.snap.core.datamodel.Product;
import org.json.JSONObject;

import wasdi.LauncherMain;
import wasdi.LoggerWrapper;
import wasdi.io.WasdiProductReader;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class ONDAProviderAdapter extends ProviderAdapter {

	/**
	 * 
	 */
	public ONDAProviderAdapter() {

	}

	/**
	 * @param logger
	 */
	public ONDAProviderAdapter(LoggerWrapper logger) {
		super(logger);
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		//file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value

		m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: start " + sFileURL);

		long lLenght = 0L;

		if(sFileURL.startsWith("file:")) {

			String sPrefix = "file:";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: full path " + sPath);
			File oSourceFile = new File(sPath);
			lLenght = oSourceFile.length();
			if (!oSourceFile.exists()) {
				m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: FILE DOES NOT EXISTS");
			}
			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: Found length " + lLenght);
		} else if(sFileURL.startsWith("https:")) {
			//lLenght = getSizeViaHttp(sFileURL);
			lLenght = getDownloadFileSizeViaHttp(sFileURL);
		}

		return lLenght;
	}


	/**
	 * @see wasdi.filebuffer.DownloadFile#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if(sFileURL.startsWith("file:")) {
			//  file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value		
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: this is a \"file:\" protocol, get file name");
			
			String sPrefix = "file:";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: source file: " + sPath);
			File oSourceFile = new File(sPath);
			
			// Destination file name: start from the simple name
			String sDestinationFileName = getFileName(sFileURL);
			// set the destination folder
			if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
			sDestinationFileName = sSaveDirOnServer + sDestinationFileName;
			
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: destination file: " + sDestinationFileName);
			
			InputStream oInputStream = null;
			OutputStream oOutputStream = null;

			// copy the product from file system
			try {
				File oDestionationFile = new File(sDestinationFileName);
				
				if (oDestionationFile.getParentFile() != null) { 
					if (oDestionationFile.getParentFile().exists() == false) {
						oDestionationFile.getParentFile().mkdirs();
					}
				}
				
				oInputStream = new FileInputStream(oSourceFile);
				oOutputStream = new FileOutputStream(oDestionationFile);
				
				m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: start copy stream");
				
				//TODO change method signature and check result: if it fails try https or retry if timeout
				//MAYBE pass the entire pseudopath so that if one does not work, another one can be tried
				copyStream(m_oProcessWorkspace, oSourceFile.length(), oInputStream, oOutputStream);

			} catch (Exception e) {
				m_oLogger.info("ONDAProviderAdapter.ExecuteDownloadFile: " + e);
			}
			finally {
				try {
					if (oOutputStream != null) {
						oOutputStream.close();
					}
				} catch (IOException e) {
					
				}
				try {
					if (oInputStream!= null) {
						oInputStream.close();
					}
				} catch (IOException e) {
					
				}
			}			
			//TODO else - i.e., if it fails - try get the file from https instead
			//	- in this case the sUrl must be modified in order to include http, so that it can be retrieved  
			return sDestinationFileName;
		} 
		else if(sFileURL.startsWith("https://")) {
			//  https://catalogue.onda-dias.eu/dias-catalogue/Products(357ae76d-f1c4-4f25-b535-e278c3f937af)/$value
						
			Boolean bAvailable = false;
			String sResult = null;
			
			int iAttempts = iMaxRetry;
			
			long lDeltaT = 10;

			while(iAttempts > 0) {
				
				m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: Attempt # " + (iMaxRetry-iAttempts+1));
				
				// Check Product Availability				
				bAvailable = checkProductAvailability(sFileURL, sDownloadUser, sDownloadPassword);
				
				if(null == bAvailable) {
					m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: impossible to check product availability");
				} 
				else if(bAvailable) {
					
					// Product Available
					m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: product available, try to download");
					
					if (Utils.isNullOrEmpty(m_oProcessWorkspace.getProductName())) {
						
						m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: product name is still null, try to get it now");
						
						String sFileNameWithoutPath = this.getFileName(sFileURL);
						
						if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {
							m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: got product name " + sFileNameWithoutPath);
							m_oProcessWorkspace.setProductName(sFileNameWithoutPath);
						}
					}
					
					// If we were in waiting, move in ready and wait the scheduler to resume us
					if (m_oProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name())) {
						
						// Put processor in READY State
						m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: Process Waiting, set it ready and wait for resume");
						LauncherMain.updateProcessStatus(new ProcessWorkspaceRepository(), m_oProcessWorkspace, ProcessStatus.READY, m_oProcessWorkspace.getProgressPerc());
						String sResumedStatus = LauncherMain.waitForProcessResume(m_oProcessWorkspace);
						m_oProcessWorkspace.setStatus(sResumedStatus);
						
						if (sResumedStatus.equals(ProcessStatus.ERROR.name()) || sResumedStatus.equals(ProcessStatus.STOPPED.name()) ) {
							m_oLogger.error("ONDAProviderAdapter.ExecuteDownloadFile: Process resumed with status ERROR or STOPPED: exit");
							break;
						}
						
						m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: Process resumed let's go!");
					}
					
					sResult = downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
					
					if (!Utils.isNullOrEmpty(sResult)) {
						
						// Get The product view Model
						File oProductFile = new File(sResult);
						
						String sNameOnly = oProductFile.getName();
						
						if (sNameOnly.startsWith("S1") || sNameOnly.startsWith("S2")) {
							
							try {
								// Product Reader will be used to test if the image has been downloaded with success.
								WasdiProductReader oReadProduct = new WasdiProductReader();
								
								Product oProduct = oReadProduct.readSnapProduct(oProductFile, null);
								if (oProduct != null)  {
									m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: download method finished result [attemp#" + (iMaxRetry-iAttempts+1) + "]: " + sResult);
									// Break the retry attempt cycle
									break;							
								}
								else {
									m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: file not readable: " + sResult + " try again");
									try {
										String sDestination = oProductFile.getPath();
										sDestination += ".attemp"+ (iMaxRetry-iAttempts+1);
										FileUtils.copyFile(oProductFile, new File(sDestination));										
									}
									catch (Exception oEx) {
										m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: Exception making copy of attempt file " + oEx.toString());
									}
								}								
							}
							catch (Exception oReadEx) {
								m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: exception reading file: " + oReadEx.toString() + " try again");
							}
							
							
							try {
								m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: delete corrupted file");
								if (oProductFile.delete()==false) {
									m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: error deleting corrupted file");
								}
							}
							catch (Exception oDeleteEx) {
								m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: exception deleting not valid file ");
							}
						}
						else {
							m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: download method finished result: " + sResult);
							// Break the retry attempt cycle
							break;							
						}
						
					}
					else {
						m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: download method finished result null, try again");
					}
				} 
				else {
					
					m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: product not available, place order");
					
					if (!m_oProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name())) {
						m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: set the process in WAITING STATE");
						// Set the process in WAITING
						LauncherMain.updateProcessStatus(new ProcessWorkspaceRepository(), m_oProcessWorkspace, ProcessStatus.WAITING, m_oProcessWorkspace.getProgressPerc());						
					}
					else {
						m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: process already in WAITING STATE");
					}
					
					String sDate = placeOrder(sFileURL, sDownloadUser, sDownloadPassword);
					
					if(null!=sDate) {
						
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
						
						long lExpectedDelta = lDeltaT;
						
						Date oNow = new Date();
						
						try {
							Date oExpected = sdf.parse(sDate);
							lExpectedDelta = oExpected.getTime() - oNow.getTime();
							lExpectedDelta /= 1000;
							
							m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: order placed. Expected date " + sDate);
							
							if (LauncherMain.s_oSendToRabbit != null) {
								String sInfo = "Download Operation<br>File in long term archive<br>Retrive scheduled for " + sDate;
								LauncherMain.s_oSendToRabbit.SendRabbitMessage(true,LauncherOperations.INFO.name(),m_oProcessWorkspace.getWorkspaceId(), sInfo,m_oProcessWorkspace.getWorkspaceId());
							}
						}
						catch (Exception e) {
							m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: impossible to parse expected date " + sDate);
						}
						
						lDeltaT = Math.max(lDeltaT, lExpectedDelta);
						
						//add some padding
						lDeltaT += 10;
					}
					else {
						m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: impossible to place product Order");
					}
					
					m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: going to sleep for " + lDeltaT + " [s]");										
					TimeUnit.SECONDS.sleep(lDeltaT);
				}
				
				iAttempts--;
				
				if(iAttempts <= 0) {
					m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: attemps finished. Break cycle");
					break;
				}
				
				// Sleep a bit before next attempt
				if (bAvailable != null) {
					TimeUnit.SECONDS.sleep(2);
				}
				else {
					m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: since it was impossible to check availability, sleep for 5 min before retry");
					TimeUnit.SECONDS.sleep(5l*60l);
				}
			}
			
			String sResLog = sResult;
			if (Utils.isNullOrEmpty(sResLog)) sResLog = "NULL";
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: return value " + sResLog);
			
			return sResult;
		}		
		return "";
	}

	
	/**
	 * Checks the Onda Product Availability from the file URL
	 * @param sFileURL link of the file
	 * @param sDownloadUser user
	 * @param sDownloadPassword pw
	 * @return true if available false otherwise
	 */
	protected Boolean checkProductAvailability(String sFileURL, String sDownloadUser, String sDownloadPassword) {
		m_oLogger.debug( "ONDAProviderAdapter.checkProductAvailability( " + sFileURL + ", " + sDownloadUser + ", ************** )");
		String sUUID = extractProductUUID(sFileURL);
		return checkProductAvailabilityFromUUID(sUUID, sDownloadUser, sDownloadPassword);
	}
	
	/**
	 * Checks the Onda Product Availability from the UUID
	 * @param sUUID product IID
	 * @param sDownloadUser user
	 * @param sDownloadPassword password
	 * @return true if available false otherwise
	 */
	private Boolean checkProductAvailabilityFromUUID(String sUUID, String sDownloadUser, String sDownloadPassword) {
		m_oLogger.debug( "ONDAProviderAdapter.checkProductAvailability( " + sUUID + ", " + sDownloadUser + ", ************** )");
		
		String sCheckUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products(" + sUUID + ")?$select=offline,downloadable";
		
		try {
				
			URL oUrl = new URL(sCheckUrl);
	        HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
	        oHttpConn.setRequestMethod("GET");
			oHttpConn.setRequestProperty("Accept", "application/json");
	        oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
	        int iResponseCode = oHttpConn.getResponseCode();
	        
	        if (iResponseCode == HttpURLConnection.HTTP_OK) {
	        	//get json
	        	String sJson = null;
	        	try {
	    			InputStream oInputStream = oHttpConn.getInputStream();		
					if(null!=oInputStream) {
						ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sJson = oBytearrayOutputStream.toString();
					}
					oHttpConn.disconnect();
	        	}catch (Exception oE) {
	    			m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", *************** ): error retrieving  reply: " + oE);
	    			return null;
	        	}
	        	//then parse json
	        	try {
	        		Boolean bAvailable = null;
					if(!Utils.isNullOrEmpty(sJson)) {
						JSONObject oJson = new JSONObject(sJson);
						if( oJson.has("offline") ) {
							bAvailable = oJson.optBoolean("offline");
							if(null!=bAvailable) {
								//if offline then not available
								bAvailable = !bAvailable;
							}
						} else if(oJson.has("downloadable")) {
							bAvailable = oJson.optBoolean("downloadable"); 
						} else {
							m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", *************): cannot infer availability from json" );
						}
					}
					return bAvailable;
	        	}catch (Exception oE) {
	        		m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", **************): during parse: " + oE);
	    			return null;
	        	}
	        } else {
	        	m_oLogger.info( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", **********): Server replied with HTTP code: " + iResponseCode);
	            InputStream oErrorStream = oHttpConn.getErrorStream();
	            if(null!=oErrorStream) {
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sError = oBytearrayOutputStream.toString();
					if(!Utils.isNullOrEmpty(sError)) {
						m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", **********): additional error info: " + sError );
					}
				}
	            oHttpConn.disconnect();
	            return null;
	        }
			
		} catch (Exception oE) {
			m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + " ): error on get: " + oE );
			return null;
		}
	}
	
	private String extractProductUUID(String sUrl) {
		// we expect a link in the form
		// https://catalogue.onda-dias.eu/dias-catalogue/Products(865b6925-59ba-49be-8444-8c99d3f0c3c4)/$value
		int iStart = sUrl.indexOf('(') + 1;
		int iEnd = sUrl.indexOf(')');
		String sUUID = sUrl.substring(iStart, iEnd);
		return sUUID;
	}
	
	private String placeOrder(String sFileUrl, String sDownloadUser, String sDownloadPassword) {
		String sUUID = extractProductUUID(sFileUrl);
		return placeOrderWithUUID(sUUID, sDownloadUser, sDownloadPassword);
	}
	
	private String placeOrderWithUUID(String sUUID, String sDownloadUser, String sDownloadPassword) {
		
		String sOrderUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products(" + sUUID + ")/Ens.Order";
		
		m_oLogger.debug("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", ********* )");
		
		if (sDownloadUser != null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					try {
						return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
					} catch (Exception oEx) {
						m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", *********** ): " + oEx);
					}
					return null;
				}
			});
		}
		
		URL oUrl;
		try {
			oUrl = new URL(sOrderUrl);
			
	        HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
	        oHttpConn.setRequestMethod("POST");
	        oHttpConn.addRequestProperty("Content-Type","application/json");
			oHttpConn.setRequestProperty("Accept", "*/*");
	        //oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
	        
	        int iResponseCode = oHttpConn.getResponseCode();
	        if (iResponseCode == HttpURLConnection.HTTP_OK) {
	        	String sJson = null;
	        	InputStream oInputStream = oHttpConn.getInputStream();
	        	ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				sJson = oBytearrayOutputStream.toString();
	        	if(!Utils.isNullOrEmpty(sJson)) {
	        		try {
	        			String sDate = null;
	        			JSONObject oJson = new JSONObject(sJson);
	        			if(oJson.has("EstimatedTime")) {
							sDate = oJson.optString("EstimatedTime", null);
						}
			        	return sDate;
	        		} catch (Exception oEx) {
	        			m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", ******* ): JSON creation failed: " + oEx);
	        		}	        		
	        	} else {
	        		m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", **************): JSON string is null or empty");
	        	}
	        } else {
	        	m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", ************* ): server returned status: " + iResponseCode );
        		InputStream  oErrorStream = oHttpConn.getErrorStream();
        		ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
        		Util.copyStream(oErrorStream, oBytearrayOutputStream);
        		String sError = oBytearrayOutputStream.toString();
        		m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", *********** ): additional message: " + sError );
        		return null;
	        }
		} catch (Exception oEx) {
			m_oLogger.debug("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", *********** ): connection failed: " + oEx);
			return null;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetFileName(java.lang.String)
	 */
	@Override
	public String getFileName(String sFileURL) throws Exception {
		//check whether the file has already been downloaded, else return null

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("ONDAProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}

		if(sFileURL.startsWith("file:")) {
			
			// In Onda, the real file is .value but here we need the name of Satellite image that, in ONDA is the parent folder name
			String sPrefix = "file:";
			String sSuffix = "/.value";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			// remove the ".value" suffix
			sPath = sPath.substring(0, sPath.lastIndexOf(sSuffix));

			// Destination file name: start from the simple name, i.e., exclude the containing dir, slash included:
			String sDestinationFileName = sPath.substring( sPath.lastIndexOf("/") + 1);
			return sDestinationFileName;

		} else if(sFileURL.startsWith("https://")) {
			return getFileNameViaHttp(sFileURL);
		} 
		return null;	
	}
	
}
