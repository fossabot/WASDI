package wasdi.shared.viewmodels;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class ProductViewModel {

    private String name;

    private String fileName;

    private String productFriendlyName;

    private MetadataViewModel metadata;

    private NodeGroupViewModel bandsGroups;
    
    private String metadataFileReference;

    
    public ProductViewModel() {
	}
    
    public ProductViewModel(ProductViewModel base) {
		setBandsGroups(base.getBandsGroups());
		setFileName(base.getFileName());
		setMetadata(base.getMetadata());
		setName(base.getName());
		setProductFriendlyName(base.getProductFriendlyName());
    }


    public MetadataViewModel getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataViewModel metadata) {
        this.metadata = metadata;
    }

    public NodeGroupViewModel getBandsGroups() {
        return bandsGroups;
    }

    public void setBandsGroups(NodeGroupViewModel bandsGroups) {
        this.bandsGroups = bandsGroups;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getProductFriendlyName() {
        return productFriendlyName;
    }

    public void setProductFriendlyName(String productFriendlyName) {
        this.productFriendlyName = productFriendlyName;
    }

	public String getMetadataFileReference() {
		return metadataFileReference;
	}

	public void setMetadataFileReference(String metadataFileReference) {
		this.metadataFileReference = metadataFileReference;
	}
}
