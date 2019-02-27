/**
 * Created by Cristiano Nattero on 2019-02-20
 * 
 * Fadeout software
 *
 */
package wasdi.wps;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringBufferInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.n52.geoprocessing.wps.client.ExecuteRequestBuilder;
import org.n52.geoprocessing.wps.client.WPS20ProcessParser;

import com.sun.tools.doclets.standard.Standard;

/**
 * @author c.nattero
 *
 */
public abstract class WpsAdapter {
	
	protected static String s_sWpsHost;
	protected static String s_sVersion;
	protected String m_sService;
	
	protected String m_sResponse;
	protected String m_sXmlPayload;
	
	protected String m_sJobId;
	

	public WpsAdapter(){
		m_sService = "WPS";
	}
		
	public void setM_sXmlPayload(String sXmlPayload) {
		this.m_sXmlPayload = sXmlPayload;
	}
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#getcapabilities
	//MAYBE getCapabilities

	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#describeprocess
	//MAYBE describeProcess

	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#execute
	public int execute() {
		System.out.println("WpsAdapter.execute");
		int iResponseCode = -1;
		try {
			
			//TODO use WPSclient: org.n52.wps.client.ExecuteRequestBuilder.ExecuteRequestBuilder
			//see WPSClientExample.executeProcess
			
			InputStream oInputStream = new ByteArrayInputStream(m_sXmlPayload.getBytes(StandardCharsets.UTF_8));

			org.n52.geoprocessing.wps.client.model.Process oWpsProcess = WPS20ProcessParser.parseProcess(oInputStream);
			ExecuteRequestBuilder oExecuteRequestBuilder = new ExecuteRequestBuilder(oWpsProcess);		
			
			
		
			String sUrl = s_sWpsHost + "?" +
					"service=" + m_sService + "&" +
					"version=" + s_sVersion;
					//and nothing else, pass the XML payload instead
			
			
			URL oUrl = new URL(sUrl);
			Object oConnectionObject = oUrl.openConnection();
			HttpURLConnection oConnection = (HttpURLConnection)oConnectionObject;
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Accept", "application/xml");
			oConnection.setRequestProperty("Content-Type", "application/xml");
			oConnection.setRequestProperty("User-Agent", "Mozilla/5.0");

			oConnection.setDoOutput(true);
			DataOutputStream oOutputStream = new DataOutputStream(oConnection.getOutputStream());
			OutputStreamWriter oWriter = new OutputStreamWriter(oOutputStream, "UTF-8");
			oWriter.write(m_sXmlPayload);
			oWriter.flush();
			oWriter.close();
			oOutputStream.flush();
			oOutputStream.close();
			
			oConnection.connect();
			
			iResponseCode = oConnection.getResponseCode();
			if(HttpURLConnection.HTTP_OK == iResponseCode) {
				m_sResponse = null;
				BufferedInputStream oBufferedReader = new BufferedInputStream(oConnection.getInputStream());
				ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();
				int iResult = oBufferedReader.read();
				while(iResult != -1) {
				    oByteArrayOutputStream.write((byte) iResult);
				    iResult = oBufferedReader.read();
				}
				m_sResponse = oByteArrayOutputStream.toString();
				System.out.println(m_sResponse);
				//TODO set job ID
			} else {
				System.out.println("WpsAdapter.execute: response status: " + iResponseCode);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return iResponseCode;
	}

	public String getResponse() {
		System.out.println("WpsAdapter.getLastStatus");
		return m_sResponse;
	}
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#getstatus
	public String getStatus() {
		//TODO GET status URL
		//TODO refactor URL + query param + maybe headers and cookies for authentication
		return null;
	}
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#getresult
	public abstract String getResult();
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#dismiss
	//MAYBE public abstract String dismiss();
}
