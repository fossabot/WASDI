package it.fadeout;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.glassfish.jersey.server.ResourceConfig;

import it.fadeout.business.DownloadsThread;
import it.fadeout.business.IDLThread;
import it.fadeout.business.ProcessingThread;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.rabbit.RabbitFactory;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.Utils;

public class Wasdi extends ResourceConfig {
	@Context
	ServletConfig m_oServletConfig;

	@Context
	ServletContext m_oContext;

	/**
	 * Flag for Debug Log: if true Authentication is disabled
	 */
	private static boolean s_bDebug = false;

	/**
	 * Process queue scheduler
	 */
	private static ProcessingThread s_oProcessingThread = null;

	/**
	 * Downloads queue scheduler
	 */
	private static DownloadsThread s_oDownloadsThread = null;

	/**
	 * IDL Processors queue scheduler
	 */
	private static IDLThread s_oIDLThread = null;

	/**
	 * User for debug mode auto login
	 */
	public static String s_sDebugUser = "user";

	/**
	 * Password for debug mode auto login
	 */
	public static String s_sDebugPassword = "password";

	// XXX replace with dependency injection
	private static CredentialPolicy m_oCredentialPolicy;

	static {
		m_oCredentialPolicy = new CredentialPolicy();
	}

	public Wasdi() {
		register(new WasdiBinder());
		packages(true, "it.fadeout.rest.resources");
	}

	//	@Override
	//	public Set<Class<?>> getClasses() {
	//		final Set<Class<?>> classes = new HashSet<Class<?>>();
	//		// register resources and features
	//		classes.add(FileBufferResource.class);
	//		classes.add(OpenSearchResource.class);
	//		classes.add(WasdiResource.class);
	//		classes.add(AuthResource.class);
	//		classes.add(WorkspaceResource.class);
	//		classes.add(ProductResource.class);
	//		classes.add(OpportunitySearchResource.class);
	//		classes.add(ProcessingResources.class);
	//		classes.add(ProcessWorkspaceResource.class);
	//		classes.add(CatalogResources.class);
	//		return classes;
	//	}

