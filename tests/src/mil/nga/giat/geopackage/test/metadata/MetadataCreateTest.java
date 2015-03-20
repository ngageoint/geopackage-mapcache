package mil.nga.giat.geopackage.test.metadata;

import java.sql.SQLException;

import mil.nga.giat.geopackage.test.CreateGeoPackageTestCase;
import mil.nga.giat.geopackage.test.TestSetupTeardown;

/**
 * Test Metadata from a created database
 * 
 * @author osbornb
 */
public class MetadataCreateTest extends CreateGeoPackageTestCase {

	/**
	 * Constructor
	 */
	public MetadataCreateTest() {

	}

	/**
	 * Test reading
	 * 
	 * @throws SQLException
	 */
	public void testRead() throws SQLException {

		MetadataUtils.testRead(geoPackage,
				TestSetupTeardown.CREATE_METADATA_COUNT);

	}

	/**
	 * Test updating
	 * 
	 * @throws SQLException
	 */
	public void testUpdate() throws SQLException {

		MetadataUtils.testUpdate(geoPackage);

	}

	/**
	 * Test creating
	 * 
	 * @throws SQLException
	 */
	public void testCreate() throws SQLException {

		MetadataUtils.testCreate(geoPackage);

	}

	/**
	 * Test deleting
	 * 
	 * @throws SQLException
	 */
	public void testDelete() throws SQLException {

		MetadataUtils.testDelete(geoPackage);

	}

	/**
	 * Test cascade deleting
	 * 
	 * @throws SQLException
	 */
	public void testDeleteCascade() throws SQLException {

		MetadataUtils.testDeleteCascade(geoPackage);

	}

}
