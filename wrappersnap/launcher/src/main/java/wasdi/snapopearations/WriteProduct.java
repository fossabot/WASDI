package wasdi.snapopearations;

import java.io.File;

import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.internal.OperatorExecutor;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;

/**
 * SNAP Product Write Utility
 * Created by s.adamo on 24/05/2016.
 */
public class WriteProduct  {
	
	private ProcessWorkspaceRepository m_oProcessWorkspaceRepository = null;
	private ProcessWorkspace m_oProcessWorkspace = null;

	public WriteProduct() {
	}
	
	public WriteProduct(ProcessWorkspaceRepository oProcessWorkspaceRepository, ProcessWorkspace oProcessWorkspace) {
		super();
		this.m_oProcessWorkspaceRepository = oProcessWorkspaceRepository;
		this.m_oProcessWorkspace = oProcessWorkspace;
	}

	public String writeBEAMDIMAP(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        String sFormat = DimapProductWriterPlugIn.DIMAP_FORMAT_NAME;

        return doWriteProduct(oProduct, sFilePath, sFileName, sFormat, ".dim");
    }

    public String writeGeoTiff(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        return doWriteProduct(oProduct, sFilePath, sFileName, "GeoTIFF", ".tif");
    }

    private String doWriteProduct(Product oProduct, String sFilePath, String sFileName, String sFormat, String sExtension)
    {
        try {
            if (!sFilePath.endsWith("/")) sFilePath += "/";
            File newFile = new File(sFilePath + sFileName + sExtension);
            LauncherMain.s_oLogger.debug("WriteProduct: Output File: " + newFile.getAbsolutePath());
            
            WriteOp writeOp = new WriteOp(oProduct, newFile, sFormat);
            writeOp.setDeleteOutputOnFailure(true);
            writeOp.setWriteEntireTileRows(true);
            writeOp.setClearCacheAfterRowWrite(false);
            writeOp.setIncremental(true);
            final OperatorExecutor executor = OperatorExecutor.create(writeOp);
            executor.execute(new WasdiProgreeMonitor(m_oProcessWorkspaceRepository, m_oProcessWorkspace));        

            return newFile.getAbsolutePath();
        }
        catch (Exception oEx)
        {
        	oEx.printStackTrace();
            LauncherMain.s_oLogger.error("WriteProduct: Error writing product. " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }

        return null;
    }



}
