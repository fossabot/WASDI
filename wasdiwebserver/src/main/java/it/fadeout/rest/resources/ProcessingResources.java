package it.fadeout.rest.resources;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.rcp.imgfilter.FilteredBandAction;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.bc.ceres.jai.operator.PaintDescriptor;

import it.fadeout.Wasdi;
import it.fadeout.business.DownloadsThread;
import it.fadeout.business.ProcessingThread;
import wasdi.shared.LauncherOperations;
import wasdi.shared.SnapOperatorFactory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.CalibratorSetting;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.MultilookingSetting;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.NDVISetting;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingSetting;
import wasdi.shared.utils.BandImageManager;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandImageViewModel;
import wasdi.shared.viewmodels.SnapOperatorParameterViewModel;
import wasdi.shared.viewmodels.SnapWorkflowViewModel;

@Path("/processing")
public class ProcessingResources {
	
	@Context
	ServletConfig m_oServletConfig;
		
//	@GET
//	@Path("test")
//	@Produces({"application/xml", "application/json", "text/xml"})
//	public Response test() throws IOException
//	{
//		return ExecuteOperation("ciao", "test", "test", "test", null, LauncherOperations.TERRAIN);
//	}
	
	
	@POST
	@Path("geometric/rangeDopplerTerrainCorrection")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response TerrainCorrection(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, RangeDopplerGeocodingSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.TerrainCorrection");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.TERRAIN);
	}
	
