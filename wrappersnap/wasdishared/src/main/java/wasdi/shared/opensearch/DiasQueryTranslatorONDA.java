/**
 * Created by Cristiano Nattero on 2018-12-04
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class DiasQueryTranslatorONDA extends DiasQueryTranslator {
	
	//TODO make a JSON file with query configuration
	//TODO write the path of the JSON file in the configuration file (web.xml, filename read and passed from the server?)
	
	public DiasQueryTranslatorONDA() {
		/*
		keyMapping.put("platformname", "name");
		valueMapping.put("Sentinel-1", "S1*");
		valueMapping.put("Sentinel-2", "S2*");
		valueMapping.put("Sentinel-3", "S3*");
		
		keyMapping.put("filename", "name");
		keyMapping.put("producttype", "name");
		
		valueMapping.put("SLC", "*SLC*");
		valueMapping.put("GRD", "*GRD*");
		valueMapping.put("GRD", "*GRD*");
		valueMapping.put("S2MSI1C", "*S2MSI1C*");
		
		keyMapping.put("timeliness", "timeliness");
		valueMapping.put("Near Real Time", "NRT");
		valueMapping.put("Short Time Critical", "STC");
		valueMapping.put("Non Time Critical", "NTC");
		
		//these can have only integer values
		keyMapping.put("relativeorbitstart", "relativeOrbitNumber");
		keyMapping.put("relativeorbitnumber", "relativeOrbitNumber");
		
		keyMapping.put("sensoroperationalmode", "sensorOperationalMode");
		keyMapping.put("cloudcoverpercentage", "cloudCoverPercentage");
		*/
		
	}
	
	protected String cleanerTranslate(String sInput) {
		if(Utils.isNullOrEmpty(sInput)) {
			return new String("");
		}
			
		String sQuery = prepareQuery(sInput);
		
		String sResult = new String("");
		String sTmp = "";
		
		sTmp += parseSentinel1(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			sResult += sTmp;
		}
		
		sTmp = parseSentinel2(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult += " AND ";
			}
			sResult += sTmp;
		}
		
		sTmp = parseSentinel3(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult += " AND ";
			}
			sResult += sTmp;
		}
		
		//Proba-V
		if(sQuery.contains("Proba-V")) {
			//ignore this case
			System.out.println("DiasQueryTranslatorONDA.CleanerTranslate: ignoring Proba-V as not supported by ONDA");
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult += " AND ";
			}
			sResult += "( platformName:ErrorProba-VNotSupported )";
		}
		
		sTmp = parseEnvisat(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult += " AND ";
			}
			sResult += sTmp;
		}
		
	
		sTmp = parseLandsat(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult += " AND ";
			}
			sResult += sTmp;
		}
		
		
		sTmp = parseTimeFrame(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult = sResult + " AND ( ( " + sTmp + " ) )";
			} else {
				sResult += sTmp;
			}
		}
		
		sTmp = parseFootPrint(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult += " AND ";
			}
			sResult += sTmp;
		}
		return sResult;
	}

	protected String parseLandsat(String sQuery) {
		String sResult = "";
		if(sQuery.contains("Landsat")) {
			sResult += "( ( platformName:Landsat-* AND name:*";
			int iStart = sQuery.indexOf("Landsat");
			if(sQuery.substring(iStart).contains("L1T")) {
				sResult+="L1T*";
			} else if(sQuery.substring(iStart).contains("L1G")) {
				sResult+="L1G*";
			} else if(sQuery.substring(iStart).contains("L1GT")) {
				sResult+="L1GT*";
			} else if(sQuery.substring(iStart).contains("L1GS")) {
				sResult+="L1GS*";
			} else if(sQuery.substring(iStart).contains("L1TP")) {
				sResult+="L1TP*";
			}
			sResult += " )";
			
			if(sQuery.substring(iStart).contains("cloudCoverPercentage")) {
				//cloudCoverPercentage:[16.33 TO 95.6]
				iStart = sQuery.indexOf("cloudCoverPercentage:");
				iStart = sQuery.indexOf("[", iStart);
				int iStop = sQuery.indexOf("]", iStart);
				sResult += " AND cloudCoverPercentage:";
				sResult+=sQuery.substring(iStart, iStop+1);
				
			}
			sResult += ")";
		}
		return sResult;
	}

	protected String parseEnvisat(String sQuery) {
		String sResult = "";
		if(sQuery.contains("Envisat")) {
			sResult +="( ( platformName:Envisat AND name:*";
			int iStart = sQuery.indexOf("Envisat");
			if(sQuery.substring(iStart).contains("ASA_IM__0P")) {
				sResult+="ASA_IM__0P*";
			} else if(sQuery.substring(iStart).contains("ASA_WS__0P")) {
				sResult+="ASA_WS__0P*";
			}
			
			if(sQuery.substring(iStart).contains("orbitDirection:")) {
				sResult+=" AND orbitDirection:";
				iStart = sQuery.indexOf("orbitDirection:");
				if(sQuery.substring(iStart).contains("ASCENDING")) {
					sResult += "ASCENDING";
				} else {
					sResult += "DESCENDING";
				}
			}
			
			sResult +=" ) )";
		}
		return sResult;
	}

	protected String parseSentinel3(String sQuery) {
		String sResult = "";
		if(sQuery.contains("Sentinel-3")) {
			sResult += "( ( name:S3* AND name:*";
			
			int iStart = sQuery.indexOf("Sentinel-3");
			if(sQuery.substring(iStart).contains("SR_1_SRA___")) {
				sResult += "SR_1_SRA___*";
			} else if(sQuery.substring(iStart).contains("SR_1_SRA_A_")) {
				sResult += "SR_1_SRA_A_*";
			} else if(sQuery.substring(iStart).contains("SR_1_SRA_BS")) {
				sResult += "SR_1_SRA_BS*";
			} else if(sQuery.substring(iStart).contains("SR_2_LAN___")) {
				sResult += "SR_2_LAN___*";
			}
			
			if(sQuery.substring(iStart).contains("timeliness")) {
				sResult += " AND timeliness:";
				iStart = sQuery.substring(iStart).indexOf("timeliness");
				if(sQuery.substring(iStart).contains("Near Real Time") ){
					sResult += "NRT";
				} else if(sQuery.substring(iStart).contains("Short Time Critical")) {
					sResult += "STC";
				} else if(sQuery.substring(iStart).contains("Non Time Critical") ) {
					sResult += "NTC";
				}
			}
			sResult += " ) )";
		}
		return sResult;
	}


	protected String parseSentinel2(String sQuery) {
		String sSentinel2 = "";
		if(sQuery.contains("platformname:Sentinel-2")) {
			sSentinel2 = "( ( name:S2* AND ";
			if(sQuery.contains("filename:S2A_*")){
				sSentinel2+="name:S2A_* AND ";
			} else if(sQuery.contains("filename:S2B_*")){
				sSentinel2+="name:S2B_* AND ";
			} else {
				sSentinel2+="name:* AND ";
			}

			if(sQuery.contains("producttype:S2MSI1C")) {
				sSentinel2+="name:*MSI1C*";
			} else if(sQuery.contains("producttype:S2MSI2Ap")) {
				sSentinel2+="name:*MSIL2Ap*";
			} else {
				sSentinel2+="name:*";
			}
			sSentinel2+=" )";

			//cloudCoverPercentage make sure to read the query for s2 and not for landsat
			int iFrom = sQuery.indexOf("platformname:Sentinel-2");
			int iTo = sQuery.length();
			int iLand = sQuery.indexOf("platformname:Landsat");
			if(iLand > 0 ) {
				iTo = Math.min(iTo, iLand);
			}
			String sSearchSubString = sQuery.substring(iFrom, iTo);
			String sCloud = "cloudcoverpercentage";
			if(sSearchSubString.contains(sCloud)) {
				iFrom = sSearchSubString.indexOf(sCloud) + sCloud.length()+2;
				iTo = sSearchSubString.indexOf("]");
				sSearchSubString = sSearchSubString.substring(iFrom, iTo);
				String[] sInterval = sSearchSubString.split(" TO ");
				String sLow = sInterval[0];
				String sHi = sInterval[1];
				sSentinel2 +=" AND cloudCoverPercentage:[ " + sLow + " TO " + sHi + " ]";

			}
			sSentinel2 += " )";
		}
		
		return sSentinel2;
	}

	protected String parseSentinel1(String sQuery) {
		String sSentinel1 = "";
		if(sQuery.contains("platformname:Sentinel-1")) {
			sSentinel1 = "( ( name:S1* AND ";
			
			//filename:[S1A_*|S1B_*] (optional)
			if(sQuery.matches(".*filename\\s*\\:\\s*S1A.*")){
				sSentinel1 += "name:S1A_*";
			} else if(sQuery.matches(".*filename\\s*\\:\\s*S1B.*")) {
				sSentinel1 += "name:S1B_*";
			} else {
				sSentinel1 += "name:*";
			}
			sSentinel1+=" AND ";
			//producttype:[SLC|GRD|OCN] (optional)
			if(sQuery.matches(".*producttype\\s*\\:\\s*SLC.*")) {
				sSentinel1+= "name:*SLC*";
			} else if( sQuery.matches(".*producttype\\s*\\:\\s*GRD.*") ) {
				sSentinel1+="name:*GRD*";
			} else if( sQuery.matches(".*producttype\\s*\\:\\s*OCN.*") ) {
				sSentinel1+="name:*OCN*";
			} else {
				sSentinel1+="name:*";
			}
			sSentinel1+=" AND ";
			//the next token w/ wildcard is always added by ONDA
			sSentinel1+="name:*";
			
			//relativeorbitnumber/relativeOrbitNumber:[integer in [1-175]] (optional)
			if(sQuery.contains("relativeorbitnumber")) {
				sSentinel1+=" AND relativeOrbitNumber:";
				String sPattern = "(?<= relativeorbitnumber\\:)(\\d*.\\d*)(?= )";
				Pattern oPattern = Pattern.compile(sPattern);
				Matcher oMatcher = oPattern.matcher(sQuery);
				String sIntRelativeOrbit = "";
				if(oMatcher.find()) {
					//its a number with decimal digits
					sIntRelativeOrbit = oMatcher.group(1);
					sIntRelativeOrbit = sIntRelativeOrbit.split("\\.")[0];
					sSentinel1+= sIntRelativeOrbit;
				} else {
					//it's an integer
					sPattern = "(?<= relativeorbitnumber\\:)(\\d*)(?= )";
					oPattern = Pattern.compile(sPattern);
					oMatcher = oPattern.matcher(sQuery);
					if(oMatcher.find()) {
						sIntRelativeOrbit = oMatcher.group(1);
						sSentinel1+= sIntRelativeOrbit;
					} //don't add the query otherwise
				} 
			}
			//Sensor Mode:[SM|IW|EW|WV] (optional)
			if(sQuery.matches(".*sensoroperationalmode\\s*\\:\\s*SM.*")) {
				sSentinel1+=" AND sensorOperationalMode:SM";
			} else if(sQuery.matches(".*sensoroperationalmode\\s*\\:\\s*IW.*")) {
				sSentinel1+=" AND sensorOperationalMode:IW";
			} else if(sQuery.matches(".*sensoroperationalmode\\s*\\:\\s*EW.*")) {
				sSentinel1+=" AND sensorOperationalMode:EW";
			} else if(sQuery.matches(".*sensoroperationalmode\\s*\\:\\s*WV.*")) {
				sSentinel1+=" AND sensorOperationalMode:WV";
			}
			sSentinel1 +=" ) )";
		}
		
		return sSentinel1;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 * 
	 * translates from WASDI query (OpenSearch) to OpenData format used by ONDA DIAS
	 * 
	 * https://github.com/fadeoutsoftware/WASDI/issues/18
	 * 
	 */
	@Override
	public String translate(String sQuery) {
		String sResult = sQuery;
		//Safe to assume:
			//no leading/ending whitespaces
			//no double whitespaces
		sResult = sResult.trim().replaceAll(" +", " "); 
		
		
		//What is supported by ONDA?
		//checked on 2018.12.06
		//https://catalogue.onda-dias.eu/catalogue/ supports:
		//(by mission)
		
		
		
		//Sentinel-2
			//YES
				//Platform:[S2A_*|S2B-*] (optional)
				//Type:[MSI1C|MSI2A|MSI2Ap] (optional)
				//Cloud Cover %:interval [a,b] between float [0,100] (optional)
		//Sentinel-3
			//YES
				//Type:[SR_1_SRA__|SR_1_SRA_A_|SR_1_SRA_BS|SR_2_LAN__] (optional)
				//Timeliness:[Near Real time|Short Time Critical|Non Time Critical]
			//NO
				//instrument
				//product level
				//CRelative Orbit Start
		//ENVISAT
			//YES
				//type: [ASA_IM__0P|ASA_WS__0P] (optional)
				//Orbit Position:[ASCENDING|DESCENDING]
			//NO
		//LANDSAT
			//YES
				//Type:[L1T|L1G|L1GT|L1GS|L1TP] (optional)
				//Cloud Cover %:interval [a,b] between float [0,100] (optional)
			//NO
		//PROBAV
			//not supported
		
		//begin translation
		
		//polygon
		sResult = sResult.replaceAll("intersects", "Intersects");
		
		
		//SENTINEL 1 2 3 platform
		sResult = sResult.replaceAll("platformname:Sentinel-1", "name:S1*");
		sResult = sResult.replaceAll("platformname:Sentinel-2", "name:S2*");
		sResult = sResult.replaceAll("platformname:Sentinel-3", "name:S3*");
		
		//Envisat
		sResult = sResult.replaceAll("platformname:Envisat", "platformName:Envisat");
		
		//Landsat
		sResult = sResult.replaceAll("platformname:Landsat-*","platformName:Landsat-*");

		//SENTINEL 1 2 3 filename
		sResult = sResult.replaceAll("filename:", "name:");
		
		//specific product types that need asterisks
		sResult = sResult.replaceAll("producttype:SLC","name:\\*SLC\\*");
		sResult = sResult.replaceAll("producttype:GRD","name:\\*GRD\\*");
		sResult = sResult.replaceAll("producttype:OCN","name:\\*OCN\\*");
		sResult = sResult.replaceAll("producttype:S2MSI1C", "name:\\*S2MSI1C\\*");
		sResult = sResult.replaceAll("producttype:", "name:");
		
		sResult = sResult.replaceAll("name:ASA_IM__0P","name:*ASA_IM__0P*");
		sResult = sResult.replaceAll("name:ASA_WS__0P","name:*ASA_WS__0P*");
		
		sResult = sResult.replaceAll("name:L1T","name:*L1T*");
		sResult = sResult.replaceAll("name:L1G","name:*L1G*");
		sResult = sResult.replaceAll("name:L1GT","name:*L1GT*");
		sResult = sResult.replaceAll("name:L1GS","name:*L1GS*");
		sResult = sResult.replaceAll("name:L1TP","name:*L1TP*");
				
		//polarisationmode:HH not supported by ONDA? 
		//sensoroperationalmode:SM same name in ONDA 
		//swathidentifier:b not supported by ONDA?
		//cloudcoverpercentage:a same name in ONDA
		sResult = sResult.replaceAll("timeliness:Near Real Time", "timeliness:NRT");
		sResult = sResult.replaceAll("timeliness:Short Time Critical", "timeliness:STC");
		sResult = sResult.replaceAll("timeliness:Non Time Critical", "timeliness:NTC");
		//OS: just for sentinel1, ONDA: just for sentinel3
		sResult = sResult.replaceAll("relativeorbitstart:", "relativeOrbitNumber:");
		sResult = sResult.replaceAll("relativeorbitnumber:", "relativeOrbitNumber:");
		//if there's a dot "." remove it and the decimals that follow
		String sPattern = "(?<=relativeOrbitNumber:\\d{1,50})"+
				"\\.\\d*"+
				"(?=\\s{0,1}|\\)|[a-z]|[A-Z])";
		sResult = sResult.replaceAll(sPattern, "");

		
		sResult = sResult.replaceAll("sensoroperationalmode:", "sensorOperationalMode:");
		sResult = sResult.replaceAll("cloudcoverpercentage", "cloudCoverPercentage");
		
		//cloudCoverPercentage should be the same
		
		//return sResult;
		return cleanerTranslate(sQuery);
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#encode(java.lang.String)
	 */
	@Override
	public String encode(String sDecoded) {
		String sResult = new String(sDecoded); 
		sResult = sResult.replaceAll(" ", "%20");
		sResult = sResult.replaceAll("\"", "%22");
		//sResult = java.net.URLEncoder.encode(sDecoded, m_sEnconding);
		return sResult;
	}

	String getNextDateTime(String sSubQuery) {
		String sDateTime = "";
		String sDateTimePattern = "(\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d\\:\\d\\d:\\d\\d\\.\\d\\d\\dZ)"; 
		Pattern oDateTimePattern = Pattern.compile(sDateTimePattern);
		Matcher oMatcher = oDateTimePattern.matcher(sSubQuery);
		if(oMatcher.find()) {
			sDateTime = oMatcher.group(1);
		}
		return sDateTime;
	}
	
	//time frame
	//WASDI OpenSearch format
	//( beginPosition:[2018-12-10T00:00:00.000Z TO 2018-12-17T23:59:59.999Z] AND endPosition:[2018-12-10T00:00:00.000Z TO 2018-12-17T23:59:59.999Z] )
	//ONDA format
	//( ( beginPosition:[2018-01-01T00:00:00.000Z TO 2018-12-01T23:59:59.999Z] AND endPosition:[2018-01-01T00:00:00.000Z TO 2018-12-01T23:59:59.999Z] ) )
	protected String parseTimeFrame(String sQuery) {
		String sResult = "";
		int iStart = 0;
		if(sQuery.contains("beginPosition") ) {
			sResult += "beginPosition:[";
			//start
			iStart = sQuery.indexOf("beginPosition");
			String sDateTime = getNextDateTime(sQuery.substring(iStart));	
			sResult += sDateTime;
			sResult += " TO ";
			//end
			iStart = sQuery.indexOf(" TO ", iStart);
			sDateTime = getNextDateTime(sQuery.substring(iStart));	
			sResult += sDateTime;
			sResult += "]";
		}	
		if( sQuery.contains("endPosition")) {
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult += " AND ";
			}
			//start
			sResult += "endPosition:[";
			iStart = sQuery.indexOf("endPosition", iStart);
			String sDateTime = getNextDateTime(sQuery.substring(iStart));	
			sResult += sDateTime;
			sResult += " TO ";
			//end
			iStart = sQuery.indexOf(" TO ", iStart);
			sDateTime = getNextDateTime(sQuery.substring(iStart));	
			sResult += sDateTime;
			sResult += "]";
		}
		return sResult;
	}

	//footprint
	//WASDI OpenSearch format
	//footprint:"intersects(POLYGON((-13.535156250000002 18.97902595325528,-13.535156250000002 60.23981116999893,62.92968750000001 60.23981116999893,62.92968750000001 18.97902595325528,-13.535156250000002 18.97902595325528)))"
	//ONDA format
	//footprint:"Intersects(POLYGON((-13.535156250000002 18.97902595325528,-13.535156250000002 60.23981116999893,62.92968750000001 60.23981116999893,62.92968750000001 18.97902595325528,-13.535156250000002 18.97902595325528)))"
	protected String  parseFootPrint(String sQuery) {
		String sFootprint = "";
		if(sQuery.contains("footprint")) {
			sFootprint += "footprint:\"Intersects(POLYGON(("; //no leading spaces
			//parse polygon
			String sPolygonLabel = "POLYGON (";
			int iStart = sQuery.indexOf(sPolygonLabel) + sPolygonLabel.length() + 1;
			Character cCurrent = sQuery.charAt(iStart);
			while(cCurrent.equals('(') || cCurrent.equals(' ')) {
				iStart++;
				cCurrent = sQuery.charAt(iStart);
			}
			int iEnd = sQuery.indexOf(" )", iStart);
			String sPolygonCoordinates = sQuery.substring(iStart, iEnd);
			sFootprint += sPolygonCoordinates;
			sFootprint+=")))\""; //no trailing spaces
		}
		return sFootprint;
	}
	
	protected String prepareQuery(String sInput) {
		String sQuery = new String(sInput);
		//insert space before and after round brackets
		sQuery = sQuery.replaceAll("\\(", " \\( ");
		sQuery = sQuery.replaceAll("\\)", " \\) ");
		//remove space before and after square brackets 
		sQuery = sQuery.replaceAll(" \\[", "\\[");
		sQuery = sQuery.replaceAll("\\[ ", "\\[");
		sQuery = sQuery.replaceAll(" \\]", "\\]");
		sQuery = sQuery.replaceAll("\\] ", "\\]");
		sQuery = sQuery.replaceAll("POLYGON", "POLYGON ");
		sQuery = sQuery.replaceAll("\\: ", "\\:");
		sQuery = sQuery.replaceAll(" \\: ", "\\:");
		
		sQuery = sQuery.replaceAll("AND", " AND ");
		sQuery = sQuery.trim().replaceAll(" +", " ");
		return sQuery;
	}


}
