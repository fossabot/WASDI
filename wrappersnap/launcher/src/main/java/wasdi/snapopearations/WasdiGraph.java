package wasdi.snapopearations;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;

import sun.management.VMManagement;
import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.LoggerWrapper;
import wasdi.io.WasdiProductReader;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ProductViewModel;


/**
 * Wasdi SNAP Workflow Graph Wrapper and Exectur
 * @author p.campanella
 *
 */
public class WasdiGraph {
	
	/**
	 * First input file
	 */
	private File m_oInputFile;
	/**
	 * First output file
	 */
	private File m_oOutputFile;
	/**
	 * Snap Graph
	 */
	private Graph m_oGraph;
	/**
	 * Logger
	 */
	private LoggerWrapper m_oLogger = LauncherMain.s_oLogger;
	/**
	 * ProcessWorkspaceRepository 
	 */
	private ProcessWorkspaceRepository m_oProcessRepository;
	/**
	 * Process Workspace Entity
	 */
	private ProcessWorkspace m_oProcess;
	/**
	 * Rabbit reference
	 */
	private Send m_oRabbitSender;
	/**
	 * Workflow Parameters
	 */
	private GraphParameter m_oParams;
	/**
	 * List of input nodes
	 */
	private ArrayList<String> m_asInputNodes = new ArrayList<>();
	/**
	 * List of output nodes
	 */
	private ArrayList<String> m_asOutputNodes = new ArrayList<>();
	
	
	/**
	 * Construct the Graph object
	 * @param oParams
	 * @param oRabbitSender
	 * @throws Exception
	 */
	public WasdiGraph(GraphParameter oParams, Send oRabbitSender) throws Exception {
		//set the pgraph parameters
		this.m_oParams = oParams;
		
		//set the rabbit sender
		this.m_oRabbitSender = oRabbitSender;
		
		//build snap graph object
		m_oGraph = GraphIO.read(new StringReader(((GraphSetting)(oParams.getSettings())).getGraphXml()));
		
		//retrieve wasdi process
        m_oProcessRepository = new ProcessWorkspaceRepository();
        m_oProcess = m_oProcessRepository.getProcessByProcessObjId(oParams.getProcessObjId());
        
        m_oLogger.info("WasdiGraph: call find IO Nodes");
        findIONodes();
        
        if (m_oParams.getSettings() != null) {
        	GraphSetting oGraphSetting = (GraphSetting) m_oParams.getSettings();
        	if (oGraphSetting != null) {
        		if (oGraphSetting.getInputNodeNames() == null) {
        			 m_oLogger.info("WasdiGraph: input node names null: set m_asInputNodes");
        			oGraphSetting.setInputNodeNames(m_asInputNodes);
        		}
        		else if (oGraphSetting.getInputNodeNames().size() == 0) {
        			m_oLogger.info("WasdiGraph: input node names size 0: set m_asInputNodes");
        			oGraphSetting.setInputNodeNames(m_asInputNodes);
        		}
        		
        		if (oGraphSetting.getOutputNodeNames() == null) {
        			m_oLogger.info("WasdiGraph: output node names null: set m_asOutputNodes");
        			oGraphSetting.setOutputNodeNames(m_asOutputNodes);
        		}
        		else if (oGraphSetting.getOutputNodeNames().size() == 0) {
        			m_oLogger.info("WasdiGraph: output node names size 0: set m_asOutputNodes [size] " + m_asOutputNodes.size());
        			oGraphSetting.setOutputNodeNames(m_asOutputNodes);
        		}

        	}
        }
        
	}
	
	/**
	 * Find the Read and Write Nodes in this Workflow. 
	 * NOTE: The execute function uses the list received in the parameters and not these 
	 */
	public void findIONodes() {
		
		Node [] aoNodes = m_oGraph.getNodes();
		
		if (aoNodes == null) return;
		if (aoNodes.length == 0) return;
		
		for (int iNodes=0; iNodes<aoNodes.length;iNodes++) {
			Node oNode = aoNodes[iNodes];
			if (oNode.getOperatorName().equals("Read")) {
				m_oLogger.info("WasdiGraph.findIONodes: input node found " + oNode.getId());
				m_asInputNodes.add(oNode.getId());
			}
			else if (oNode.getOperatorName().equals("Write")) {
				m_oLogger.info("WasdiGraph.findIONodes: output node found " + oNode.getId());
				m_asOutputNodes.add(oNode.getId());
			}
		}
	}


