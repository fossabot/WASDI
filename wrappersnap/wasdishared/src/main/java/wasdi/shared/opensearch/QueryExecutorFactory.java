/**
 * Created by Cristiano Nattero on 2019-02-06
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorFactory {

	private static final Map<String, Supplier<QueryExecutor>> s_aoExecutors;

	static {
		System.out.println("QueryExecutorFactory");
		final Map<String, Supplier<QueryExecutor>> aoMap = new HashMap<>();
		
		aoMap.put("ONDA", QueryExecutorONDA::new);
		aoMap.put("SENTINEL", QueryExecutorSENTINEL::new);
		aoMap.put("MATERA", QueryExecutorMATERA::new);
		aoMap.put("PROBAV", QueryExecutorPROBAV::new);
		aoMap.put("FEDEO", QueryExecutorFEDEO::new);
		
		s_aoExecutors = Collections.unmodifiableMap(aoMap);
		System.out.println("QueryExecutorFactory.static constructor, s_aoExecutors content:");
		for (String sKey : s_aoExecutors.keySet()) {
			System.out.println("QueryExecutorFactory.s_aoExecutors key: " + sKey);
		}
	}
	
	private QueryExecutor supply(String sProvider) {
		System.out.println("QueryExecutorFactory.QueryExecutor( "+sProvider+" )");
		QueryExecutor oExecutor = null;
		if(null!=sProvider) {
			Supplier<QueryExecutor> oSupplier = s_aoExecutors.get(sProvider);
			if(null!=oSupplier) {
				oExecutor = oSupplier.get();
			}
		} else {
			System.out.println("QueryExecutorFactory.QueryExecutor: sProvider is null");
		}
		return oExecutor;	
	}
	
	public QueryExecutor getExecutor(
			String sProvider,
			AuthenticationCredentials oCredentials,
			String sDownloadProtocol, String sGetMetadata) {
		System.out.println("QueryExecutorFactory.getExecutor( "+sProvider+" )...");
		QueryExecutor oExecutor = null;
		
		try {
			oExecutor = supply(sProvider);
			oExecutor.setCredentials(oCredentials);
			oExecutor.setMustCollectMetadata(Utils.doesThisStringMeansTrue(sGetMetadata));
			oExecutor.setDownloadProtocol(sDownloadProtocol);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return oExecutor;
	}

}
