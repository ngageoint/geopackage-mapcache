package mil.nga.mapcache.data;

import java.io.Serializable;
import java.util.List;

import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.sf.GeometryType;

/**
 * GeoPackage Feature table information
 *
 * @author osbornb
 */
public class GeoPackageFeatureTable extends GeoPackageTable implements Serializable {

    /**
     * UID
     */
    private static final long serialVersionUID = 1;

    /**
     * Geometry Type
     */
    public GeometryType geometryType;

    /**
     * Feature columns
     */
    public List<FeatureColumn> featureColumns;

    /**
     * Create a new feature table
     *
     * @param database
     * @param name
     * @param geometryType
     * @param count
     * @return
     */
    public GeoPackageFeatureTable(String database, String name,
                                  GeometryType geometryType, int count) {
        super(database, name, count);
        this.geometryType = geometryType;
    }

    @Override
    public GeoPackageTableType getType() {
        return GeoPackageTableType.FEATURE;
    }

    public GeometryType getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryType geometryType) {
        this.geometryType = geometryType;
    }

    public List<FeatureColumn> getFeatureColumns() {
        return featureColumns;
    }

    public void setFeatureColumns(List<FeatureColumn> featureColumns) {
        this.featureColumns = featureColumns;
    }
}