	/**
	 * Run the graph
	 * NOTE: uses the list of I/O in the parameter object and not the self member took from the XML of the workflow
	 * @throws Exception
	 */
	public void execute() throws Exception {
		
		try {
			m_oLogger.info("WasdiGraph.execute: start");
			
	        //check input file
			//File oBaseDir = new File(ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
	        //File oUserDir = new File(oBaseDir, m_oParams.getUserId());
	        //File oWorkspaceDir = new File(oUserDir, m_oParams.getWorkspace());
	        
	        String sWorkspaceDir = LauncherMain.getWorspacePath(m_oParams);
	        File oWorkspaceDir = new File(sWorkspaceDir);
	        
	        ArrayList<File> aoOutputFiles = new ArrayList<>(); 

			GraphSetting oGraphSettings = (GraphSetting) m_oParams.getSettings();
			
			m_oLogger.info("WasdiGraph.execute: setting input files");
			
			for (int iNode = 0; iNode<oGraphSettings.getInputNodeNames().size(); iNode++) {
				
				String sInputNode = oGraphSettings.getInputNodeNames().get(iNode);
				
				//set the input and output files to the graph snap object
				Node oNodeReader = m_oGraph.getNode(sInputNode);
				
				if (oNodeReader==null ) {
					m_oLogger.error("WasdiGraph.execute: Reader node and Writer node are mandatory!!");
					throw new Exception("Reader node and Writer node are mandatory");
				}
				
		        File oInputFile = new File(oWorkspaceDir, oGraphSettings.getInputFileNames().get(iNode));
		        if (!oInputFile.canRead()) throw new Exception("WasdiGraph: cannot access input file: " + oInputFile.getAbsolutePath());
		        
		        if (m_oInputFile == null) {
		        	m_oInputFile= oInputFile;
		        }
				
		        setNodeValue(oNodeReader, "file", oInputFile.getAbsolutePath());
				
				m_oLogger.info("WasdiGraph.execute: input file set " + oInputFile.getAbsolutePath());
			}
			
			m_oLogger.info("WasdiGraph.execute: setting output files");
			
			for (int iNode = 0; iNode<oGraphSettings.getOutputNodeNames().size(); iNode++) {
				
				String sOutputNode = oGraphSettings.getOutputNodeNames().get(iNode);
				
				//set the input and output files to the graph snap object
				Node oNodeWriter = m_oGraph.getNode(sOutputNode);
				
				if (oNodeWriter==null ) {
					m_oLogger.error("WasdiGraph.execute: Reader node and Writer node are mandatory!!");
					throw new Exception("Reader node and Writer node are mandatory");
				}
				
				// Output file name: prepare default
				String sWorkflowAppend = "Workflow";
				
				if (!Utils.isNullOrEmpty(oGraphSettings.getWorkflowName())) {
					sWorkflowAppend = oGraphSettings.getWorkflowName();
				}
				
				String sOutputName = "Output_" + sWorkflowAppend + "_" + iNode;
				
				// First Try: corresponding input plus workflowname
				if (oGraphSettings.getInputFileNames() != null) {
					if (oGraphSettings.getInputFileNames().size()>iNode) {
						sOutputName = Utils.GetFileNameWithoutExtension(oGraphSettings.getInputFileNames().get(iNode));
						sOutputName = sOutputName + "_" + oGraphSettings.getWorkflowName();						
					}
				}
				
				// Second try (Best choice): did the user supplied an output?
				if (oGraphSettings.getOutputFileNames() != null) {
					if (oGraphSettings.getOutputFileNames().size()>iNode) {
						sOutputName = oGraphSettings.getOutputFileNames().get(iNode);
					}
				}
				
				m_oLogger.info("Output File ["+iNode+"]: " + sOutputName);
				
		        File oOutputFile = new File(oWorkspaceDir, sOutputName);
		        
		        if (m_oOutputFile == null) {
		        	m_oOutputFile = oOutputFile;
		        }
		        
		        aoOutputFiles.add(oOutputFile);
				
		        setNodeValue(oNodeWriter, "file", oOutputFile.getAbsolutePath());
		        				
				m_oLogger.info("WasdiGraph.execute: output file set " + oOutputFile.getAbsolutePath());
			}
			
			m_oLogger.info("WasdiGraph.execute: call SetFileSizeToProcess");
			
			// P.Campanella 16/06/2017: should add real file size to the Process Log
            //set file size     
            LauncherMain.setFileSizeToProcess(m_oInputFile, m_oProcess);
			
			//build the snap graph context and processor
			GraphContext oContext = new GraphContext(m_oGraph);		
			GraphProcessor oProcessor = new GraphProcessor();
			
			m_oLogger.info("WasdiGraph.execute: call init process");
			
			//update the wasdi process 
			initProcess();
			
			m_oLogger.info("WasdiGraph.execute: start graph");
			
			oProcessor.executeGraph(oContext, new WasdiProgreeMonitor(m_oProcessRepository, m_oProcess));
			
			Product[] aoOutputs = oContext.getOutputProducts();
			if (aoOutputs==null || aoOutputs.length==0) throw new Exception("No output created");
			
			//if (aoOutputs.length>1) m_oLogger.warn("More than 1 output created... keep only the first");
					
			for (int iOutputs = 0; iOutputs<aoOutputs.length; iOutputs++) {
				Product oProduct = aoOutputs[iOutputs];
				
				m_oLogger.debug("WasdiGraph.execute: managing output product ["+ iOutputs+"]: " + oProduct.getName());
				
	            // Get the original Bounding Box
	            DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
	            DownloadedFile oDownloadedFile = oDownloadedRepo.getDownloadedFileByPath(m_oInputFile.getAbsolutePath()) ;
	            
	            String sBbox = "";            
	            if (oDownloadedFile != null) {
	            	sBbox = oDownloadedFile.getBoundingBox();
	            }
	            
	            addProductToDb(oProduct, sBbox, aoOutputFiles.get(iOutputs));				
			}
			
		} finally {
            closeProcess();
		}
			
	}

