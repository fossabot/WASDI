package it.fadeout.rest.resources;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import it.fadeout.rest.resources.largeFileDownload.ZipStreamingOutput;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Catalog;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.CatalogRepository;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.FtpUploadParameters;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.CatalogViewModel;
import wasdi.shared.viewmodels.FtpTransferViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductViewModel;

@Path("/catalog")
public class CatalogResources {

	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();

	@Context
	ServletConfig m_oServletConfig;


	@GET
	@Path("categories")
	@Produces({"application/json"})
	public ArrayList<String> getCategories(@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog("CatalogResources.GetCategories");

		ArrayList<String> categories = new ArrayList<String>();
		for ( DownloadedFileCategory c : DownloadedFileCategory.values()) {
			categories.add(c.name());
		}
		return categories; 
	}


	@GET
	@Path("entries")
	@Produces({"application/json"})
	public ArrayList<DownloadedFile> getEntries(@HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("from") String sFrom, 
			@QueryParam("to") String sTo,
			@QueryParam("freetext") String sFreeText,
			@QueryParam("category") String sCategory
			) {

		Utils.debugLog("CatalogResources.GetEntries");

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		String sUserId = oUser.getUserId();

		SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		try {
			Date dtFrom = (sFrom==null || sFrom.isEmpty())?null:oDateFormat.parse(sFrom);
			Date dtTo = (sTo==null || sTo.isEmpty())?null:oDateFormat.parse(sTo);
			return searchEntries(dtFrom, dtTo, sFreeText, sCategory, sUserId);
		} catch (ParseException e) {
			Utils.debugLog("CatalogResources.GetEntries: " + e);
			throw new InternalServerErrorException("invalid date: " + e);
		}		
	}


	@POST
	@Path("downloadentry")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadEntry(@HeaderParam("x-session-token") String sSessionId, DownloadedFile oEntry) {

		Utils.debugLog("CatalogResources.DownloadEntry");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("CatalogResources.DownloadEntry: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		File oFile = new File(oEntry.getFilePath());

		ResponseBuilder oResponseBuilder = null;
		if (!oFile.canRead()) {
			Utils.debugLog("CatalogResources.DownloadEntry: file not readable");
			oResponseBuilder = Response.serverError();
		} 
		else {
			Utils.debugLog("CatalogResources.DownloadEntry: file ok return content");
			oResponseBuilder = Response.ok(oFile);
			oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oEntry.getFileName());
		}

		Utils.debugLog("CatalogResources.DownloadEntry: done, return");
		return oResponseBuilder.build();
	}


	/**
	 * Get the entry file to download
	 * @param sFileName
	 * @return A File object if the file can be download, NULL if the file does not exist or is unreadable
	 */
	private File getEntryFile(String sFileName, String sUserId, String sWorkspace)
	{
		Utils.debugLog("CatalogResources.getEntryFile( " + sFileName + " )");
				
		String sTargetFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFileName;

		DownloadedFilesRepository oRepo = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oRepo.getDownloadedFileByPath(sTargetFilePath);

		if (oDownloadedFile == null) 
		{
			Utils.debugLog("CatalogResources.getEntryFile: file " + sFileName + " not found");
			return null;
		}
		
		File oFile = new File(sTargetFilePath);

		if( oFile.canRead() == true) {
			return oFile;
		}
		else {
			return null; 
		}
	}


