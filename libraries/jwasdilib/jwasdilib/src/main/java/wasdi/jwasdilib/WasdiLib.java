package wasdi.jwasdilib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class WasdiLib {
	
	protected static ObjectMapper s_oMapper = new ObjectMapper();
	
	/**
	 * Wasdi User
	 */
	private String m_sUser = "";
	
	/**
	 * Wasdi Password
	 */
	private String m_sPassword = "";
	
	/**
	 * Wasdi Active Workspace
	 */
	private String m_sActiveWorkspace = "";
	
	/**
	 * Wasdi Active Session
	 */
	private String m_sSessionId = "";
	
	/**
	 * Data Base Url
	 */
	private String m_sBaseUrl = "";
	
	/**
	 * Flag to know if we are on the real server
	 */
	private Boolean m_bIsOnServer = false;
	
	/**
	 * Flag to activate the automatic local download
	 */
	private Boolean m_bDownloadActive = true;
	
	/**
	 * Base Folder Path
	 */
	private String m_sBasePath = "";

	/**
	 * Own Proc Id
	 */
	private String m_sMyProcId = "";

	/**
	 * Flag to set if the lib has to be verbose or not
	 */
	private Boolean m_bVerbose = false;

	/**
	 * Params dictionary
	 */
	private HashMap<String, String> m_aoParams = new HashMap<String, String>();
	
	/**
	 * Utility paramters reader
	 */
	private ParametersReader m_oParametersReader = null;
	
	/**
	 * Path of the Parameters file
	 */
	private String m_sParametersFilePath = "";
	
	/**
	 * Self constructor. If there is a config file initilizes the class members
	 */
	public WasdiLib() {
		
	}

	/**
	 * Get User 
	 * @return User
	 */
	public String getUser() {
		return m_sUser;
	}

	/**
	 * Set User
	 * @param sUser User
	 */
	public void setUser(String sUser) {
		this.m_sUser = sUser;
	}

	/**
	 * Get Password
	 * @return
	 */
	public String getPassword() {
		return m_sPassword;
	}

	/**
	 * Set Password
	 * @param sPassword
	 */
	public void setPassword(String sPassword) {
		this.m_sPassword = sPassword;
	}

	/**
	 * Get Active Workspace
	 * @return
	 */
	public String getActiveWorkspace() {
		return m_sActiveWorkspace;
	}

	/**
	 * Set Active Workspace
	 * @param sActiveWorkspace
	 */
	public void setActiveWorkspace(String sActiveWorkspace) {
		this.m_sActiveWorkspace = sActiveWorkspace;
	}

	/**
	 * Get Session Id
	 * @return
	 */
	public String getSessionId() {
		return m_sSessionId;
	}

	/**
	 * Set Session Id
	 * @param sSessionId
	 */
	public void setSessionId(String sSessionId) {
		this.m_sSessionId = sSessionId;
	}

	/**
	 * Get Base Url
	 * @return
	 */
	public String getBaseUrl() {
		return m_sBaseUrl;
	}

	/**
	 * Set Base URl
	 * @param sBaseUrl
	 */
	public void setBaseUrl(String sBaseUrl) {
		this.m_sBaseUrl = sBaseUrl;
	}

	/**
	 * Get is on server flag
	 * @return
	 */
	public Boolean getIsOnServer() {
		return m_bIsOnServer;
	}

	/**
	 * Set is on server flag
	 * @param bIsOnServer
	 */
	public void setIsOnServer(Boolean bIsOnServer) {
		this.m_bIsOnServer = bIsOnServer;
	}

	/**
	 * Get Download Active flag
	 * @return
	 */
	public Boolean getDownloadActive() {
		return m_bDownloadActive;
	}

	/**
	 * Set Download Active Flag
	 * @param bDownloadActive
	 */
	public void setDownloadActive(Boolean bDownloadActive) {
		this.m_bDownloadActive = bDownloadActive;
	}

	/**
	 * Set Base Path
	 * @return
	 */
	public String getBasePath() {
		return m_sBasePath;
	}

	/**
	 * Get Base Path
	 * @param sBasePath
	 */
	public void setBasePath(String sBasePath) {
		this.m_sBasePath = sBasePath;
	}
	
	/**
	 * Get my own Process Id
	 * @return
	 */
	public String getMyProcId() {
		return m_sMyProcId;
	}

	/**
	 * Set My own process ID
	 * @param m_sMyProcId
	 */
	public void setMyProcId(String sMyProcId) {
		this.m_sMyProcId = sMyProcId;
	}

	/**
	 * Get Verbose Flag
	 * @return
	 */
	public Boolean getVerbose() {
		return m_bVerbose;
	}

	/**
	 * Set Verbose flag
	 * @param bVerbose
	 */
	public void setVerbose(Boolean bVerbose) {
		this.m_bVerbose = bVerbose;
	}

	// Get Params HashMap
	public HashMap<String, String> getParams() {
		return m_aoParams;
	}

	/**
	 * Add Param
	 * @param sKey
	 * @param sParam
	 */
	public void addParam(String sKey, String sParam) {
		m_aoParams.put(sKey, sParam);
	}

	/**
	 * Get Param
	 * @param sKey
	 * @return
	 */
	public String getParam(String sKey) {
		if (m_aoParams.containsKey(sKey))
			return m_aoParams.get(sKey);
		return "";
	}
	
	/**
	 * Log
	 * @param sLog
	 */
	protected void log(String sLog) {
		if (m_bVerbose == false) return;
		
		System.out.println(sLog);
	}
	
	/**
	 * Get Parameters file path
	 * @return
	 */
	public String getParametersFilePath() {
		return m_sParametersFilePath;
	}

	/**
	 * Set Parameters file path
	 * @param sParametersFilePath
	 */
	public void setParametersFilePath(String sParametersFilePath) {
		this.m_sParametersFilePath = sParametersFilePath;
	}


	public Boolean init(String sConfigFilePath) {
		try {
			
			if (sConfigFilePath != null) {
				if (!sConfigFilePath.equals("")) {
					File oConfigFile = new File(sConfigFilePath);
					if (oConfigFile.exists()) {
						ConfigReader.setConfigFilePath(sConfigFilePath);
					}
				}
			}
			
			m_sUser = ConfigReader.getPropValue("USER", "");
			m_sPassword = ConfigReader.getPropValue("PASSWORD", "");
			m_sBasePath = ConfigReader.getPropValue("BASEPATH", "");
			m_sBaseUrl = ConfigReader.getPropValue("BASEURL", "http://www.wasdi.net/wasdiwebserver/rest");
			m_sSessionId = ConfigReader.getPropValue("SESSIONID","");
			m_sActiveWorkspace = ConfigReader.getPropValue("WORKSPACEID","");
			m_sParametersFilePath = ConfigReader.getPropValue("PARAMETERSFILEPATH","./parameters.txt");

			String sDownloadActive = ConfigReader.getPropValue("DOWNLOADACTIVE", "1");
			
			if (sDownloadActive.equals("0")) {
				m_bDownloadActive = false;
			}

			String sIsOnServer = ConfigReader.getPropValue("ISONSERVER", "0");
			
			if (sIsOnServer.equals("1")) {
				m_bIsOnServer = true;
				// On Server Force Download to false
				m_bDownloadActive = false;
			}

			if (m_sBasePath.equals("")) {
				String sUserHome = System.getProperty("user.home");
				String sWasdiHome = sUserHome + "/.wasdi/";
				
				File oFolder = new File(sWasdiHome);
				oFolder.mkdirs();
				
				m_sBasePath = sWasdiHome;
			}
			
			// Read the parameters file
			m_oParametersReader = new ParametersReader(m_sParametersFilePath);
			m_aoParams = m_oParametersReader.getParameters();
			
			if (internalInit()) {
				
				if (m_sActiveWorkspace == null || m_sActiveWorkspace.equals("")) {
					
					String sWorkspaceName = ConfigReader.getPropValue("WORKSPACE","");
					
					log("Workspace to open: " + sWorkspaceName);
					
					if (!sWorkspaceName.equals("")) {
						openWorkspace(sWorkspaceName);						
					}
				}
				else {
					log("Active workspace set " + m_sActiveWorkspace);
				}
				
				return true;
			}
			else {
				return false;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public Boolean init() {
		return init(null);
	}
	
	/**
	 * Call this after base parameters settings to init the system
	 * Needed at least:
	 * Base Path
	 * User
	 * Password or SessionId
	 * @return
	 */
	public Boolean internalInit() {
		
		try {
			
			log("jWASDILib Init");
			
			
			// User Name Needed 
			if (m_sUser == null) return false;
			
			log("User not null");
			
			// Is there a password?
			if (m_sPassword != null && !m_sPassword.equals("")) {
				
				log("Password not null. Try to login");
				
				// Try to log in
				String sResponse = login(m_sUser, m_sPassword);
				
				// Get JSON
				Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
				 
				if (aoJSONMap == null) return false;
				
				if (aoJSONMap.containsKey("sessionId")) {
					// Got Session
					m_sSessionId = (String) aoJSONMap.get("sessionId"); 
					return true;
				}				
			}
			else if (m_sSessionId != null) {
				
				// Check User supplied Session
				String sResponse = checkSession(m_sSessionId);
				
				// Get JSON
				Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
				if (aoJSONMap == null) return false;
				
				// Check if session and user id are the same
				if (aoJSONMap.containsKey("userId")) {
					if (((String)aoJSONMap.get("userId")).equals(m_sUser)) {
						return true;
					}
				}
			}
			
			
			return false;
			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Call CheckSession API
	 * @param sSessionID actual session Id
	 * @return Session Id or "" if there are problems
	 */
	public String checkSession(String sSessionID) {
		try {
			
			String sUrl = m_sBaseUrl + "/auth/checksession";
			
			HashMap<String, String> aoHeaders = new HashMap<String, String>();
			aoHeaders.put("x-session-token", sSessionID);
			aoHeaders.put("Content-Type", "application/json");
			
			String sResult = httpGet(sUrl, aoHeaders);
			
			return sResult;

		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	
	/**
	 * Call Login API
	 * @param sUser
	 * @param sPassword
	 * @return
	 */
	public String login(String sUser, String sPassword) {
		try {
			String sUrl = m_sBaseUrl + "/auth/login";

			String sPayload = "{\"userId\":\"" + m_sUser + "\",\"userPassword\":\"" + m_sPassword + "\" }";
			
			HashMap<String, String> aoHeaders = new HashMap<String, String>();
			
			aoHeaders.put("Content-Type", "application/json");
			
			String sResult = httpPost(sUrl, sPayload, aoHeaders);
			
			return sResult;

		} catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Get the standard headers for a WASDI call
	 * @return
	 */
	protected HashMap<String, String> getStandardHeaders() {
		HashMap<String, String> aoHeaders = new HashMap<String, String>();
		aoHeaders.put("x-session-token", m_sSessionId);
		aoHeaders.put("Content-Type", "application/json");
		
		return aoHeaders;
	}
	
	/**
	 * 
	 * @return
	 */
	protected HashMap<String, String> getStreamingHeaders() {
		HashMap<String, String> aoHeaders = new HashMap<String, String>();
		aoHeaders.put("x-session-token", m_sSessionId);
		aoHeaders.put("Content-Type", "multipart/form-data");
		
		return aoHeaders;
	}

	/**
	 * get the list of workspaces of the logged user
	 * @return List of Workspace as JSON representation
	 */
	public List<Map<String, Object>> getWorkspaces() {
		
		try {
		    String sUrl = m_sBaseUrl + "/ws/byuser";
		    
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});
		    
			return aoJSONMap;			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return null;
		}	    
	}
	
	/**
	 * Get Id of a Workspace from the name
     * Return the WorkspaceId as a String, "" if there is any error
	 * @param sWorkspaceName Workspace Name
	 * @return Workspace Id if found, "" if there is any error
	 */
	public String getWorkspaceIdByName(String sWorkspaceName) {
		try {
			String sUrl = m_sBaseUrl + "/ws/byuser";
		    
			// Get all the Workspaces
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});
		    
		    // Search the one by name
		    for (Map<String, Object> oWorkspace : aoJSONMap) {
				if (oWorkspace.get("workspaceName").toString().equals(sWorkspaceName)) {
					// Found
					return (String) oWorkspace.get("workspaceId").toString();
				}
			}
		    
		    return "";
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	
	/**
	 * Open a workspace
	 * @param sWorkspaceName Workspace name to open
	 * @return WorkspaceId as a String, '' if there is any error
	 */
	public String openWorkspace(String sWorkspaceName) {
		
		log("Open Workspace " + m_sActiveWorkspace);
		
		m_sActiveWorkspace = getWorkspaceIdByName(sWorkspaceName);
		return m_sActiveWorkspace;
	}
	
	/**
	 * Get a List of the products in a Workspace
	 * @param sWorkspaceName Workspace Name
	 * @return List of Strings representing the product names
	 */
	public List <String> getProductsByWorkspace(String sWorkspaceName) {
		 
		ArrayList<String> asProducts = new ArrayList<String>();
		try {
			
			openWorkspace(sWorkspaceName);
			
			String sUrl = m_sBaseUrl + "/product/byws?sWorkspaceId=" + m_sActiveWorkspace;
		    
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});
		    
		    for (Map<String, Object> oProduct : aoJSONMap) {
		    	asProducts.add(oProduct.get("fileName").toString());
			}
		    
		    return asProducts;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return asProducts;
		}
	}
	
	/**
	 * Get the full local path of a product given the product name. Use the output of this API to open the file
	 * @param sProductName Product Name
	 * @return Product Full Path as a String ready to open file
	 */
	public String getFullProductPath(String sProductName) {
		try {
			String sFullPath = m_sBasePath;
			
			if (m_bIsOnServer) {
				sFullPath = "/data/wasdi";
			}
			
			if (! (sFullPath.endsWith("\\") || sFullPath.endsWith("/") ) ){
				sFullPath +="/";
			}
			
			sFullPath = sFullPath +m_sUser + "/" + m_sActiveWorkspace + "/" + sProductName;
			
			if (m_bIsOnServer==false) {
				if (m_bDownloadActive == true) {
					if (new File(sFullPath).exists() == false) {
						System.out.println("Local file Missing. Start WASDI download. Please wait");
						downloadFile(sProductName);
						System.out.println("File Downloaded on Local PC, keep on working!");
					}
				}
			}
			
			return sFullPath;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	public String getSavePath() {
		try {
			String sFullPath = m_sBasePath;
			
			if (m_bIsOnServer) {
				sFullPath = "/data/wasdi";
			}
			
			if (! (sFullPath.endsWith("\\") || sFullPath.endsWith("/") ) ){
				sFullPath +="/";
			}
			
			sFullPath = sFullPath +m_sUser + "/" + m_sActiveWorkspace + "/";
			
			return sFullPath;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	
	

	/**
     *   Get the list of Workflows for the user
     *   Return None if there is any error
     *   Return an array of WASI Workspace JSON Objects if everything is ok:
     *   
     *   {
     *       "description":STRING,
     *       "name": STRING,
     *       "workflowId": STRING
     *   }        
     *   
	 * @return
	 */
	public List<Map<String, Object>> getWorkflows() {
		
		try {
		    String sUrl = m_sBaseUrl + "/processing/getgraphsbyusr";
		    
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});
		    
			return aoJSONMap;			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return null;
		}	    
	}
	

	/**
	 * Internal execute workflow
	 * @param asInputFileNames
	 * @param asOutputFileNames
	 * @param sWorkflowName
	 * @param bAsynch true if asynch, false for synch
	 * @return if Asynch, the process Id else the ouput status of the workflow process
	 */
	protected String internalExecuteWorkflow(String [] asInputFileNames, String []  asOutputFileNames, String sWorkflowName, Boolean bAsynch) {
		try {
			
			String sProcessId = "";
			String sWorkflowId = "";
		    String sUrl = m_sBaseUrl + "/processing/graph_id?workspace=" + m_sActiveWorkspace;
		    
		    List<Map<String,Object>> aoWorkflows = getWorkflows();
		    
		    for (Map<String, Object> oWorkflow : aoWorkflows) {
				if (oWorkflow.get("name").equals(sWorkflowName)) {
					sWorkflowId = oWorkflow.get("workflowId").toString();
					break;
				}
			}
		    
		    if (sWorkflowId.equals("")) return "";
		    
		    String sPayload = "{\"name\":\""+ sWorkflowName+"\", \"description\":\"\",\"workflowId\":\"" + sWorkflowId+"\"";
		    sPayload += ", \"inputNodeNames\": []";
		    sPayload += ", \"inputFileNames\": [";
		    for (int i=0; i<asInputFileNames.length; i++) {
		    	if (i>0) sPayload += ",";
		    	sPayload += "\""+asInputFileNames[i]+"\"";
		    }
		    sPayload += "]";
		    sPayload += ", \"outputNodeNames\":[]";
		    sPayload += ", \"outputFileNames\":[";
		    for (int i=0; i<asOutputFileNames.length; i++) {
		    	if (i>0) sPayload += ",";
		    	sPayload += "\""+asOutputFileNames[i]+"\"";
		    }
		    sPayload += "]";
		    sPayload += "}";
		    
		    String sResponse = httpPost(sUrl,sPayload, getStandardHeaders());
		    Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
		    
		    sProcessId = aoJSONMap.get("stringValue").toString(); 
		    
			if (bAsynch) return sProcessId;
			else return waitProcess(sProcessId);		
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}	 		
	}
	
	/**
	 * Executes a WASDI SNAP Workflow in a asynch mode
	 * @param sInputFileName
	 * @param sOutputFileName
	 * @param sWorkflowName
	 * @return Workflow Process Id if every thing is ok, '' if there was any problem
	 */
	public String asynchExecuteWorkflow(String [] asInputFileName, String []  asOutputFileName, String sWorkflowName) {
		return internalExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflowName, true);
	}
	
	/**
	 * Executes a WASDI SNAP Workflow waiting for the process to finish
	 * @param sInputFileName 
	 * @param sOutputFileName
	 * @param sWorkflowName
	 * @return output status of the Workflow Process
	 */
	public String executeWorkflow(String []  asInputFileName, String [] asOutputFileName, String sWorkflowName) {
 		return internalExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflowName, false);
	}
	
	/**
	 * Get WASDI Process Status 
	 * @param sProcessId Process Id
	 * @return  Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
	 */
	public String getProcessStatus(String sProcessId) {
		try {
			
		    String sUrl = m_sBaseUrl + "/process/byid?sProcessId="+sProcessId;
		    
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
		    
			return aoJSONMap.get("status").toString();			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}	  
	}
	
	/**
	 *  Update the status of a process
	 * @param sProcessId Process Id
	 * @param sStatus Status to set
	 * @param iPerc Progress in %
	 * @return updated status as a String or '' if there was any problem
	 */
	public String updateProcessStatus(String sProcessId,String sStatus,int iPerc) {
		try {
			
			if (iPerc<0 || iPerc>100) {
				System.out.println("Percentage must be between 0 and 100 included");
				return "";
			}
			
			if (sStatus == null) {
				System.out.println("sStatus must not be null");
				return "";				
			}
			
			if (!(sStatus.equals("CREATED") ||  sStatus.equals("RUNNING") ||  sStatus.equals("STOPPED")||  sStatus.equals("DONE")||  sStatus.equals("ERROR"))) {
				System.out.println("sStatus must be a string like one of  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR");
				return "";
			}
			
			if (sProcessId == null) {
				System.out.println("sProcessId must not be null");
			}

			if (sProcessId.equals("")) {
				System.out.println("sProcessId must not be empty");
			}

		    String sUrl = m_sBaseUrl + "/process/updatebyid?sProcessId="+sProcessId+"&status="+sStatus+"&perc="+iPerc;
		    
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
		    
			return aoJSONMap.get("status").toString();			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 *  Update the status of a process
	 * @param sProcessId Process Id
	 * @param sStatus Status to set
	 * @param iPerc Progress in %
	 * @return updated status as a String or '' if there was any problem
	 */
	public String updateProgressPerc(int iPerc) {
		try {
			
			if (iPerc<0 || iPerc>100) {
				System.out.println("Percentage must be between 0 and 100 included");
				return "";
			}
						
			if (m_sMyProcId == null) {
				System.out.println("Own process Id net available");
			}

			if (m_sMyProcId.equals("")) {
				System.out.println("Progress: " + iPerc);
			}
			
			String sStatus = "RUNNING";

		    String sUrl = m_sBaseUrl + "/process/updatebyid?sProcessId="+m_sMyProcId+"&status="+sStatus+"&perc="+iPerc + "&sendrabbit=1";
		    
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
		    
			return aoJSONMap.get("status").toString();			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Wait for a process to finish
	 * @param sProcessId
	 * @return
	 */
	public String waitProcess(String sProcessId) {
		String sStatus = "";
		while ( ! (sStatus.equals("DONE") || sStatus.equals("STOPPED") || sStatus.equals("ERROR"))) {
			sStatus = getProcessStatus(sProcessId);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return sStatus;
	}
	
	/**
	 * Adds output payload to a process
	 * @param sProcessId
	 * @param sData
	 * @return
	 */
	public String setProcessPayload(String sProcessId,String sData) {
		try {
			
			if (sProcessId == null) {
				System.out.println("sProcessId must not be null");
			}

			if (sProcessId.equals("")) {
				System.out.println("sProcessId must not be empty");
			}

		    String sUrl = m_sBaseUrl + "/process/setpayload?sProcessId="+sProcessId+"&payload="+sData;
		    
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
		    
			return aoJSONMap.get("status").toString();			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	
	
	/**
	 * Refresh Parameters reading again the file
	 */
	public void refreshParameters() {
		m_oParametersReader.refresh();
	}
	
	protected String internalAddFileToWADI(String sFileName, Boolean bAsynch) {
		try {
			
			if (sFileName == null) {
				System.out.println("sFileName must not be null");
			}

			if (sFileName.equals("")) {
				System.out.println("sFileName must not be empty");
			}

		    String sUrl = m_sBaseUrl + "/catalog/upload/ingestinws?file="+sFileName+"&workspace="+m_sActiveWorkspace;
		    
		    String sResponse = httpGet(sUrl, getStandardHeaders());
		    Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
		    
		    String sProcessId = aoJSONMap.get("stringValue").toString();
		    
			if (bAsynch) return sProcessId;
			else return waitProcess(sProcessId);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}		
	}
	
	/**
	 * Ingest a new file in the Active WASDI Workspace waiting for the result
	 * The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
	 * To work be sure that the file is on the server
	 * @param sFileName Name of the file to add
	 * @return Output state of the ingestion process
	 */
	public String addFileToWASDI(String sFileName) {
		return internalAddFileToWADI(sFileName, false);
	}
	
	/**
	 * Ingest a new file in the Active WASDI Workspace in an asynch way
	 * The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
	 * To work be sure that the file is on the server 
	 * @param sFileName Name of the file to add
	 * @return Process Id of the ingestion process
	 */
	public String asynchAddFileToWASDI(String sFileName) {
		return internalAddFileToWADI(sFileName, true);
	}
	

	public String httpGet(String sUrl, Map<String, String> asHeaders) {
		
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();


			// optional default is GET
			oConnection.setRequestMethod("GET");
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}

			//System.out.println("\nSending 'GET' request to URL : " + sUrl);

			int iResponseCode =  oConnection.getResponseCode();
			//System.out.println("Response Code : " + responseCode);

			if(200 == iResponseCode) {
				BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
				String sInputLine;
				StringBuffer sResponse = new StringBuffer();
		
				while ((sInputLine = oInputBuffer.readLine()) != null) {
					sResponse.append(sInputLine);
				}
				oInputBuffer.close();
			

				//print result
				//System.out.println("Count Done: Response " + sResponse.toString());
		
				return sResponse.toString();
			} else {
				String sMessage = oConnection.getResponseMessage();
				System.out.println(sMessage);
				return "";
			}			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	
	public String httpPost(String sUrl, String sPayload, Map<String, String> asHeaders) {
		
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			oConnection.setDoOutput(true);
			// Set POST
			oConnection.setRequestMethod("POST");
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}
			
			OutputStream oPostOutputStream = oConnection.getOutputStream();
			OutputStreamWriter oStreamWriter = new OutputStreamWriter(oPostOutputStream, "UTF-8");  
			oStreamWriter.write(sPayload);
			oStreamWriter.flush();
			oStreamWriter.close();
			oPostOutputStream.close(); 
			
			oConnection.connect();

			BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
			String sInputLine;
			StringBuffer sResponse = new StringBuffer();
	
			while ((sInputLine = oInputBuffer.readLine()) != null) {
				sResponse.append(sInputLine);
			}
			oInputBuffer.close();
			
			return sResponse.toString();
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	
	public String httpPost(String sUrl, File oUploadFile, Map<String, String> asHeaders) {
//		URLConnection urlconnection = null;
//		try {
//			URL url = new URL(sUrl);
//			urlconnection = url.openConnection();
//			urlconnection.setDoOutput(true);
//			urlconnection.setDoInput(true);
//
//			if (urlconnection instanceof HttpURLConnection) {
//				((HttpURLConnection) urlconnection).setRequestMethod("POST");
//				((HttpURLConnection) urlconnection).setRequestProperty("Content-type", "multipart/form-data");
//				((HttpURLConnection) urlconnection).connect();
//			}
//
//			BufferedOutputStream bos = new BufferedOutputStream(urlconnection.getOutputStream());
//			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(oUploadFile));
//			int i;
//			// read byte by byte until end of stream
//			while ((i = bis.read()) > 0) {
//				bos.write(i);
//			}
//			bis.close();
//			bos.close();
//			System.out.println(((HttpURLConnection) urlconnection).getResponseMessage());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//
//			InputStream inputStream;
//			int responseCode = ((HttpURLConnection) urlconnection).getResponseCode();
//			if ((responseCode >= 200) && (responseCode <= 202)) {
//				inputStream = ((HttpURLConnection) urlconnection).getInputStream();
//				int j;
//				while ((j = inputStream.read()) > 0) {
//					System.out.println(j);
//				}
//
//			} else {
//				inputStream = ((HttpURLConnection) urlconnection).getErrorStream();
//			}
//			((HttpURLConnection) urlconnection).disconnect();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return "";
				try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			oConnection.setDoOutput(true);
			// Set POST
			oConnection.setRequestMethod("POST");
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}

			//TODO MY CODE (BY STACKOVERFLOW) 
//			BufferedOutputStream oBos = new BufferedOutputStream(oConnection.getOutputStream());
			OutputStream oBos = oConnection.getOutputStream();

//			BufferedInputStream oBis = new BufferedInputStream(new FileInputStream(oUploadFile));
			InputStream oBis = new FileInputStream(oUploadFile);

			
			int i;
			// read byte by byte until end of stream
			while ((i = oBis.read()) > 0) {
				oBos.write(i);
			}
			oBis.close();
			oBos.close();
			//TODO MY CODE END 
			
			oConnection.connect();//MOVE ABOVE ? 

			InputStream oInputStream= oConnection.getInputStream();
			BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oInputStream));
			String sInputLine;
			StringBuffer sResponse = new StringBuffer();
	
			while ((sInputLine = oInputBuffer.readLine()) != null) {
				sResponse.append(sInputLine);
			}
			oInputBuffer.close();
			
			return sResponse.toString();
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	/**
	 * Download a file on the local PC
	 * @param sFileName File Name
	 * @return Full Path
	 */
	protected String downloadFile(String sFileName) {
		try {
			
			if (sFileName == null) {
				System.out.println("sFileName must not be null");
			}

			if (sFileName.equals("")) {
				System.out.println("sFileName must not be empty");
			}

		    String sUrl = m_sBaseUrl + "/catalog/downloadbyname?filename="+sFileName;
		    
		    String sOutputFilePath = "";
		    
		    HashMap<String, String> asHeaders = getStandardHeaders();
			
			try {
				URL oURL = new URL(sUrl);
				HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

				// optional default is GET
				oConnection.setRequestMethod("GET");
				
				if (asHeaders != null) {
					for (String sKey : asHeaders.keySet()) {
						oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
					}
				}
				
				int responseCode =  oConnection.getResponseCode();

				if(responseCode == 200) {
					
					sOutputFilePath = getSavePath();
					sOutputFilePath += sFileName;

					File oTargetFile = new File(sOutputFilePath);
					File oTargetDir = oTargetFile.getParentFile();
					oTargetDir.mkdirs();

					// opens an output stream to save into file
					FileOutputStream oOutputStream = new FileOutputStream(sOutputFilePath);

					InputStream oInputStream = oConnection.getInputStream();

					copyStream(oInputStream, oOutputStream);

					oInputStream.close();
				
			
					return sOutputFilePath;
				} else {
					String sMessage = oConnection.getResponseMessage();
					System.out.println(sMessage);
					return "";
				}

			} catch (Exception oEx) {
				oEx.printStackTrace();
				return "";
			}
			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}		
	}
	
	/**
	 * Copy Input Stream in Output Stream
	 * @param oInputStream
	 * @param oOutputStream
	 * @throws IOException
	 */
	protected void copyStream(InputStream oInputStream, OutputStream oOutputStream) throws IOException {

		int BUFFER_SIZE = 4096;
	    //int MAX_NUM_ZEORES_DURING_READ = 20;

		int iBytesRead = -1;
		byte[] abBuffer = new byte[BUFFER_SIZE];
		//int nZeroes = MAX_NUM_ZEORES_DURING_READ;
		while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {

			//if (iBytesRead <= 0) {
			//	nZeroes--;
			//} else {
			//	nZeroes = MAX_NUM_ZEORES_DURING_READ;
			//}
			//if (nZeroes <= 0) break;

			oOutputStream.write(abBuffer, 0, iBytesRead);

		}
				
		oOutputStream.close();
		oInputStream.close();
		
	}
	
	/**
	 * 
	 * @param sFileName
	 */
	public void uploadFile(String sFileName) 
	{
		if(sFileName==null || sFileName.isEmpty())
		{
			//TODO ERROR
			System.out.println("sFileName must not be empty or null");
		}
		String sFullPath = getSavePath() + sFileName;
//		FileInputStream oFileInputStream=null;
//		try {
//			oFileInputStream = new FileInputStream(sFullPath);
//		} catch (Exception e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		

		String sUrl = m_sBaseUrl + "/product/uploadfile?name="+sFileName +"&workspace=" + m_sActiveWorkspace;
//		String sUrl = m_sBaseUrl + "/product/uploadfile?workspace=" + m_sActiveWorkspace + "&name=" + sFileName;

		URL oURL;
		HttpURLConnection oConnection;
	    HashMap<String, String> asHeaders = getStreamingHeaders();

	    File oFile = new File(sFullPath);

	    httpPost(sUrl,oFile ,asHeaders);
	
	}


	
	
	
	/**
	 * Default Http Post
	 * @param sUrl full url (base path + relative path + queryparams)
	 * @param sPayload body payload as a string
	 * @param asHeaders headers dictionary <String key> <String value>
	 * @return Server response as a String
	protected String httpPost2(String sUrl, String sPayload, Map<String, String> asHeaders) {
		
		HttpClient oClient = new DefaultHttpClient();
		try {
			
			HttpPost oHost = new HttpPost(sUrl);

			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oHost.setHeader(sKey,asHeaders.get(sKey));
				}
			}

			StringEntity input = new StringEntity(sPayload);

			oHost.setEntity(input);

			HttpResponse response = oClient.execute(oHost);

			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			StringBuilder oStringBuilder = new StringBuilder();

			String sLine = "";

			while ((sLine = rd.readLine()) != null) {

				oStringBuilder.append(sLine);

			}

			return oStringBuilder.toString();

		} catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		} finally { 
			if (oClient != null) ((DefaultHttpClient)oClient).close();
		}

	}
	*/
	
	
	/**
	 * Default Http Get
	 * @param sUrl full url (base path + relative path + queryparams)
	 * @param asHeaders headers dictionary <String key> <String value>
	 * @return Server response as a String
	 * @return
	public String httpGet2(String sUrl, Map<String, String> asHeaders) {
		
		HttpClient oClient = new DefaultHttpClient();
		
		try {
			
			HttpGet oHost = new HttpGet(sUrl);
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oHost.setHeader(sKey,asHeaders.get(sKey));
				}
			}
			
			HttpResponse response = oClient.execute(oHost);
		
			
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			StringBuilder oStringBuilder = new StringBuilder();

			String sLine = "";

			while ((sLine = rd.readLine()) != null) {
				oStringBuilder.append(sLine);
			}

			return oStringBuilder.toString();

		} catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	*/
	
	/**
	 * Default Http Get
	 * @param sUrl full url (base path + relative path + queryparams)
	 * @param asHeaders headers dictionary <String key> <String value>
	 * @return Server response as a String
	 * @return
	
	public String httpsGet(String sUrl, Map<String, String> asHeaders) {
		try {
			
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
				 
			CloseableHttpClient client = HttpClients.custom()
				      .setSSLContext(sslContext)
				      .setSSLHostnameVerifier(new NoopHostnameVerifier())
				      .build();

			HttpGet oHost = new HttpGet(sUrl);

			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oHost.setHeader(sKey,asHeaders.get(sKey));
				}
			}

			HttpResponse response = client.execute(oHost);
			

			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			StringBuilder oStringBuilder = new StringBuilder();

			String sLine = "";

			while ((sLine = rd.readLine()) != null) {
				oStringBuilder.append(sLine);
			}

			return oStringBuilder.toString();

		} catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	 */
	
}