	/**
	 * Initialize the WASDI process
	 * @throws Exception
	 */
	private void initProcess() throws Exception {
		if (m_oProcess != null) {
			//set source file size in the process
			long lInputFileSize = m_oInputFile.length();
		    double dInputFileSizeGiga = ( (double) lInputFileSize )/ (1024.0 * 1024.0 * 1024.0);
		    DecimalFormat oFormat = new DecimalFormat("#.00"); 	        
		    m_oLogger.debug("WasdiGraph.execute: File size [Gb] = " + oFormat.format(dInputFileSizeGiga));
		    m_oProcess.setFileSize(oFormat.format(dInputFileSizeGiga));
		    //set process pid, status and progress
			//m_oProcess.setPid(GetProcessId());
			m_oProcess.setStatus(ProcessStatus.RUNNING.name());
			m_oProcess.setProgressPerc(0);
			//update the process
		    if (!m_oProcessRepository.updateProcess(m_oProcess)) {
		    	m_oLogger.error("WasdiGraph.execute: Error during process update (pip + starting)");
		    } else {
		    	m_oLogger.debug("WasdiGraph.execute: Updated process  " + m_oProcess.getProcessObjId());
		    }
	        //send update process message
		    if (m_oRabbitSender!=null && !m_oRabbitSender.SendUpdateProcessMessage(m_oProcess)) {
				m_oLogger.error("WasdiGraph.execute: Error sending rabbitmq message to update process list");
			}
		}
		
	}

	
    /**
     * Close the Process on the mongo Db. Set progress to 100 and end date time 
     */
	private void closeProcess() {
		try{
			if (m_oProcess != null) {
		        //update the process
				m_oProcess.setProgressPerc(100);
				m_oProcess.setStatus(ProcessStatus.DONE.name());
				m_oProcess.setOperationEndDate(Utils.getFormatDate(new Date()));
		        if (!m_oProcessRepository.updateProcess(m_oProcess)) {
		        	m_oLogger.error("WasdiGraph: Error during process update (terminated)");
		        }
		        //send update process message
				if (m_oRabbitSender!=null && !m_oRabbitSender.SendUpdateProcessMessage(m_oProcess)) {
				    m_oLogger.debug("WasdiGraph: Error sending rabbitmq message to update process list");
				}
			}
		} catch (Exception e) {
		    m_oLogger.error("WasdiGraph: Exception closing process", e);
		}
	}

