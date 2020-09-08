package it.fadeout.rest.resources;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.jexp.impl.Tokenizer;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.bc.ceres.binding.PropertyContainer;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import wasdi.shared.LauncherOperations;
import wasdi.shared.SnapOperatorFactory;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.User;
import wasdi.shared.business.WpsProvider;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.WpsProvidersRepository;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.CalibratorSetting;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.MosaicSetting;
import wasdi.shared.parameters.MultiSubsetParameter;
import wasdi.shared.parameters.MultiSubsetSetting;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.MultilookingSetting;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.NDVISetting;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingSetting;
import wasdi.shared.parameters.RegridParameter;
import wasdi.shared.parameters.RegridSetting;
import wasdi.shared.parameters.SubsetParameter;
import wasdi.shared.parameters.SubsetSetting;
import wasdi.shared.utils.BandImageManager;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandImageViewModel;
import wasdi.shared.viewmodels.ColorManipulationViewModel;
import wasdi.shared.viewmodels.MaskViewModel;
import wasdi.shared.viewmodels.MathMaskViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductMaskViewModel;
import wasdi.shared.viewmodels.RangeMaskViewModel;
import wasdi.shared.viewmodels.SnapOperatorParameterViewModel;
import wasdi.shared.viewmodels.SnapWorkflowViewModel;
import wasdi.shared.viewmodels.WpsViewModel;

@Path("/processing")
public class ProcessingResources {

	@Context
	ServletConfig m_oServletConfig;

	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();