	@GET
	@Path("downloadbyname")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadEntryByName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("filename") String sFileName,
			@QueryParam("workspace") String sWorkspace)
	{			

		Utils.debugLog("CatalogResources.DownloadEntryByName( " + sSessionId + ", "+ sTokenSessionId + ", " + sFileName + ", " + sWorkspace);
		
		try {
			
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.GetUserFromSession(sTokenSessionId);

			if (oUser == null) {
				Utils.debugLog("CatalogResources.DownloadEntryByName: user not authorized");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			File oFile = this.getEntryFile(sFileName, oUser.getUserId(), sWorkspace);
			
			ResponseBuilder oResponseBuilder = null;
			if(oFile == null) {
				Utils.debugLog("CatalogResources.DownloadEntryByName: file not readable");
				oResponseBuilder = Response.serverError();	
			} else {
				//InputStream oStream = null;
				FileStreamingOutput oStream;
				boolean bMustZip = mustBeZipped(oFile); 
				if(bMustZip) {
					Utils.debugLog("CatalogResources.DownloadEntryByName: file " + oFile.getName() + " must be zipped");
					return zipOnTheFlyAndStream(oFile);
				} else {
					Utils.debugLog("CatalogResources.DownloadEntryByName: no need to zip file " + oFile.getName());
					oStream = new FileStreamingOutput(oFile);
					//oStream = new FileInputStream(oFile);
					Utils.debugLog("CatalogResources.DownloadEntryByName: file ok return content");
					oResponseBuilder = Response.ok(oStream);
					oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
					oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
				}
			}
			Utils.debugLog("CatalogResources.DownloadEntryByName: done, return");
			Utils.debugLog(new EndMessageProvider().getGood());
			return oResponseBuilder.build();
		} catch (Exception e) {
			Utils.debugLog("CatalogResources.DownloadEntryByName: " + e);
		}
		return null;
	}



	private Response zipOnTheFlyAndStream(File oInitialFile) {
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream");
		if(null==oInitialFile) {
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: oFile is null");
			return null;
		}		
		try {
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: init");
			Stack<File> aoFileStack = new Stack<File>();
			aoFileStack.push(oInitialFile);
			String sBasePath = oInitialFile.getAbsolutePath();
			
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sBasePath = " + sBasePath);
			
			int iLast = sBasePath.lastIndexOf(".dim");
			String sDir = sBasePath.substring(0, iLast) + ".data";
			
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sDir = " + sDir);
			
			File oFile = new File(sDir);
			aoFileStack.push(oFile);
			
			iLast = sBasePath.lastIndexOf(oInitialFile.getName());
			sBasePath = sBasePath.substring(0, iLast);
			
			if(!sBasePath.endsWith("/") && !sBasePath.endsWith("\\")) {
				sBasePath = sBasePath + "/";
			}
			
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: updated sBasePath = " + sBasePath);
			
			int iBaseLen = sBasePath.length();
			Map<String, File> aoFileEntries = new HashMap<>();
			
			while(aoFileStack.size()>=1) {
				//Wasdi.DebugLog("CatalogResources.zipOnTheFlyAndStream: pushing files into stack");
				oFile = aoFileStack.pop();
				String sAbsolutePath = oFile.getAbsolutePath();
				
				Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sAbsolute Path " + sAbsolutePath);

				if(oFile.isDirectory()) {
					if(!sAbsolutePath.endsWith("/") && !sAbsolutePath.endsWith("\\")) {
						sAbsolutePath = sAbsolutePath + "/";
					}
					File[] aoChildren = oFile.listFiles();
					for (File oChild : aoChildren) {
						aoFileStack.push(oChild);
					}
				}
				
				Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sAbsolute Path 2 " + sAbsolutePath);
				
				String sRelativePath = sAbsolutePath.substring(iBaseLen);
				Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: adding file " + sRelativePath +" for compression");
				aoFileEntries.put(sRelativePath,oFile);
			}
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: done preparing map, added " + aoFileEntries.size() + " files");
						
			ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);

			// Set response headers and return 
			ResponseBuilder oResponseBuilder = Response.ok(oStream);
			String sFileName = oInitialFile.getName();
			
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sFileName1 " + sFileName);
			
			sFileName = sFileName.substring(0, sFileName.lastIndexOf(".dim") ) + ".zip";
			
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sFileName2 Path " + sFileName);
			
			oResponseBuilder.header("Content-Disposition", "attachment; filename=\""+ sFileName +"\"");
			Long lLength = 0L;
			for (String sFile : aoFileEntries.keySet()) {
				File oTempFile = aoFileEntries.get(sFile);
				if(!oTempFile.isDirectory()) {
					//NOTE: this way we are cheating, it is an upper bound, not the real size!
					lLength += oTempFile.length();
				}
			}
			oResponseBuilder.header("Content-Length", lLength);
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: return ");
			return oResponseBuilder.build();
		} catch (Exception e) {
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: " + e);
		} 
		return null;
	}


	private boolean mustBeZipped(File oFile) {
		Utils.debugLog("CatalogResources.mustBeZipped");
		if(null==oFile) {
			Utils.debugLog("CatalogResources.mustBeZipped: oFile is null");
			throw new NullPointerException("File is null");
		}
		boolean bRet = false;

		String sName = oFile.getName(); 
		if(!Utils.isNullOrEmpty(sName)) {
			if(sName.endsWith(".dim")) {
				bRet = true;
			}
		}
		return bRet;
	}


	@GET
	@Path("checkdownloadavaialibitybyname")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response checkDownloadEntryAvailabilityByName(@QueryParam("token") String sSessionId, @QueryParam("filename") String sFileName, @QueryParam("workspace") String sWorkspace)
	{			
		Utils.debugLog("CatalogResources.CheckDownloadEntryAvailabilityByName");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("CatalogResources.DownloadEntryByName: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		File oFile = this.getEntryFile(sFileName, oUser.getUserId(),sWorkspace);
		
		if(oFile == null) {
			return Response.serverError().build();	
		}

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(oFile != null);
		return Response.ok(oResult).build();		
	}

	/**
	 * Search entries in the internal WASDI Catalogue
	 * @param oFrom Date From
	 * @param oTo Date To
	 * @param sFreeText Free Text query
	 * @param sCategory Category
	 * @param sUserId User calling
	 * @return
	 */
	private ArrayList<DownloadedFile> searchEntries(Date oFrom, Date oTo, String sFreeText, String sCategory, String sUserId) {
		ArrayList<DownloadedFile> aoEntries = new ArrayList<DownloadedFile>();

		WorkspaceRepository oWorkspaceRepo = new WorkspaceRepository();
		ProductWorkspaceRepository oProductWorkspaceRepo = new ProductWorkspaceRepository();
		DownloadedFilesRepository oDownloadedFilesRepo = new DownloadedFilesRepository();

		//get all my workspaces
		List<Workspace> aoWorkspacesList = oWorkspaceRepo.getWorkspaceByUser(sUserId);
		Map<String, Workspace> aoWorkspaces = new HashMap<String, Workspace>();
		for (Workspace wks : aoWorkspacesList) {
			aoWorkspaces.put(wks.getWorkspaceId(), wks);
		}

		//retrieve all compatible files
		List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepo.search(oFrom, oTo, sFreeText, sCategory);

		for (DownloadedFile oDownloadedFile : aoDownloadedFiles) {

			//check if the product is in my workspace
			ProductViewModel oProductViewModel = oDownloadedFile.getProductViewModel();

			boolean bIsOwnedByUser = sCategory.equals(DownloadedFileCategory.PUBLIC.name());
			if (!bIsOwnedByUser) {
				if (oProductViewModel != null) {
					oProductViewModel.setMetadata(null);
					List<String> aoProductWorkspaces = oProductWorkspaceRepo.getWorkspaces(oProductViewModel.getFileName()); //TODO check if productName should be used					
					for (String sProductWorkspace : aoProductWorkspaces) {
						if (aoWorkspaces.containsKey(sProductWorkspace)) {
							bIsOwnedByUser = true;
							break;
						}
					}
				}
			}

			if (bIsOwnedByUser) {	
				aoEntries.add(oDownloadedFile);
			}

		}

		return aoEntries;
	}


	@GET
	@Path("")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<CatalogViewModel> getCatalogs(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {

		Utils.debugLog("CatalogResources.GetCatalogues");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<CatalogViewModel> aoCatalogList = new ArrayList<CatalogViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
				return aoCatalogList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoCatalogList;
			}

			// Create repo
			CatalogRepository oRepository = new CatalogRepository();

			// Get Process List
			List<Catalog> aoCatalogs = oRepository.getCatalogs();

			// For each
			for (int iCatalog=0; iCatalog<aoCatalogs.size(); iCatalog++) {
				// Create View Model
				CatalogViewModel oViewModel = new CatalogViewModel();
				Catalog oCatalog = aoCatalogs.get(iCatalog);

				oViewModel.setDate(oCatalog.getDate());
				oViewModel.setFilePath(oCatalog.getFilePath());
				oViewModel.setFileName(oCatalog.getFileName());

				aoCatalogList.add(oViewModel);

			}

		}
		catch (Exception oEx) {
			Utils.debugLog("CatalogResources.GetCatalogs: " + oEx);
		}

		return aoCatalogList;
	}


	/**
	 * Ingest a new file from sftp in to a target Workspace
	 * @param sSessionId User session token header
	 * @param sFile name of the file to ingest
	 * @param sWorkspace target workspace
	 * @return Http Response
	 */
	@PUT
	@Path("/upload/ingest")
	@Produces({"application/json", "text/xml"})
	public Response ingestFile(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspace) {

		Utils.debugLog("CatalogResource.IngestFile");

		// Check user session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();		
		String sAccount = oUser.getUserId();		

		// Find the sftp folder
		String sUserBaseDir = m_oServletConfig.getInitParameter("sftpManagementUserDir");

		File oUserBaseDir = new File(sUserBaseDir);
		File oFilePath = new File(new File(new File(oUserBaseDir, sAccount), "uploads"), sFile);

		// Is the file available?
		if (!oFilePath.canRead()) {
			Utils.debugLog("CatalogResource.IngestFile: ERROR: unable to access uploaded file " + oFilePath.getAbsolutePath());
			return Response.serverError().build();
		}
		try {
			// Create the ingest process
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
						
			// Generate the unique process id
			String sProcessObjId = Utils.GetRandomName();
			
			// Ingest file parameter
			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sAccount);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspace));

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			try
			{
				// Create the process
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sAccount);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.insertProcessWorkspace(oProcess);
				Utils.debugLog("CatalogueResource.IngestFile: Process Scheduled for Launcher");
			}
			catch(Exception oEx){
				Utils.debugLog("DownloadResource.Download: " + oEx);
				return Response.serverError().build();
			}

			return Response.ok().build();

		} catch (Exception e) {
			Utils.debugLog("DownloadResource.Download: " + e);
		}

		return Response.serverError().build();

	}

	/**
	 * Ingest a file already existing in a Workspace 
	 * @param sSessionId User Session token
	 * @param sFile Name of the file to ingest
	 * @param sWorkspace Id of the target workspace 
	 * @return Primitive Result with boolValue true or false and Http Code in intValue
	 */
	@GET
	@Path("/upload/ingestinws")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ingestFileInWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspace) {
		
		// Create the result object
		PrimitiveResult oResult = new PrimitiveResult();

		Utils.debugLog("CatalogResource.IngestFileInWorkspace");

		// Check the user session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;		
		}
		
		// Get the user account
		String sAccount = oUser.getUserId();		
		
		// Get the file path		
		String sFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFile;
		File oFilePath = new File(sFilePath);
		
		// Check if the file exists 
		if (!oFilePath.canRead()) {
			Utils.debugLog("CatalogResource.IngestFileInWorkspace: file not found. Check if it is an extension problem");

			String [] asSplittedFileName = sFile.split("\\.");

			if (asSplittedFileName.length == 1) {

				Utils.debugLog("CatalogResource.IngestFileInWorkspace: file without exension, try .dim");

				sFile = sFile + ".dim";
				sFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFile;
				oFilePath = new File(sFilePath);

				if (!oFilePath.canRead()) {
					Utils.debugLog("CatalogResource.IngestFileInWorkspace: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;							
				}
			}
			else {
				Utils.debugLog("CatalogResource.IngestFileInWorkspace: file with exension but not available");
				Utils.debugLog("CatalogResource.IngestFileInWorkspace: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;											
			}

		}
		
		try {
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			String sProcessObjId = Utils.GetRandomName();

			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sAccount);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspace));

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sAccount);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.insertProcessWorkspace(oProcess);
				Utils.debugLog("CatalogueResource.IngestFileInWorkspace: Process Scheduled for Launcher");
			}
			catch(Exception oEx){
				Utils.debugLog("CatalogueResource.IngestFileInWorkspace: Error updating process list " + oEx);
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;		
			}

			oResult.setBoolValue(true);
			oResult.setIntValue(200);
			oResult.setStringValue(oProcess.getProcessObjId());
			return oResult;		

		} catch (Exception e) {
			Utils.debugLog("CatalogueResource.IngestFileInWorkspace: " + e);
		}

		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		return oResult;
	}

	@PUT
	@Path("/upload/ftp")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ftpTransferFile(@HeaderParam("x-session-token") String sSessionId,  @QueryParam("workspace") String sWorkspace, FtpTransferViewModel oFtpTransferVM) {
		Utils.debugLog("CatalogResource.ftpTransferFile");

		//input validation
		if(null == sSessionId || null == oFtpTransferVM) {
			// check appropriateness
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Null arguments");
			return oResult;
		}
		if(!m_oCredentialPolicy.validSessionId(sSessionId)) {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("sSessionId badly formatted");
			return oResult;
		}
		SessionRepository oSessionRep = new SessionRepository();
		UserSession oSession = oSessionRep.getSession(sSessionId);

		if(null==oSession) {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Invalid Session");
			return oResult;
		}
		String sUserId = oSession.getUserId();
		if(null==sUserId) {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Null User");
			return oResult;
		}

		if (sWorkspace==null)  {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Null Workspace");
			return oResult;			
		}


		try {
			Utils.debugLog("CatalogResource.ftpTransferFile: prepare parameters");
			
			FtpUploadParameters oParams = new FtpUploadParameters();
			oParams.setFtpServer(oFtpTransferVM.getServer());
			oParams.setPort(oFtpTransferVM.getPort());
			oParams.setUsername(oFtpTransferVM.getUser());
			oParams.setPassword(oFtpTransferVM.getPassword());
			oParams.setRemotePath(oFtpTransferVM.getDestinationAbsolutePath());
			String sFileName = oFtpTransferVM.getFileName();
			oParams.setRemoteFileName(sFileName);
			oParams.setLocalFileName(sFileName);
			oParams.setExchange(sWorkspace);
			oParams.setWorkspace(sWorkspace);
			
			String sFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sUserId), sWorkspace);
			String sFullLocalPath = sFullPath+sFileName;
			
			oParams.setLocalPath(sFullLocalPath);
			oParams.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sUserId));

			Utils.debugLog("CatalogResource.ftpTransferFile: prepare process");
			ProcessWorkspace oProcess = new ProcessWorkspace();
			oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
			oProcess.setOperationType(LauncherOperations.FTPUPLOAD.name());
			//oProcess.setProductName(sFileUrl);
			oProcess.setWorkspaceId(sWorkspace);
			oProcess.setUserId(sUserId);
			oProcess.setProcessObjId(Utils.GetRandomName());
			oProcess.setStatus(ProcessStatus.CREATED.name());
			oParams.setProcessObjId(oProcess.getProcessObjId());

			Utils.debugLog("CatalogResource.ftpTransferFile: serialize parameters");
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + oProcess.getProcessObjId();
			SerializationUtils.serializeObjectToXML(sPath, oParams);

			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			oRepository.insertProcessWorkspace(oProcess);
			Utils.debugLog("CatalogueResource.ftpTransferFile: Process Scheduled for Launcher");

		} catch (Exception e) {
			Utils.debugLog("CatalogueResource.ftpTransferFile: " + e);
			PrimitiveResult oRes = PrimitiveResult.getInvalidInstance();
			oRes.setStringValue(e.toString());
			return oRes;
		}

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(true);
		return oResult;
	}

}