	@POST
	@Path("radar/applyOrbit")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response ApplyOrbit(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, ApplyOrbitSetting oSetting) throws IOException
	{	
		Wasdi.DebugLog("ProcessingResources.ApplyOrbit");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.APPLYORBIT);
	}
	
	@POST
	@Path("radar/radiometricCalibration")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Calibrate(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, CalibratorSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.Calibrate");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.CALIBRATE);
	}
	
	@POST
	@Path("radar/multilooking")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Multilooking(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, MultilookingSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.Multilooking");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MULTILOOKING);

	}
	
	@POST
	@Path("optical/ndvi")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response NDVI(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, NDVISetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.NDVI");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.NDVI);
	}


	@GET
	@Path("parameters")
	@Produces({"application/json"})
	public SnapOperatorParameterViewModel[] OperatorParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sOperation") String sOperation) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.OperatorParameters");
		ArrayList<SnapOperatorParameterViewModel> oChoices = new ArrayList<SnapOperatorParameterViewModel>();
		
		Class oOperatorClass = SnapOperatorFactory.getOperatorClass(sOperation);
		
		Field[] aoOperatorFields = oOperatorClass.getDeclaredFields();
    	for (Field oOperatorField : aoOperatorFields) {
    		
    		if (oOperatorField.getName().equals("mapProjection")) {
    			System.out.println("ciao");
    		}
    		
			Annotation[] aoAnnotations = oOperatorField.getAnnotations();			
			for (Annotation oAnnotation : aoAnnotations) {
				
				if (oAnnotation instanceof Parameter) {
					Parameter oAnnotationParameter = (Parameter) oAnnotation;					
					
					SnapOperatorParameterViewModel oParameter = new SnapOperatorParameterViewModel();					
					oParameter.setField(oOperatorField.getName());
					
				    oParameter.setAlias(oAnnotationParameter.alias());
				    oParameter.setItemAlias(oAnnotationParameter.itemAlias());
				    oParameter.setDefaultValue(oAnnotationParameter.defaultValue());
				    oParameter.setLabel(oAnnotationParameter.label());
				    oParameter.setUnit(oAnnotationParameter.unit());
				    oParameter.setDescription(oAnnotationParameter.description());
				    oParameter.setValueSet(oAnnotationParameter.valueSet());
				    oParameter.setInterval(oAnnotationParameter.interval());
				    oParameter.setCondition(oAnnotationParameter.condition());
				    oParameter.setPattern(oAnnotationParameter.pattern());
				    oParameter.setFormat(oAnnotationParameter.format());
				    oParameter.setNotNull(oAnnotationParameter.notNull());
				    oParameter.setNotEmpty(oAnnotationParameter.notEmpty());
					
					oChoices.add(oParameter);					
				}
			}
		}
		
		
		return oChoices.toArray(new SnapOperatorParameterViewModel[oChoices.size()]);
	}
	
	
	/**
	 * Save a SNAP Workflow XML
	 * @param fileInputStream
	 * @param sSessionId
	 * @param workspace
	 * @param name
	 * @param description
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/uploadgraph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadGraph(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("name") String name, @QueryParam("description") String description) throws Exception {

		Wasdi.DebugLog("ProcessingResources.uploadGraph");
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return Response.status(401).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

		String sUserId = oUser.getUserId();
		
		String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
		if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
		
		String sWorkflowId =  UUID.randomUUID().toString();
		File humidityTifFile = new File(sDownloadRootPath+sUserId+ "/workflows/" + sWorkflowId + ".xml");
		
		Wasdi.DebugLog("ProcessingResources.uploadGraph: workflow file Path: " + humidityTifFile.getPath());
		
		//save uploaded file
		int read = 0;
		byte[] bytes = new byte[1024];
		OutputStream out = new FileOutputStream(humidityTifFile);
		while ((read = fileInputStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}
		out.flush();
		out.close();
		
		SnapWorkflow oWorkflow = new SnapWorkflow();
		oWorkflow.setName(name);
		oWorkflow.setDescription(description);
		oWorkflow.setFilePath(humidityTifFile.getPath());
		oWorkflow.setUserId(sUserId);
		oWorkflow.setWorkflowId(sWorkflowId);
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		oSnapWorkflowRepository.InsertSnapWorkflow(oWorkflow);
				
		return Response.ok().build();
	}
	
	/**
	 * Get workflow list by user id
	 * @param sSessionId
	 * @return
	 */
	@GET
	@Path("/getgraphsbyusr")
	public ArrayList<SnapWorkflowViewModel> getWorkflowsByUser(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("ProcessingResources.getWorkflowsByUser");
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		String sUserId = oUser.getUserId();
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		ArrayList<SnapWorkflowViewModel> aoRetWorkflows = new ArrayList<>();
		
		List<SnapWorkflow> aoDbWorkflows = oSnapWorkflowRepository.GetSnapWorkflowByUser(sUserId);
		
		for (int i=0; i<aoDbWorkflows.size(); i++) {
			SnapWorkflowViewModel oVM = new SnapWorkflowViewModel();
			oVM.setName(aoDbWorkflows.get(i).getName());
			oVM.setDescription(aoDbWorkflows.get(i).getDescription());
			oVM.setWorkflowId(aoDbWorkflows.get(i).getWorkflowId());
			
			aoRetWorkflows.add(oVM);
		}
		
		return aoRetWorkflows;
	}
	
	/**
	 * Delete a workflow from id
	 * @param sSessionId
	 * @param sWorkflowId
	 * @return
	 */
	@GET
	@Path("/deletegraph")
	public Response deleteWorkflow(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String sWorkflowId) {
		Wasdi.DebugLog("ProcessingResources.deleteWorkflow");
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		String sUserId = oUser.getUserId();
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		
		SnapWorkflow oWorkflow = oSnapWorkflowRepository.GetSnapWorkflow(sWorkflowId);
		 
		if (oWorkflow == null) return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		
		if (oWorkflow.getUserId().equals(sUserId) == false)  return Response.status(Status.UNAUTHORIZED).build();

		oSnapWorkflowRepository.DeleteSnapWorkflow(sWorkflowId);
	
		return Response.ok().build();
	}
	
	/**
	 * Executes a workflow from a file stream
	 * @param fileInputStream
	 * @param sessionId
	 * @param workspace
	 * @param sourceProductName
	 * @param destinationProdutName
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/graph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response executeGraph(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName) throws Exception {

		Wasdi.DebugLog("ProcessingResources.ExecuteGraph");
		
		if (Utils.isNullOrEmpty(sessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		GraphSetting settings = new GraphSetting();
		String graphXml;
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
		
	}
	
	@POST
	@Path("/graph_file")
	public Response executeGraphFromFile(@HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName) throws Exception {

		Wasdi.DebugLog("ProcessingResources.executeGraphFromFile");

		if (Utils.isNullOrEmpty(sessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		
		GraphSetting settings = new GraphSetting();		
		String graphXml;
		
		FileInputStream fileInputStream = new FileInputStream("/usr/lib/wasdi/S1_GRD_preprocessing.xml");
		
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
	}
	
	/**
	 * Exectues a Workflow from workflow Id
	 * @param sessionId
	 * @param workspace
	 * @param sourceProductName
	 * @param destinationProdutName
	 * @param workflowId
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/graph_id")
	public Response executeGraphFromWorkflowId(@HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName, @QueryParam("workflowId") String workflowId) throws Exception {

		Wasdi.DebugLog("ProcessingResources.executeGraphFromWorkflowId");
		
		if (Utils.isNullOrEmpty(sessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		String sUserId = oUser.getUserId();
		
		GraphSetting settings = new GraphSetting();		
		String graphXml;
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		SnapWorkflow oWF = oSnapWorkflowRepository.GetSnapWorkflow(workflowId);
		
		if (oWF == null) return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		if (oWF.getUserId().equals(sUserId)==false) return Response.status(Status.UNAUTHORIZED).build();
		
		FileInputStream fileInputStream = new FileInputStream(oWF.getFilePath());
		
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
	}

	@GET
	@Path("/standardfilters")
	@Produces({"application/json"})
	public Map<String, Filter[]> getStandardFilters(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("ProcessingResources.GetStandardFilters");
		
		Map<String, Filter[]> filtersMap = new HashMap<String, Filter[]>();
		filtersMap.put("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
		filtersMap.put("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
		filtersMap.put("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
		filtersMap.put("Sharpen", StandardFilters.SHARPENING_FILTERS);
		filtersMap.put("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
		filtersMap.put("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
		filtersMap.put("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);
		return filtersMap;
	}
	
	@POST
	@Path("/bandimage")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getBandImage(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String workspace,
			BandImageViewModel model) throws IOException {
		
		Wasdi.DebugLog("ProcessingResources.getBandImage");
		
		OperationRegistry operationRegistry = JAI.getDefaultInstance().getOperationRegistry();
		RegistryElementDescriptor oDescriptor = operationRegistry.getDescriptor("rendered", "Paint");
		
		if (oDescriptor==null) {
			System.out.println("getBandImage: REGISTER Descriptor!!!!");
			try {
				operationRegistry.registerServices(this.getClass().getClassLoader());
			} catch (Exception e) {
				e.printStackTrace();
			}
			oDescriptor = operationRegistry.getDescriptor("rendered", "Paint");
			
			
			IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
		}
		
		

		String userId = AcceptedUserAndSession(sSessionId);
		if (Utils.isNullOrEmpty(userId)) return Response.status(401).build();
		
        String downloadPath = m_oServletConfig.getInitParameter("DownloadRootPath");
        File productFile = new File(new File(new File(downloadPath, userId), workspace), model.getProductFileName());
		
//		File productFile = new File("/home/doy/tmp/wasdi/tmp/S2B_MSIL1C_20180117T102339_N0206_R065_T32TMQ_20180117T122826.zip");
        
        if (!productFile.exists()) {
        	System.out.println("ProcessingResource.getBandImage: FILE NOT FOUND: " + productFile.getAbsolutePath());
        	return Response.status(500).build();
        }
        
        Product oSNAPProduct = ProductIO.readProduct(productFile);
        
        if (oSNAPProduct == null) {
        	Wasdi.DebugLog("ProcessingResources.getBandImage: SNAP product is null, impossibile to read. Return");
        	return Response.status(500).build();
        } else {
        	Wasdi.DebugLog("ProcessingResources.getBandImage: product read");
        }
        
		BandImageManager manager = new BandImageManager(oSNAPProduct);
		
		RasterDataNode raster = null;
		
		if (model.getFilterVM() != null) {
			Filter filter = model.getFilterVM().getFilter();
			FilterBand filteredBand = manager.getFilterBand(model.getBandName(), filter, model.getFilterIterationCount());
			if (filteredBand == null) {
				Wasdi.DebugLog("ProcessingResource.getBandImage: CANNOT APPLY FILTER TO BAND " + model.getBandName());
	        	return Response.status(500).build();
			}
			raster = filteredBand.getSource();
		} else {
			raster = oSNAPProduct.getBand(model.getBandName());
		}
		
		if (model.getVp_x()<0||model.getVp_y()<0||model.getImg_w()<=0||model.getImg_h()<=0) {
			Wasdi.DebugLog("ProcessingResources.getBandImage: Invalid Parameters: VPX= " + model.getVp_x() +" VPY= "+ model.getVp_y() +" VPW= "+ model.getVp_w() +" VPH= "+ model.getVp_h() + " OUTW = " + model.getImg_w() + " OUTH = " +model.getImg_h() );
			return Response.status(500).build();
		} else {
			Wasdi.DebugLog("ProcessingResources.getBandImage: parameters OK");
		}
		
		Rectangle vp = new Rectangle(model.getVp_x(), model.getVp_y(), model.getVp_w(), model.getVp_h());
		Dimension imgSize = new Dimension(model.getImg_w(), model.getImg_h());
		
		BufferedImage img;
		try {
			img = manager.buildImage(raster, imgSize, vp);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(500).build();
		}
		
		if (img == null) {
			Wasdi.DebugLog("ProcessingResource.getBandImage: img null");
			return Response.status(500).build();
		}
		
		Wasdi.DebugLog("ProcessingResource.getBandImage: Generated image for band " + model.getBandName() + " X= " + model.getVp_x() + " Y= " + model.getVp_y() + " W= " + model.getVp_w() + " H= "  + model.getVp_h());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ImageIO.write(img, "jpg", baos);
	    byte[] imageData = baos.toByteArray();
		
		return Response.ok(imageData).build();
	}
	
	
	private String AcceptedUserAndSession(String sSessionId) {
		//Check user
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		return oUser.getUserId();
	}
	
	private Response ExecuteOperation(String sSessionId, String sSourceProductName, String sDestinationProductName, String sWorkspaceId, ISetting oSetting, LauncherOperations operation) {
		
		String sUserId = AcceptedUserAndSession(sSessionId);
		
		if (Utils.isNullOrEmpty(sUserId)) return Response.status(401).build();
		
		try {
			//Update process list
			String sProcessId = "";
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcess = new ProcessWorkspace();
			
			try
			{
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(operation.name());
				oProcess.setProductName(sSourceProductName);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(Utils.GetRandomName());
				oProcess.setStatus(ProcessStatus.CREATED.name());
				sProcessId = oRepository.InsertProcessWorkspace(oProcess);
			}
			catch(Exception oEx){
				System.out.println("SnapOperations.ExecuteOperation: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + oProcess.getProcessObjId();

			
			OperatorParameter oParameter = GetParameter(operation); 
			oParameter.setSourceProductName(sSourceProductName);
			oParameter.setDestinationProductName(sDestinationProductName);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(oProcess.getProcessObjId());
			if (oSetting != null) oParameter.setSettings(oSetting);	
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

//			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
//			String sJavaExe = m_oServletConfig.getInitParameter("JavaExe");
//
//			String sShellExString = sJavaExe + " -jar " + sLauncherPath +" -operation " + operation + " -parameter " + sPath;
//
//			System.out.println("SnapOperations.ExecuteOperation: shell exec " + sShellExString);
//
//			Process oProc = Runtime.getRuntime().exec(sShellExString);


		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
		
		return Response.ok().build();
	}

	
	private OperatorParameter GetParameter(LauncherOperations op)
	{
		switch (op) {
		case APPLYORBIT:
			return new ApplyOrbitParameter();
		case CALIBRATE:
			return new CalibratorParameter();
		case MULTILOOKING:
			return new MultilookingParameter();
		case TERRAIN:
			return new RangeDopplerGeocodingParameter();
		case NDVI:
			return new NDVIParameter();
			
		case GRAPH:
			return new GraphParameter();
		default:
			return null;
			
		}
	}
	
	
}
