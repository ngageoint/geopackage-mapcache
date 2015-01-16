package mil.nga.giat.geopackage.data.c1;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Spatial Reference System object
 * 
 * @author osbornb
 */
@DatabaseTable(tableName = "gpkg_spatial_ref_sys", daoClass = SpatialReferenceSystemDao.class)
public class SpatialReferenceSystem {

	/**
	 * srsName field name
	 */
	public static final String SRS_NAME = "srs_name";

	/**
	 * srsId field name
	 */
	public static final String SRS_ID = "srs_id";

	/**
	 * organization field name
	 */
	public static final String ORGANIZATION = "organization";

	/**
	 * organizationCoordsysId field name
	 */
	public static final String ORGANIZATION_COORDSYS_ID = "organization_coordsys_id";

	/**
	 * definition field name
	 */
	public static final String DEFINITION = "definition";

	/**
	 * description field name
	 */
	public static final String DESCRIPTION = "description";

	/**
	 * Human readable name of this SRS
	 */
	@DatabaseField(columnName = SRS_NAME, canBeNull = false)
	private String srsName;

	/**
	 * Unique identifier for each Spatial Reference System within a GeoPackage
	 */
	@DatabaseField(columnName = SRS_ID, id = true, canBeNull = false)
	private int srsId;

	/**
	 * Case-insensitive name of the defining organization e.g. EPSG or epsg
	 */
	@DatabaseField(columnName = ORGANIZATION, canBeNull = false)
	private String organization;

	/**
	 * Numeric ID of the Spatial Reference System assigned by the organization
	 */
	@DatabaseField(columnName = ORGANIZATION_COORDSYS_ID, canBeNull = false)
	private int organizationCoordsysId;

	/**
	 * Well-known Text [32] Representation of the Spatial Reference System
	 */
	@DatabaseField(columnName = DEFINITION, canBeNull = false)
	private String definition;

	/**
	 * Human readable description of this SRS
	 */
	@DatabaseField(columnName = DESCRIPTION)
	private String description;

	/**
	 * Default Constructor
	 */
	public SpatialReferenceSystem() {

	}

	public String getSrsName() {
		return srsName;
	}

	public void setSrsName(String srsName) {
		this.srsName = srsName;
	}

	public int getSrsId() {
		return srsId;
	}

	public void setSrsId(int srsId) {
		this.srsId = srsId;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public int getOrganizationCoordsysId() {
		return organizationCoordsysId;
	}

	public void setOrganizationCoordsysId(int organizationCoordsysId) {
		this.organizationCoordsysId = organizationCoordsysId;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
