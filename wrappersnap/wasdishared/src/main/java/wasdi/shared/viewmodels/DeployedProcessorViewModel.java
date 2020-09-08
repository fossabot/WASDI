package wasdi.shared.viewmodels;

public class DeployedProcessorViewModel {
	private String processorId;
	private String processorName;
	private String processorVersion;
	private String processorDescription;
	private String imgLink;
	private String publisher;
	private String paramsSample = "";
	private int isPublic = 0;
	private long timeoutMs = 1000l*60l*60l*3l;
	private String type = "";
	private Boolean sharedWithMe = false;
	
	public String getParamsSample() {
		return paramsSample;
	}
	public void setParamsSample(String paramsSample) {
		this.paramsSample = paramsSample;
	}
	public String getProcessorId() {
		return processorId;
	}
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	public String getProcessorName() {
		return processorName;
	}
	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}
	public String getProcessorVersion() {
		return processorVersion;
	}
	public String getImgLink() {
		return imgLink;
	}
	public void setImgLink(String imgLink) {
		this.imgLink = imgLink;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public void setProcessorVersion(String processorVersion) {
		this.processorVersion = processorVersion;
	}
	public String getProcessorDescription() {
		return processorDescription;
	}
	public void setProcessorDescription(String processorDescription) {
		this.processorDescription = processorDescription;
	}
	public int getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(int isPublic) {
		this.isPublic = isPublic;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public long getTimeoutMs() {
		return timeoutMs;
	}
	public void setTimeoutMs(long lTimeoutMs) {
		this.timeoutMs = lTimeoutMs;
	}
	public Boolean getSharedWithMe() {
		return sharedWithMe;
	}
	public void setSharedWithMe(Boolean sharedWithMe) {
		this.sharedWithMe = sharedWithMe;
	}
	
}