	@PostConstruct
	public void initWasdi() {

		Utils.debugLog("-----------welcome to WASDI - Web Advanced Space Developer Interface");

		if (getInitParameter("DebugVersion", "false").equalsIgnoreCase("true")) {
			s_bDebug = true;
			Utils.debugLog("-------Debug Version on");
			s_sDebugUser = getInitParameter("DebugUser", "user");
			s_sDebugPassword = getInitParameter("DebugPassword", "password");
		}

		try {
			Utils.m_iSessionValidityMinutes = Integer
					.parseInt(getInitParameter("SessionValidityMinutes", "" + Utils.m_iSessionValidityMinutes));
			Utils.debugLog("-------Session Validity [minutes]: " + Utils.m_iSessionValidityMinutes);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		// set nfs properties download
		String sUserHome = System.getProperty("user.home");
		String sNfsFolder = System.getProperty("nfs.data.download");
		if (sNfsFolder == null)
			System.setProperty("nfs.data.download", sUserHome + "/nfs/download");

		Utils.debugLog("-------nfs dir " + System.getProperty("nfs.data.download"));

		try {

			MongoRepository.SERVER_ADDRESS = getInitParameter("MONGO_ADDRESS", "127.0.0.1");
			MongoRepository.SERVER_PORT = Integer.parseInt(getInitParameter("MONGO_PORT", "27017"));
			MongoRepository.DB_NAME = getInitParameter("MONGO_DBNAME", "wasdi");
			MongoRepository.DB_USER = getInitParameter("MONGO_DBUSER", "mongo");
			MongoRepository.DB_PWD = getInitParameter("MONGO_DBPWD", "mongo");

			Utils.debugLog("-------Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable e) {
			e.printStackTrace();
		}

		try {

			RabbitFactory.s_sRABBIT_QUEUE_USER = getInitParameter("RABBIT_QUEUE_USER", "guest");
			RabbitFactory.s_sRABBIT_QUEUE_PWD = getInitParameter("RABBIT_QUEUE_PWD", "guest");
			RabbitFactory.s_sRABBIT_HOST = getInitParameter("RABBIT_HOST", "127.0.0.1");
			RabbitFactory.s_sRABBIT_QUEUE_PORT = getInitParameter("RABBIT_QUEUE_PORT", "5672");

			Utils.debugLog("-------Rabbit User " + RabbitFactory.s_sRABBIT_QUEUE_USER);

		} catch (Throwable e) {
			e.printStackTrace();
		}

		if (s_oProcessingThread == null) {
			try {

				Utils.debugLog("-------Starting Processing and Download Schedulers...");

				if (getInitParameter("EnableProcessingScheduler", "true").toLowerCase().equals("true")) {
					s_oProcessingThread = new ProcessingThread(m_oServletConfig);
					s_oProcessingThread.start();
					Utils.debugLog("-------processing thread STARTED");
				} else {
					Utils.debugLog("-------processing thread DISABLED");
				}

				if (getInitParameter("EnableDownloadScheduler", "true").toLowerCase().equals("true")) {
					s_oDownloadsThread = new DownloadsThread(m_oServletConfig);
					s_oDownloadsThread.start();
					Utils.debugLog("-------downloads thread STARTED");
				} else {
					Utils.debugLog("-------downloads thread DISABLED");
				}

				if (getInitParameter("EnableIDLScheduler", "true").toLowerCase().equals("true")) {
					s_oIDLThread = new IDLThread(m_oServletConfig);
					s_oIDLThread.start();
					Utils.debugLog("-------IDL thread STARTED");
				} else {
					Utils.debugLog("-------IDL thread DISABLED");
				}

			} catch (Exception e) {
				e.printStackTrace();
				Utils.debugLog("-------ERROR: CANNOT START PROCESSING THREAD!!!");
			}
		}

		Utils.debugLog("-------initializing snap...");

		try {
			String snapAuxPropPath = getInitParameter("SNAP_AUX_PROPERTIES", null);
			Utils.debugLog("snap aux properties file: " + snapAuxPropPath);
			Path propFile = Paths.get(snapAuxPropPath);
			Config.instance("snap.auxdata").load(propFile);
			Config.instance().load();

			SystemUtils.init3rdPartyLibs(null);
			SystemUtils.LOG.setLevel(Level.ALL);

			String sSnapLogActive = getInitParameter("SNAP_LOG_ACTIVE", "1");

			if (sSnapLogActive.equals("1") || sSnapLogActive.equalsIgnoreCase("true")) {
				String sSnapLogFolder = getInitParameter("SNAP_LOG_FOLDER", "/usr/lib/wasdi/launcher/logs/snapweb.log");

				FileHandler oFileHandler = new FileHandler(sSnapLogFolder, true);
				// ConsoleHandler handler = new ConsoleHandler();
				oFileHandler.setLevel(Level.ALL);
				SimpleFormatter oSimpleFormatter = new SimpleFormatter();
				oFileHandler.setFormatter(oSimpleFormatter);
				SystemUtils.LOG.setLevel(Level.ALL);
				SystemUtils.LOG.addHandler(oFileHandler);
			}
			Engine.start(false);

			// init HASH
			// initPasswordAuthenticationParameters();

		} catch (Throwable e) {
			e.printStackTrace();
		}

		Utils.debugLog("------- WASDI Init done ");
		Utils.debugLog("---------------------------------------------");
		Utils.debugLog("------- 	 Welcome to space     -------");
		Utils.debugLog("---------------------------------------------");
	}

	/**
	 * Server Shut down procedure
	 */
	public static void shutDown() {
		try {
			Utils.debugLog("-------Shutting Down Wasdi");

			s_oProcessingThread.stopThread();
			s_oDownloadsThread.stopThread();
			s_oIDLThread.stopThread();
			MongoRepository.shutDownConnection();
		} catch (Exception e) {
			Utils.debugLog("WASDI SHUTDOWN EXCEPTION: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Safe Read Init Parameter
	 * 
	 * @param sParmaneter
	 * @param sDefault
	 * @return
	 */
	private String getInitParameter(String sParmaneter, String sDefault) {
		String sParameterValue = m_oServletConfig.getInitParameter(sParmaneter);
		return sParameterValue == null ? sDefault : sParameterValue;
	}

	/*
	 * 
	 */
	/*
	 * private void initPasswordAuthenticationParameters() {
	 * PasswordAuthentication.setAlgorithm(getInitParameter("PWD_AUTHENTICATION",
	 * "PBKDF2WithHmacSHA1")); }
	 */
	/**
	 * Get Safe Random file name
	 * 
	 * @return
	 */
	public static String GetSerializationFileName() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Get Common Date Time Format
	 * 
	 * @param oDate
	 * @return
	 */
	public static String GetFormatDate(Date oDate) {
		return Utils.GetFormatDate(oDate);
	}

	/**
	 * Get the User object from the session Id
	 * 
	 * @param sSessionId
	 * @return
	 */
	public static User GetUserFromSession(String sSessionId) {

		// validate sSessionId
		if (!m_oCredentialPolicy.validSessionId(sSessionId)) {
			return null;
		}

		if (s_bDebug) {
			User oUser = new User();
			oUser.setId(1);
			oUser.setUserId(s_sDebugUser);
			oUser.setName("Name");
			oUser.setSurname("Surname");
			oUser.setPassword(s_sDebugPassword);
			return oUser;
		} else {
			// Create Session Repository
			SessionRepository oSessionRepo = new SessionRepository();
			// Get The User Session
			UserSession oSession = oSessionRepo.getSession(sSessionId);

			if (Utils.isValidSession(oSession)) {
				// Create User Repo
				UserRepository oUserRepo = new UserRepository();
				// Get the user from the session
				User oUser = oUserRepo.getUser(oSession.getUserId());

				oSessionRepo.touchSession(oSession);

				return oUser;
			}

			// Session not valid
			oSessionRepo.deleteSession(oSession);

			// No Session, No User
			return null;

		}
	}

	/**
	 * Get the OS PID of a process
	 * 
	 * @param oProc
	 * @return
	 */
	public static Integer getPIDProcess(Process oProc) {
		Integer oPID = null;

		if (oProc.getClass().getName().equals("java.lang.UNIXProcess")) {
			// get the PID on unix/linux systems
			try {
				Field oField = oProc.getClass().getDeclaredField("pid");
				oField.setAccessible(true);
				oPID = oField.getInt(oProc);
				Utils.debugLog("WASDI.getPIDProcess: found PID " + oPID);
			} catch (Throwable e) {
				Utils.debugLog("WASDI.getPIDProcess: Error getting PID " + e.getMessage());
			}
		}

		return oPID;
	}

	public static String getWorkspacePath(ServletConfig oServletConfig, String sUserId, String sWorkspace) {
		// Take path
		String sDownloadRootPath = oServletConfig.getInitParameter("DownloadRootPath");

		if (Utils.isNullOrEmpty(sDownloadRootPath)) {
			sDownloadRootPath = "/data/wasdi/";
		}

		if (!sDownloadRootPath.endsWith("/")) {
			sDownloadRootPath = sDownloadRootPath + "/";
		}
		String sPath = sDownloadRootPath + sUserId + "/" + sWorkspace + "/";

		return sPath;
	}

	/**
	 * Get The owner of a workspace starting from the workspace id
	 * 
	 * @param sWorkspaceId Id of the Workspace
	 * @return User Id of the owner
	 */
	public static String getWorkspaceOwner(String sWorkspaceId) {
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
		if (oWorkspace == null)
			return "";
		String sWorkspaceOwner = oWorkspace.getUserId();
		return sWorkspaceOwner;
	}
}