	/**
	 * Adds Output product to WASDI DB
	 * @param oProduct
	 * @param sBBox
	 * @throws Exception
	 */
    private void addProductToDb(Product oProduct, String sBBox, File oOutputFile) throws Exception {
    	
    	// Need to find the file of this output product
        
        // Get the partent folder
        File oFolder = new File(oOutputFile.getParent());
        // And the product Name
        String sName = oProduct.getName();
        
        File oProductFile = null;
        
        try {
        	// List the file in the folder
            Collection<File> aoFiles = FileUtils.listFiles(oFolder, null, false);

            // For each
            for (Iterator<File> oIterator = aoFiles.iterator(); oIterator.hasNext();) {
                File oFileInFolder = (File) oIterator.next();
                
                // Find the one starting with the exact name
                if (oFileInFolder.getName().startsWith(sName)) {
                	// Check that is not o folder
                	if (oFileInFolder.isFile()) {
                		// Should be the entry point
                		oProductFile = oFileInFolder;
                		break;
                	}
                }
                    
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // This is an error: here I should have found the file
        if (oProductFile == null) {
        	
        	m_oLogger.error("Product " + oOutputFile.getName() + " FILE NOT FOUND. TRY with .dim");
        	
        	// Try with the standard but probably will fail
        	oProductFile = new File(oOutputFile.getAbsolutePath()+".dim");
        }
        
        // Read the View Model
        WasdiProductReader oReadProduct = new WasdiProductReader();
		ProductViewModel oVM = oReadProduct.getProductViewModel(oProduct, oProductFile);
        
        // P.Campanella 12/05/2017: it looks it is done before. Let leave here a check
        DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
        DownloadedFile oCheck = oDownloadedRepo.getDownloadedFileByPath(oProductFile.getAbsolutePath());
        
        //boolean bAddProductToWS = true;
        
        if (oCheck == null) {
        	m_oLogger.debug("Insert in db");
        	
            // Save it in the register
            DownloadedFile oOutputProduct = new DownloadedFile();
            
            oOutputProduct.setFileName(oProductFile.getName());
            oOutputProduct.setFilePath(oProductFile.getAbsolutePath());
            oOutputProduct.setProductViewModel(oVM);
            oOutputProduct.setBoundingBox(sBBox);
            
    		// Write Metadata to file system
            try {
                // Get Metadata Path a Random File Name
                String sMetadataPath = ConfigReader.getPropValue("METADATA_PATH");
        		if (!sMetadataPath.endsWith("/")) sMetadataPath += "/";
        		String sMetadataFileName = Utils.GetRandomName();

    			SerializationUtils.serializeObjectToXML(sMetadataPath+sMetadataFileName, oReadProduct.getProductMetadataViewModel(oProductFile));
    			
    			oOutputProduct.getProductViewModel().setMetadataFileReference(sMetadataFileName);
    			
    		} catch (IOException e) {
    			e.printStackTrace();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}            
            
            if (!oDownloadedRepo.insertDownloadedFile(oOutputProduct)) {
            	m_oLogger.error("Impossible to Insert the new Product " + m_oOutputFile.getName() + " in the database.");            	
            }
            else {
            	m_oLogger.error("Product Inserted " + oOutputFile.getName());
            }
        }
        
        //if (bAddProductToWS) {
        addProductToWorkspace(oProductFile.getAbsolutePath(),sBBox);
        //}
        //else {
        //	m_oLogger.error("Product NOT added to the Workspace");
        //}
        
        m_oLogger.debug("OK DONE");
        
        //P.Campanella 12/05/2017: Metadata are saved in the DB but sent back to the client with a dedicated API. So here metadata are nulled
        //oVM.setMetadata(null);

        if (m_oRabbitSender!=null) m_oRabbitSender.SendRabbitMessage(true, LauncherOperations.GRAPH.name(), m_oParams.getWorkspace(), oVM, m_oParams.getExchange());
    }
	
	
    /**
     * Add product to the workspace
     * @throws Exception
     */
    private void addProductToWorkspace(String sProductName, String sBbox) throws Exception {
    	
		// Create Repository
		ProductWorkspaceRepository oProductRepository = new ProductWorkspaceRepository();
		
		// Check if product is already in the Workspace
		if (!oProductRepository.existsProductWorkspace(sProductName, m_oParams.getWorkspace())) {
			
    		// Create the entity
    		ProductWorkspace oProductWorkspaceEntity = new ProductWorkspace();
    		oProductWorkspaceEntity.setProductName(sProductName);
    		oProductWorkspaceEntity.setWorkspaceId(m_oParams.getWorkspace());
    		oProductWorkspaceEntity.setBbox(sBbox);
    		
    		// Try to insert
    		if (!oProductRepository.insertProductWorkspace(oProductWorkspaceEntity)) {        			
    			m_oLogger.debug("WasdiGraph.addProductToWorkspace:  Error adding " + sProductName + " in WS " + m_oParams.getWorkspace());
    			throw new Exception("unable to insert product in workspace");
    		}
    		
    		m_oLogger.debug("WasdiGraph.addProductToWorkspace:  Inserted " + sProductName + " in WS " + m_oParams.getWorkspace());
		}
		else {
			m_oLogger.debug("WasdiGraph.addProductToWorkspace: Product " + sProductName + " Already exists in WS " + m_oParams.getWorkspace());
		}
    }

	/**
	 * Get Process Id
	 * @return
	 * @throws Exception
	 */
	private int GetProcessId() throws Exception {
        RuntimeMXBean oRuntimeMXBean = ManagementFactory.getRuntimeMXBean();
        Field oJvmField = oRuntimeMXBean.getClass().getDeclaredField("jvm");
        oJvmField.setAccessible(true);
        VMManagement oVmManagement = (VMManagement) oJvmField.get(oRuntimeMXBean);
        Method oGetProcessIdMethod = oVmManagement.getClass().getDeclaredMethod("getProcessId");
        oGetProcessIdMethod.setAccessible(true);
        return (int)oGetProcessIdMethod.invoke(oVmManagement);
    }
	
	/**
	 * Set the value of a node of the XML chart
	 * @param oNode
	 * @param sChildName
	 * @param sValue
	 * @return
	 */
	public static void setNodeValue(Node oNode, String sChildName, String sValue) {
		DomElement oDomElement = oNode.getConfiguration();
		DomElement[] aoChildren = oDomElement.getChildren(sChildName);
		
		if (aoChildren==null || aoChildren.length!=1) {	
			XppDomElement oFileElement = new XppDomElement("file");
			oFileElement.setValue(sValue);
			oDomElement.addChild(oFileElement);
		}
		else {
			aoChildren[0].setValue(sValue);
		}
	}


	/**
	 * Get list of input nodes
	 * @return
	 */
	public ArrayList<String> getInputNodes() {
		return m_asInputNodes;
	}

	/**
	 * Set list of input nodes
	 * @param asInputNodes
	 */
	public void setInputNodes(ArrayList<String> asInputNodes) {
		this.m_asInputNodes = asInputNodes;
	}

	/**
	 * Get list of Output nodes
	 * @return
	 */
	public ArrayList<String> getOutputNodes() {
		return m_asOutputNodes;
	}

	/**
	 * Set List of Output nodes
	 * @param asOutputNodes
	 */
	public void setOutputNodes(ArrayList<String> asOutputNodes) {
		this.m_asOutputNodes = asOutputNodes;
	}

	/**
	 * Get Parameters
	 * @return
	 */
	public GraphParameter getParams() {
		return m_oParams;
	}

	/**
	 * Set Parameters
	 * @param oParams
	 */
	public void setParams(GraphParameter oParams) {
		this.m_oParams = oParams;
	}

	
}
