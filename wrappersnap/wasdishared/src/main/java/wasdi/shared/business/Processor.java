package wasdi.shared.business;

/**
 * Processor Entity
 * Represents a User Processor uploaded to WASDI 
 * @author p.campanella
 *
 */
public class Processor {
	
	/**
	 * Identifier of the processor
	 */
	private String processorId;
	/**
	 * User owner of the processor
	 */
	private String userId;
	/**
	 * Processor Name
	 */
	private String name;
	/**
	 * Processor Version
	 */
	private String version;
	/**
	 * Processor Description
	 */
	private String description;
	/**
	 * http port assigned to the processor
	 */
	private int port;
	/**
	 * Processor type
	 */
	private String type;
	/**
	 * Processor first deploy nodeCode
	 */
	private String nodeCode;	
	/**
	 * Processor first deploy nodeUrl
	 */
	private String nodeUrl;

	/**
	 * timeoutMs: 3 hours by default
	 */
	private long timeoutMs = 1000l*60l*60l*3l;
	
	/**
	 * Flag to know if it is public or not
	 */
	private int isPublic = 1;
		
	/**
	 * Sample JSON Parameter
	 */
	private String parameterSample="";
	
	public String getParameterSample() {
		return parameterSample;
	}
	public void setParameterSample(String parameterSample) {
		this.parameterSample = parameterSample;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getProcessorId() {
		return processorId;
	}
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public long getTimeoutMs() {
		return timeoutMs;
	}
	public void setTimeoutMs(long timeoutMs) {
		this.timeoutMs = timeoutMs;
	}	
	
	public int getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(int isPublic) {
		this.isPublic = isPublic;
	}

	public String getNodeCode() {
		return nodeCode;
	}
	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}
	public String getNodeUrl() {
		return nodeUrl;
	}
	public void setNodeUrl(String nodeUrl) {
		this.nodeUrl = nodeUrl;
	}
}