	@POST
	@Path("geometric/rangeDopplerTerrainCorrection")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult terrainCorrection(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sSourceProductName") String sSourceProductName,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId, 
			@QueryParam("parent") String sParentId, RangeDopplerGeocodingSetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.TerrainCorrection( Session " + sSessionId + ", Source: " + sSourceProductName + ", Dest: " + sDestinationProductName + ", Ws: " + sWorkspaceId + ", ... )");
		return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.TERRAIN, sParentId);
	}

	@POST
	@Path("radar/applyOrbit")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult applyOrbit(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sSourceProductName") String sSourceProductName,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId, 
			@QueryParam("parent") String sParentId, ApplyOrbitSetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.ApplyOrbit( Session: " + sSessionId + ", Dest: " + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
		return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.APPLYORBIT, sParentId);
	}

	@POST
	@Path("radar/radiometricCalibration")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult calibrate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sSourceProductName") String sSourceProductName,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId, 
			@QueryParam("parent") String sParentId, CalibratorSetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.Calibrate( Session: " + sSessionId + ", Source: " + sSourceProductName + ", Dest: " + sDestinationProductName + ", Ws: " + sWorkspaceId + ", ... )");
		return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.CALIBRATE, sParentId);
	}

	@POST
	@Path("radar/multilooking")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult multilooking(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sSourceProductName") String sSourceProductName,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId, 
			@QueryParam("parent") String sParentId, MultilookingSetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.Multilooking( Session: " + sSessionId + ", Source: " + sSourceProductName + ", Dest: " + sDestinationProductName + ", Ws: " + sWorkspaceId + ")");
		return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MULTILOOKING, sParentId);

	}

	@POST
	@Path("optical/ndvi")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult NDVI(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sSourceProductName") String sSourceProductName,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId,
			@QueryParam("parent") String sParentId, NDVISetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.NDVI( Session: " + sSessionId + ", Source: " + sSourceProductName + ", Destination: " + sDestinationProductName + ", Ws: " + sWorkspaceId + ", ... )");
		return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.NDVI, sParentId);
	}

	@POST
	@Path("geometric/mosaic")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult mosaic(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId, 
			@QueryParam("parent") String sParentId, MosaicSetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.Mosaic( Session: " + sSessionId + ", Destination: " + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
		return executeOperation(sSessionId, "", sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MOSAIC, sParentId);
	}

	@POST
	@Path("geometric/regrid")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult regrid(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId, 
			@QueryParam("parent") String sParentId, RegridSetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.Regrid( Session: " + sSessionId + ", Dest: " + sDestinationProductName + ", Ws: " + sWorkspaceId + ", ... )");
		return executeOperation(sSessionId, "", sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.REGRID, sParentId);
	}

	@POST
	@Path("geometric/subset")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult subset(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sSourceProductName") String sSourceProductName,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId,
			@QueryParam("parent") String sParentId, SubsetSetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.Subset( Session: " + sSessionId + ", Source: " + sSourceProductName + ", Dest:" + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
		return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.SUBSET, sParentId);
	}

	@POST
	@Path("geometric/multisubset")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult multiSubset(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sSourceProductName") String sSourceProductName,
			@QueryParam("sDestinationProductName") String sDestinationProductName,
			@QueryParam("sWorkspaceId") String sWorkspaceId, 
			@QueryParam("parent") String sParentId, MultiSubsetSetting oSetting) throws IOException {
		Utils.debugLog("ProcessingResources.MultiSubset( Session: " + sSessionId + ", Source: " + sSourceProductName + ", Dest: " + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
		return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MULTISUBSET, sParentId);
	}

	@GET
	@Path("parameters")
	@Produces({ "application/json" })
	public SnapOperatorParameterViewModel[] operatorParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sOperation") String sOperation) throws IOException {
		Utils.debugLog("ProcessingResources.operatorParameters( Session: " + sSessionId + ", Operation: " + sOperation + " )");
		ArrayList<SnapOperatorParameterViewModel> oChoices = new ArrayList<SnapOperatorParameterViewModel>();

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if(null == oUser) {
			Utils.debugLog("ProcessingResources.operatorParameters( Session: " + sSessionId + ", Operation: " + sOperation + " ): invalid session");
			return oChoices.toArray(new SnapOperatorParameterViewModel[oChoices.size()]);
		}
		try {
			Class oOperatorClass = SnapOperatorFactory.getOperatorClass(sOperation);

			Field[] aoOperatorFields = oOperatorClass.getDeclaredFields();
			for (Field oOperatorField : aoOperatorFields) {

				if (oOperatorField.getName().equals("mapProjection")) {
					Utils.debugLog("operatorParameters found mapProjection parameter");
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
		} catch (Exception oE) {
			Utils.debugLog("ProcessingResources.operatorParameters( Session: " + sSessionId + ", Operation: " + sOperation + " ): " + oE);
		}

		return oChoices.toArray(new SnapOperatorParameterViewModel[oChoices.size()]);
	}

	/**
	 * Save a SNAP Workflow XML
	 * 
	 * @param fileInputStream
	 * @param sSessionId
	 * @param sWorkspace
	 * @param sName
	 * @param sDescription
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/uploadgraph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadGraph(@FormDataParam("file") InputStream fileInputStream,
			@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspace,
			@QueryParam("name") String sName, @QueryParam("description") String sDescription,
			@QueryParam("public") Boolean bPublic) throws Exception {

		Utils.debugLog("ProcessingResources.uploadGraph( InputStream, Session: " + sSessionId + ", Ws: " + sWorkspace + ", Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " )");
		
		OutputStream oOutStream = null;

		try {
			// Check authorization
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessingResources.uploadGraph( InputStream, Session: " + sSessionId + ", Ws: " + sWorkspace + ", Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " ): invalid session");
				return Response.status(401).build();
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

			String sUserId = oUser.getUserId();

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);

			File oWorkflowsPath = new File(sDownloadRootPath + "workflows/");

			if (!oWorkflowsPath.exists()) {
				oWorkflowsPath.mkdirs();
			}

			// Generate Workflow Id and file
			String sWorkflowId = UUID.randomUUID().toString();
			File oWorkflowXmlFile = new File(sDownloadRootPath + "workflows/" + sWorkflowId + ".xml");

			Utils.debugLog("ProcessingResources.uploadGraph: workflow file Path: " + oWorkflowXmlFile.getPath());

			// save uploaded file
			int iRead = 0;
			byte[] ayBytes = new byte[1024];

			oOutStream = new FileOutputStream(oWorkflowXmlFile);

			while ((iRead = fileInputStream.read(ayBytes)) != -1) {
				oOutStream.write(ayBytes, 0, iRead);
			}

			oOutStream.flush();
			// Close it in the finally clause
			//oOutStream.close();

			// Create Entity
			SnapWorkflow oWorkflow = new SnapWorkflow();
			oWorkflow.setName(sName);
			oWorkflow.setDescription(sDescription);
			oWorkflow.setFilePath(oWorkflowXmlFile.getPath());
			oWorkflow.setUserId(sUserId);
			oWorkflow.setWorkflowId(sWorkflowId);

			if (bPublic == null) oWorkflow.setIsPublic(false);
			else oWorkflow.setIsPublic(bPublic.booleanValue());

			if (Wasdi.getActualNode() != null) {
				oWorkflow.setNodeCode(Wasdi.getActualNode().getNodeCode());
				oWorkflow.setNodeUrl(Wasdi.getActualNode().getNodeBaseAddress());
			}

			// Read the graph
			Graph oGraph = GraphIO.read(new FileReader(oWorkflowXmlFile));

			// Take the nodes
			Node[] aoNodes = oGraph.getNodes();

			for (int iNodes = 0; iNodes < aoNodes.length; iNodes++) {
				Node oNode = aoNodes[iNodes];
				// Search Read and Write nodes
				if (oNode.getOperatorName().equals("Read")) {
					oWorkflow.getInputNodeNames().add(oNode.getId());
				} else if (oNode.getOperatorName().equals("Write")) {
					oWorkflow.getOutputNodeNames().add(oNode.getId());
				}
			}

			// Save the Workflow
			SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
			oSnapWorkflowRepository.insertSnapWorkflow(oWorkflow);

		} catch (Exception oEx) {
			Utils.debugLog("ProcessingResources.uploadGraph: " + oEx);
			return Response.serverError().build();
		}
		finally {
			if (oOutStream != null) {
				try {
					oOutStream.close();
				}
				catch (Exception oEx) {
					Utils.debugLog("ProcessingResources.uploadGraph: Error " + oEx.toString());
				}
			}
		}

		return Response.ok().build();
	}

	/**
	 * Get workflow list by user id
	 * 
	 * @param sSessionId
	 * @return
	 */
	@GET
	@Path("/getgraphsbyusr")
	public ArrayList<SnapWorkflowViewModel> getWorkflowsByUser(@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog("ProcessingResources.getWorkflowsByUser( Session: " + sSessionId + " )");

		if (Utils.isNullOrEmpty(sSessionId)) {
			Utils.debugLog("ProcessingResources.getWorkflowsByUser: session null");
			return null;
		}
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("ProcessingResources.getWorkflowsByUser( " + sSessionId + " ): invalid session");
			return null;
		}

		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			Utils.debugLog("ProcessingResources.getWorkflowsByUser: user id null");
			return null;
		}

		String sUserId = oUser.getUserId();

		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		ArrayList<SnapWorkflowViewModel> aoRetWorkflows = new ArrayList<>();
		try {

			List<SnapWorkflow> aoDbWorkflows = oSnapWorkflowRepository.getSnapWorkflowPublicAndByUser(sUserId);

			for (int i = 0; i < aoDbWorkflows.size(); i++) {
				SnapWorkflowViewModel oVM = new SnapWorkflowViewModel();
				oVM.setName(aoDbWorkflows.get(i).getName());
				oVM.setDescription(aoDbWorkflows.get(i).getDescription());
				oVM.setWorkflowId(aoDbWorkflows.get(i).getWorkflowId());
				oVM.setOutputNodeNames(aoDbWorkflows.get(i).getOutputNodeNames());
				oVM.setInputNodeNames(aoDbWorkflows.get(i).getInputNodeNames());
				oVM.setPublic(aoDbWorkflows.get(i).getIsPublic());
				oVM.setUserId(aoDbWorkflows.get(i).getUserId());
				oVM.setNodeUrl(aoDbWorkflows.get(i).getNodeUrl());
				aoRetWorkflows.add(oVM);
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessingResources.getWorkflowsByUser( " + sSessionId + " ): " + oE);
		}

		Utils.debugLog("ProcessingResources.getWorkflowsByUser: return " + aoRetWorkflows.size() + " workflows");

		return aoRetWorkflows;
	}

	/**
	 * Delete a workflow from id
	 * 
	 * @param sSessionId
	 * @param sWorkflowId
	 * @return
	 */
	@GET
	@Path("/deletegraph")
	public Response deleteGraph(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String sWorkflowId) {
		Utils.debugLog("ProcessingResources.deleteWorkflow( Session: " + sSessionId + ", Workflow: " + sWorkflowId + " )");
		try {
			// Check User
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("ProcessingResources.deleteWorkflow( Session: " + sSessionId + ", Workflow: " + sWorkflowId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

			String sUserId = oUser.getUserId();

			SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
			SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);

			if (oWorkflow == null) return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			if (oWorkflow.getUserId().equals(sUserId) == false) return Response.status(Status.UNAUTHORIZED).build();

			// Get Download Path
			String sBasePath = Wasdi.getDownloadPath(m_oServletConfig);
			sBasePath += "workflows/";
			String sWorkflowFilePath = sBasePath + oWorkflow.getWorkflowId() + ".xml";

			if (!Utils.isNullOrEmpty(sWorkflowFilePath)) {
				File oWorkflowFile = new File(sWorkflowFilePath);
				if (oWorkflowFile.exists()) {
					if (!oWorkflowFile.delete()) {
						Utils.debugLog("ProcessingResource.deleteWorkflow: Error deleting the workflow file " + oWorkflow.getFilePath());
					}
				}
			} else {
				Utils.debugLog("ProcessingResource.deleteWorkflow: workflow file path is null or empty.");
			}

			oSnapWorkflowRepository.deleteSnapWorkflow(sWorkflowId);
		} catch (Exception oE) {
			Utils.debugLog("ProcessingResources.deleteWorkflow( Session: " + sSessionId + ", Workflow: " + sWorkflowId + " ): " + oE);
			return Response.serverError().build();
		}
		return Response.ok().build();
	}

	/**
	 * Executes a workflow from a file stream
	 * 
	 * @param fileInputStream
	 * @param sSessionId
	 * @param sWorkspace
	 * @param sSourceProductName
	 * @param sDestinationProdutName
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/graph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public PrimitiveResult executeGraph(@FormDataParam("file") InputStream fileInputStream,
			@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspace,
			@QueryParam("source") String sSourceProductName, @QueryParam("destination") String sDestinationProdutName, @QueryParam("parent") String sParentProcessWorkspaceId)
					throws Exception {

		Utils.debugLog("ProcessingResources.ExecuteGraph( InputStream, " + sSessionId + ", Ws: " + sWorkspace + ", Source: " + sSourceProductName + ", Dest: " + sDestinationProdutName + " )");
		PrimitiveResult oResult = new PrimitiveResult();

		if (Utils.isNullOrEmpty(sSessionId)) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("ProcessingResources.ExecuteGraph( InputStream, " + sSessionId + ", Ws: " + sWorkspace + ", Source: " + sSourceProductName + ", Dest: " + sDestinationProdutName + " ): invalid session");
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}

		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}

		GraphSetting oSettings = new GraphSetting();
		String sGraphXml;
		sGraphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		oSettings.setGraphXml(sGraphXml);

		return executeOperation(sSessionId, sSourceProductName, sDestinationProdutName, sWorkspace, oSettings, LauncherOperations.GRAPH, sParentProcessWorkspaceId);

	}


	/**
	 * Exectues a Workflow from workflow Id
	 * 
	 * @param sSessionId
	 * @param sWorkspace
	 * @param sourceProductName
	 * @param destinationProdutName
	 * @param workflowId
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/graph_id")
	public PrimitiveResult executeGraphFromWorkflowId(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspace, @QueryParam("parent") String sParentProcessWorkspaceId, SnapWorkflowViewModel oSnapWorkflowViewModel) throws Exception {

		PrimitiveResult oResult = new PrimitiveResult();
		Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId( Session: " + sSessionId + ", Ws: " + sWorkspace + ", ... )");

		if (Utils.isNullOrEmpty(sSessionId)) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId( Session: " + sSessionId + ", Ws: " + sWorkspace + ", ... ): invalid session");
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}

		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		
		FileInputStream oFileInputStream = null;

		try {
			String sUserId = oUser.getUserId();

			GraphSetting oGraphSettings = new GraphSetting();
			String sGraphXml;

			SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
			SnapWorkflow oWF = oSnapWorkflowRepository.getSnapWorkflow(oSnapWorkflowViewModel.getWorkflowId());

			if (oWF == null) {
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;
			}
			if (oWF.getUserId().equals(sUserId) == false && oWF.getIsPublic() == false) {
				oResult.setBoolValue(false);
				oResult.setIntValue(401);
				return oResult;
			}

			String sBasePath = Wasdi.getDownloadPath(m_oServletConfig);
			String sWorkflowPath = sBasePath + "workflows/" + oWF.getWorkflowId() + ".xml";
			File oWorkflowFile = new File(sWorkflowPath);

			if (!oWorkflowFile.exists()) {
				Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId: Workflow file not on this node. Try to download it");

				String sDownloadedWorflowPath = Wasdi.downloadWorkflow(oWF.getNodeUrl(),oWF.getWorkflowId(), sSessionId, m_oServletConfig);

				if (Utils.isNullOrEmpty(sDownloadedWorflowPath)) {
					Utils.debugLog("Error downloading workflow. Return error");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;
				}

				sWorkflowPath = sDownloadedWorflowPath;
			}

			oFileInputStream = new FileInputStream(sWorkflowPath);

			String sWorkFlowName = oWF.getName().replace(' ', '_');

			sGraphXml = IOUtils.toString(oFileInputStream, Charset.defaultCharset().name());
			oGraphSettings.setGraphXml(sGraphXml);
			oGraphSettings.setWorkflowName(sWorkFlowName);

			oGraphSettings.setInputFileNames(oSnapWorkflowViewModel.getInputFileNames());
			oGraphSettings.setInputNodeNames(oSnapWorkflowViewModel.getInputNodeNames());
			oGraphSettings.setOutputFileNames(oSnapWorkflowViewModel.getOutputFileNames());
			oGraphSettings.setOutputNodeNames(oSnapWorkflowViewModel.getOutputNodeNames());

			String sSourceProductName = "";
			String sDestinationProdutName = "";

			if (oSnapWorkflowViewModel.getInputFileNames().size() > 0) {
				sSourceProductName = oSnapWorkflowViewModel.getInputFileNames().get(0);
				// TODO: Output file name
				sDestinationProdutName = sSourceProductName + "_" + sWorkFlowName;
			}

			return executeOperation(sSessionId, sSourceProductName, sDestinationProdutName, sWorkspace, oGraphSettings, LauncherOperations.GRAPH, sParentProcessWorkspaceId);
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId: Error " + oEx.toString());
			oResult.setBoolValue(false);
			oResult.setIntValue(500);
			return oResult;
		}
		finally {
			if (oFileInputStream != null) {
				try {
					oFileInputStream.close();
				}
				catch (Exception oEx) {
					Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId: Error " + oEx.toString());
				}				
			}
		}
	}


	@GET
	@Path("downloadgraph")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadGraphById(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("workflowId") String sWorkflowId)
	{			

		Utils.debugLog("ProcessingResource.downloadGraphByName( Session: " + sSessionId + ", WorkflowId: " + sWorkflowId + " )");

		try {

			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				Utils.debugLog("ProcessingResource.downloadGraphByName( Session: " + sSessionId + ", WorkflowId: " + sWorkflowId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);
			String sWorkflowXmlPath = sDownloadRootPath + "workflows/" + sWorkflowId + ".xml";

			File oFile = new File(sWorkflowXmlPath);

			ResponseBuilder oResponseBuilder = null;

			if(oFile.exists()==false) {
				Utils.debugLog("ProcessingResource.downloadGraphByName: file does not exists " + oFile.getPath());
				oResponseBuilder = Response.serverError();	
			} 
			else {

				Utils.debugLog("ProcessingResource.downloadGraphByName: returning file " + oFile.getPath());

				FileStreamingOutput oStream;
				oStream = new FileStreamingOutput(oFile);

				oResponseBuilder = Response.ok(oStream);
				oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
				oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
			}

			return oResponseBuilder.build();

		} 
		catch (Exception oEx) {
			Utils.debugLog("ProcessingResource.downloadGraphByName: " + oEx);
		}

		return null;
	}




	@GET
	@Path("/standardfilters")
	@Produces({ "application/json" })
	public Map<String, Filter[]> getStandardFilters(@HeaderParam("x-session-token") String sSessionId) {

		Utils.debugLog("ProcessingResources.GetStandardFilters( " + sSessionId + " )");

		Map<String, Filter[]> aoFiltersMap = new HashMap<String, Filter[]>();
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				Utils.debugLog("ProcessingResources.GetStandardFilters( " + sSessionId + " ): invalid session");
				return aoFiltersMap;

			}	

			aoFiltersMap.put("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
			aoFiltersMap.put("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
			aoFiltersMap.put("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
			aoFiltersMap.put("Sharpen", StandardFilters.SHARPENING_FILTERS);
			aoFiltersMap.put("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
			aoFiltersMap.put("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
			aoFiltersMap.put("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);
		} catch (Exception oE) {
			Utils.debugLog("ProcessingResources.GetStandardFilters( " + sSessionId + " ): " + oE);
		}
		return aoFiltersMap;
	}

	@GET
	@Path("/productmasks")
	@Produces({ "application/json" })
	public ArrayList<ProductMaskViewModel> getProductMasks(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("file") String sProductFile, @QueryParam("band") String sBandName,
			@QueryParam("workspaceId") String sWorkspaceId) throws Exception {

		Utils.debugLog("ProcessingResources.getProductMasks");

		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("ProcessingResources.getProductMasks( " + sSessionId + ", " + sProductFile + ", " +
					sBandName + ", " + sWorkspaceId + " ): invalid session");	
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		Utils.debugLog("Params. File: " + sProductFile + " - Band: " + sBandName + " - Workspace: " + sWorkspaceId);

		String sProductFileFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId),
				sWorkspaceId) + sProductFile;

		Utils.debugLog("ProcessingResources.getProductMasks: file Path: " + sProductFileFullPath);

		ArrayList<ProductMaskViewModel> aoMasks = new ArrayList<ProductMaskViewModel>();

		try {
			Product product = ProductIO.readProduct(sProductFileFullPath);
			Band band = product.getBand(sBandName);

			final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
			for (int i = 0; i < maskGroup.getNodeCount(); i++) {
				final Mask mask = maskGroup.get(i);
				if (mask.getRasterWidth() == band.getRasterWidth()
						&& mask.getRasterHeight() == band.getRasterHeight()) {
					ProductMaskViewModel vm = new ProductMaskViewModel();
					vm.setName(mask.getName());
					vm.setDescription(mask.getDescription());
					vm.setMaskType(mask.getImageType().getName());
					vm.setColorRed(mask.getImageColor().getRed());
					vm.setColorGreen(mask.getImageColor().getGreen());
					vm.setColorBlue(mask.getImageColor().getBlue());
					aoMasks.add(vm);
				}
			}
		} catch (Exception oEx) {
			Utils.debugLog("ProcessingResources.getProductMasks: " + oEx);
		}

		return aoMasks;
	}

	@GET
	@Path("/productcolormanipulation")
	@Produces({ "application/json" })
	public ColorManipulationViewModel getColorManipulation(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("file") String sProductFile, @QueryParam("band") String sBandName,
			@QueryParam("accurate") boolean bAccurate, @QueryParam("workspaceId") String sWorkspaceId)
					throws Exception {
		try {
			Utils.debugLog("ProcessingResources.getColorManipulation( Session: " + sSessionId + ", Product: " + sProductFile + ", Band:" + sBandName + ", Accurate: " + bAccurate + ", WS: " + sWorkspaceId + " )");

			if (Utils.isNullOrEmpty(sSessionId)) return null;
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("ProcessingResources.getColorManipulation( Session: " + sSessionId +
						", Product: " + sProductFile + ", Band:" + sBandName + ", Accurate: " + bAccurate +
						", WS: " + sWorkspaceId + " ): invalid session");
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return null;


			Utils.debugLog("ProcessingResources.getColorManipulation. Params. File: " + sProductFile + " - Band: " + sBandName + " - Workspace: " + sWorkspaceId);

			String sProductFileFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

			Utils.debugLog("ProcessingResources.getColorManipulation: file Path: " + sProductFileFullPath);

			Product product = ProductIO.readProduct(sProductFileFullPath);
			BandImageManager manager = new BandImageManager(product);
			return manager.getColorManipulation(sBandName, bAccurate);
		} catch (Exception oE) {
			Utils.debugLog("ProcessingResources.getColorManipulation( Session: " + sSessionId + ", Product: " + sProductFile +
					", Band:" + sBandName + ", Accurate: " + bAccurate + ", WS: " + sWorkspaceId + " ): " + oE);
		}
		return null;
	}

	@POST
	@Path("/bandimage")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getBandImage(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspace, BandImageViewModel oBandImageViewModel) throws IOException {

		try {
			Utils.debugLog("ProcessingResources.getBandImage( Session: " + sSessionId + ", WS: " + sWorkspace + ", ... )");

			// Check user session
			String sUserId = acceptedUserAndSession(sSessionId);
			if (Utils.isNullOrEmpty(sUserId)) {
				Utils.debugLog("ProcessingResources.getBandImage( Session: " + sSessionId + ", WS: " + sWorkspace + ", ... ): invalid session");
				return Response.status(401).build();
			}

			// Init the registry for JAI
			OperationRegistry oOperationRegistry = JAI.getDefaultInstance().getOperationRegistry();
			RegistryElementDescriptor oDescriptor = oOperationRegistry.getDescriptor("rendered", "Paint");

			if (oDescriptor == null) {
				Utils.debugLog("getBandImage: REGISTER Descriptor!!!!");
				try {
					oOperationRegistry.registerServices(this.getClass().getClassLoader());
				} catch (Exception e) {
					Utils.debugLog("ProcessingResources.getBandImage: " + e);
				}
				oDescriptor = oOperationRegistry.getDescriptor("rendered", "Paint");

				IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
			}

			String sProductPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace);
			File oProductFile = new File(sProductPath + oBandImageViewModel.getProductFileName());

			if (!oProductFile.exists()) {
				Utils.debugLog("ProcessingResource.getBandImage: FILE NOT FOUND: " + oProductFile.getAbsolutePath());
				return Response.status(500).build();
			}

			Product oSNAPProduct = ProductIO.readProduct(oProductFile);

			if (oSNAPProduct == null) {
				Utils.debugLog("ProcessingResources.getBandImage: SNAP product is null, impossibile to read. Return");
				return Response.status(500).build();
			} else {
				Utils.debugLog("ProcessingResources.getBandImage: product read");
			}

			BandImageManager oBandImageManager = new BandImageManager(oSNAPProduct);

			RasterDataNode oRasterDataNode = null;

			if (oBandImageViewModel.getFilterVM() != null) {
				Filter oFilter = oBandImageViewModel.getFilterVM().getFilter();
				FilterBand oFilteredBand = oBandImageManager.getFilterBand(oBandImageViewModel.getBandName(), oFilter, oBandImageViewModel.getFilterIterationCount());

				if (oFilteredBand == null) {
					Utils.debugLog("ProcessingResource.getBandImage: CANNOT APPLY FILTER TO BAND " + oBandImageViewModel.getBandName());
					return Response.status(500).build();
				}
				oRasterDataNode = oFilteredBand;
			} 
			else {
				oRasterDataNode = oSNAPProduct.getBand(oBandImageViewModel.getBandName());
			}

			if (oBandImageViewModel.getVp_x() < 0 || oBandImageViewModel.getVp_y() < 0
					|| oBandImageViewModel.getImg_w() <= 0 || oBandImageViewModel.getImg_h() <= 0) {
				Utils.debugLog("ProcessingResources.getBandImage: Invalid Parameters: VPX= " + oBandImageViewModel.getVp_x()
				+ " VPY= " + oBandImageViewModel.getVp_y() + " VPW= " + oBandImageViewModel.getVp_w() + " VPH= "
				+ oBandImageViewModel.getVp_h() + " OUTW = " + oBandImageViewModel.getImg_w() + " OUTH = "
				+ oBandImageViewModel.getImg_h());
				return Response.status(500).build();
			} 
			else {
				Utils.debugLog("ProcessingResources.getBandImage: parameters OK");
			}

			Rectangle oRectangleViewPort = new Rectangle(oBandImageViewModel.getVp_x(), oBandImageViewModel.getVp_y(), oBandImageViewModel.getVp_w(), oBandImageViewModel.getVp_h());
			Dimension oImgSize = new Dimension(oBandImageViewModel.getImg_w(), oBandImageViewModel.getImg_h());

			// apply product masks
			List<ProductMaskViewModel> aoProductMasksModels = oBandImageViewModel.getProductMasks();
			if (aoProductMasksModels != null) {
				for (ProductMaskViewModel oMaskModel : aoProductMasksModels) {
					Mask oMask = oSNAPProduct.getMaskGroup().get(oMaskModel.getName());
					if (oMask == null) {
						Utils.debugLog("ProcessingResources.getBandImage: cannot find mask by name: " + oMaskModel.getName());
					} else {
						// set the user specified color
						oMask.setImageColor(new Color(oMaskModel.getColorRed(), oMaskModel.getColorGreen(), oMaskModel.getColorBlue()));
						oMask.setImageTransparency(oMaskModel.getTransparency());
						oRasterDataNode.getOverlayMaskGroup().add(oMask);
					}
				}
			}

			// applying range masks
			List<RangeMaskViewModel> aoRangeMasksModels = oBandImageViewModel.getRangeMasks();
			if (aoRangeMasksModels != null) {
				for (RangeMaskViewModel oMaskModel : aoRangeMasksModels) {

					Mask oMask = createMask(oSNAPProduct, oMaskModel, Mask.RangeType.INSTANCE);

					String sExternalName = Tokenizer.createExternalName(oBandImageViewModel.getBandName());
					PropertyContainer oImageConfig = oMask.getImageConfig();
					oImageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, oMaskModel.getMin());
					oImageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, oMaskModel.getMax());
					oImageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, sExternalName);
					oSNAPProduct.addMask(oMask);
					oRasterDataNode.getOverlayMaskGroup().add(oMask);
				}
			}

			// applying math masks
			List<MathMaskViewModel> aoMathMasksModels = oBandImageViewModel.getMathMasks();
			if (aoMathMasksModels != null) {
				for (MathMaskViewModel oMaskModel : aoMathMasksModels) {

					Mask oMask = createMask(oSNAPProduct, oMaskModel, Mask.BandMathsType.INSTANCE);

					PropertyContainer oImageConfig = oMask.getImageConfig();
					oImageConfig.setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, oMaskModel.getExpression());
					oSNAPProduct.addMask(oMask);
					oRasterDataNode.getOverlayMaskGroup().add(oMask);
				}
			}

			// applying color manipulation

			ColorManipulationViewModel oColorManiputalionViewModel = oBandImageViewModel.getColorManiputalion();
			if (oColorManiputalionViewModel != null) {
				oBandImageManager.applyColorManipulation(oRasterDataNode, oColorManiputalionViewModel);
			}

			// creating the image
			BufferedImage oBufferedImg;
			try {
				oBufferedImg = oBandImageManager.buildImageWithMasks(oRasterDataNode, oImgSize, oRectangleViewPort, oColorManiputalionViewModel == null);
			} catch (Exception e) {
				Utils.debugLog("ProcessingResources.getBandImage: Exception: " + e.toString());
				Utils.debugLog("ProcessingResources.getBandImage: ExMessage: " + e.getMessage());
				e.printStackTrace();
				return Response.status(500).build();
			}

			if (oBufferedImg == null) {
				Utils.debugLog("ProcessingResource.getBandImage: img null");
				return Response.status(500).build();
			}

			Utils.debugLog("ProcessingResource.getBandImage: Generated image for band " + oBandImageViewModel.getBandName()
			+ " X= " + oBandImageViewModel.getVp_x() + " Y= " + oBandImageViewModel.getVp_y() + " W= "
			+ oBandImageViewModel.getVp_w() + " H= " + oBandImageViewModel.getVp_h());

			ByteArrayOutputStream oByteOutStream = new ByteArrayOutputStream();
			ImageIO.write(oBufferedImg, "jpg", oByteOutStream);
			byte[] ayImageData = oByteOutStream.toByteArray();

			return Response.ok(ayImageData).build();
		} catch (Exception oE) {
			Utils.debugLog("ProcessingResources.getBandImage( Session: " + sSessionId + ", WS: " + sWorkspace + ", ... ): " + oE);
		}
		return Response.serverError().build();
	}

	private Mask createMask(Product oSNAPProduct, MaskViewModel maskModel, Mask.ImageType type) {
		Utils.debugLog("ProcessingResource.createMask( Product, MaskViewModel, Mask.ImageType )");
		String maskName = UUID.randomUUID().toString();
		Dimension maskSize = new Dimension(oSNAPProduct.getSceneRasterWidth(), oSNAPProduct.getSceneRasterHeight());
		Mask mask = new Mask(maskName, maskSize.width, maskSize.height, type);
		mask.setImageColor(new Color(maskModel.getColorRed(), maskModel.getColorGreen(), maskModel.getColorBlue()));
		mask.setImageTransparency(maskModel.getTransparency());
		return mask;
	}

	@GET
	@Path("/WPSlist")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<WpsViewModel> getWpsList(@HeaderParam("x-session-token") String sSessionId) {
		try {
			Utils.debugLog("ProcessingResource.getWpsList( " + sSessionId + " )");
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				Utils.debugLog("ProcessingResource.getWpsList( " + sSessionId + " ): invalid session");
				return null;
			}
	
			WpsProvidersRepository oWPSrepo = new WpsProvidersRepository();
			ArrayList<WpsProvider> aoWPSProviders = oWPSrepo.getWpsList();
	
			if (null != aoWPSProviders) {
				ArrayList<WpsViewModel> aoResult = new ArrayList<WpsViewModel>();
				for (WpsProvider oWpsProvider : aoWPSProviders) {
					if (null != oWpsProvider) {
						WpsViewModel oWpsViewModel = new WpsViewModel();
						oWpsViewModel.setAddress(oWpsProvider.getAddress());
						aoResult.add(oWpsViewModel);
					}
				}
				return aoResult;
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessingResource.getWpsList( " + sSessionId + " ): " + oE);
		}
		return null;
	}

	private String acceptedUserAndSession(String sSessionId) {
		try {
			Utils.debugLog("ProcessingResource.acceptedUserAndSession( " + sSessionId + " )");
			// Check user
			if (Utils.isNullOrEmpty(sSessionId))
				return null;
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog("ProcessingResource.acceptedUserAndSession( " + sSessionId + " ): invalid session");
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId()))
				return null;
	
			return oUser.getUserId();
		} catch (Exception oE) {
			Utils.debugLog("ProcessingResource.acceptedUserAndSession( " + sSessionId + " ): " + oE);
		}
		return null;
	}

	/**
	 * Trigger the execution in the launcher of a SNAP Operation
	 * 
	 * @param sSessionId              User Session Id
	 * @param sSourceProductName      Source Product Name
	 * @param sDestinationProductName Target Product Name
	 * @param sWorkspaceId            Active Workspace
	 * @param oSetting                Generic Operation Setting
	 * @param oOperation              Launcher Operation Type
	 * @return
	 */
	private PrimitiveResult executeOperation(String sSessionId, String sSourceProductName, String sDestinationProductName, String sWorkspaceId, ISetting oSetting, LauncherOperations oOperation) {
		return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, oOperation, null);
	}

	/**
	 * Trigger the execution in the launcher of a SNAP Operation
	 * 
	 * @param sSessionId              User Session Id
	 * @param sSourceProductName      Source Product Name
	 * @param sDestinationProductName Target Product Name
	 * @param sWorkspaceId            Active Workspace
	 * @param oSetting                Generic Operation Setting
	 * @param oOperation              Launcher Operation Type
	 * @param sParentProcessWorkspaceId Id of the parent Process Workspace or null
	 * @return
	 */
	private PrimitiveResult executeOperation(String sSessionId, String sSourceProductName, String sDestinationProductName, String sWorkspaceId, ISetting oSetting, LauncherOperations oOperation, String sParentProcessWorkspaceId) {

		Utils.debugLog("ProsessingResources.executeOperation( Session: " + sSessionId + ", Source: " + sSourceProductName + ", Destination: "
				+ sDestinationProductName + ", WS: " + sWorkspaceId + ", Parent: " + sParentProcessWorkspaceId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		String sProcessObjId = "";

		// Check the user
		String sUserId = acceptedUserAndSession(sSessionId);

		// Is valid?
		if (Utils.isNullOrEmpty(sUserId)) {

			// Not authorized
			oResult.setIntValue(401);
			oResult.setBoolValue(false);

			return oResult;
		}

		try {
			// Update process list

			sProcessObjId = Utils.GetRandomName();

			// Create Operator instance
			OperatorParameter oParameter = getParameter(oOperation);
			
			if (oParameter == null) {
				Utils.debugLog("ProsessingResources.ExecuteOperation: impossible to create the parameter from the operation");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;				
			}

			// Set common settings
			oParameter.setSourceProductName(sSourceProductName);
			oParameter.setDestinationProductName(sDestinationProductName);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

			// Do we have settings?
			if (oSetting != null) oParameter.setSettings(oSetting);

			// Serialization Path
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");

			return Wasdi.runProcess(sUserId, sSessionId, oOperation.name(), sSourceProductName, sPath, oParameter, sParentProcessWorkspaceId);

		} catch (IOException e) {
			Utils.debugLog("ProsessingResources.ExecuteOperation: " + e);
			oResult.setBoolValue(false);
			oResult.setIntValue(500);
			return oResult;
		} catch (Exception e) {
			Utils.debugLog("ProsessingResources.ExecuteOperation: " + e);
			oResult.setBoolValue(false);
			oResult.setIntValue(500);
			return oResult;
		}
	}

	@POST
	@Path("run")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult runProcess(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sOperation") String sOperationId, @QueryParam("sProductName") String sProductName, @QueryParam("parent") String sParentProcessWorkspaceId, String sParameter) throws IOException {
		//@QueryParam("sOperation") String sOperationId, @QueryParam("sProductName") String sProductName, BaseParameter oParameter) throws IOException {


		// Log intro
		Utils.debugLog("ProsessingResources.runProcess( Session: " + sSessionId + ", Operation: " + sOperationId + ", Product: " + sProductName + ")");
		PrimitiveResult oResult = new PrimitiveResult();

		try {

			// Check the user
			String sUserId = acceptedUserAndSession(sSessionId);

			// Is valid?
			if (Utils.isNullOrEmpty(sUserId)) {

				// Not authorised
				oResult.setIntValue(401);
				oResult.setBoolValue(false);

				return oResult;
			}

			BaseParameter oParameter = BaseParameter.getParameterFromOperationType(sOperationId);

			if (oParameter == null) {
				// Error
				oResult.setIntValue(500);
				oResult.setBoolValue(false);

				return oResult;			
			}			

			oParameter = (BaseParameter) SerializationUtils.deserializeStringXMLToObject(sParameter);

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			return Wasdi.runProcess(sUserId, sSessionId, sOperationId, sProductName, sPath, oParameter, sParentProcessWorkspaceId);
		} 
		catch (Exception e) {
			e.printStackTrace();
			oResult.setStringValue(e.toString());
			oResult.setIntValue(500);
			oResult.setBoolValue(false);

			return oResult;					
		}

	}



	/**
	 * Get the paramter Object for a specific Launcher Operation
	 * 
	 * @param oOperation
	 * @return
	 */
	private OperatorParameter getParameter(LauncherOperations oOperation) {
		Utils.debugLog("ProcessingResources.OperatorParameter(  LauncherOperations )");
		switch (oOperation) {
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
		case MOSAIC:
			return new MosaicParameter();
		case SUBSET:
			return new SubsetParameter();
		case MULTISUBSET:
			return new MultiSubsetParameter();
		case REGRID:
			return new RegridParameter();
		default:
			return null;

		}
	}
}
