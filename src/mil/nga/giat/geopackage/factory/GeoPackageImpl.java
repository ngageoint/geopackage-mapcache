package mil.nga.giat.geopackage.factory;

import java.sql.SQLException;
import java.util.List;

import mil.nga.giat.geopackage.GeoPackage;
import mil.nga.giat.geopackage.GeoPackageException;
import mil.nga.giat.geopackage.core.contents.Contents;
import mil.nga.giat.geopackage.core.contents.ContentsDao;
import mil.nga.giat.geopackage.core.srs.SpatialReferenceSystem;
import mil.nga.giat.geopackage.core.srs.SpatialReferenceSystemDao;
import mil.nga.giat.geopackage.core.srs.SpatialReferenceSystemSfSql;
import mil.nga.giat.geopackage.core.srs.SpatialReferenceSystemSfSqlDao;
import mil.nga.giat.geopackage.core.srs.SpatialReferenceSystemSqlMm;
import mil.nga.giat.geopackage.core.srs.SpatialReferenceSystemSqlMmDao;
import mil.nga.giat.geopackage.db.GeoPackageTableCreator;
import mil.nga.giat.geopackage.features.columns.GeometryColumns;
import mil.nga.giat.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.giat.geopackage.features.user.FeatureCursor;
import mil.nga.giat.geopackage.features.user.FeatureDao;
import mil.nga.giat.geopackage.features.user.FeatureTable;
import mil.nga.giat.geopackage.features.user.FeatureTableReader;
import mil.nga.giat.geopackage.tiles.matrix.TileMatrix;
import mil.nga.giat.geopackage.tiles.matrix.TileMatrixDao;
import mil.nga.giat.geopackage.tiles.matrixset.TileMatrixSet;
import mil.nga.giat.geopackage.tiles.matrixset.TileMatrixSetDao;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

/**
 * A single GeoPackage database connection implementation
 * 
 * @author osbornb
 */
class GeoPackageImpl implements GeoPackage {

	/**
	 * SQLite database
	 */
	private final SQLiteDatabase database;

	/**
	 * Cursor factory
	 */
	private final GeoPackageCursorFactory cursorFactory;

	/**
	 * Connection source for creating data access objects
	 */
	private final ConnectionSource connectionSource;

	/**
	 * Table creator
	 */
	private final GeoPackageTableCreator tableCreator;

	/**
	 * Constructor
	 * 
	 * @param database
	 * @param cursorFactory
	 * @param tableCreator
	 */
	GeoPackageImpl(SQLiteDatabase database,
			GeoPackageCursorFactory cursorFactory,
			GeoPackageTableCreator tableCreator) {
		this.database = database;
		this.cursorFactory = cursorFactory;
		this.connectionSource = new AndroidConnectionSource(database);
		this.tableCreator = tableCreator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		connectionSource.closeQuietly();
		database.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SQLiteDatabase getDatabase() {
		return database;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConnectionSource getConnectionSource() {
		return connectionSource;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SpatialReferenceSystemDao getSpatialReferenceSystemDao() {
		return createDao(SpatialReferenceSystem.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SpatialReferenceSystemSqlMmDao getSpatialReferenceSystemSqlMmDao() {

		SpatialReferenceSystemSqlMmDao dao = createDao(SpatialReferenceSystemSqlMm.class);
		verifyTableExists(dao);

		return dao;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SpatialReferenceSystemSfSqlDao getSpatialReferenceSystemSfSqlDao() {

		SpatialReferenceSystemSfSqlDao dao = createDao(SpatialReferenceSystemSfSql.class);
		verifyTableExists(dao);

		return dao;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ContentsDao getContentsDao() {
		ContentsDao dao = createDao(Contents.class);
		dao.setDatabase(database);
		return dao;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GeometryColumnsDao getGeometryColumnsDao() {
		return createDao(GeometryColumns.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createGeometryColumnsTable() {
		boolean created = false;
		GeometryColumnsDao dao = getGeometryColumnsDao();
		try {
			if (!dao.isTableExists()) {
				created = tableCreator.createGeometryColumns() > 0;
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to check if Geometry Columns table exists and create it",
					e);
		}
		return created;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FeatureDao getFeatureDao(GeometryColumns geometryColumns) {

		if (geometryColumns == null) {
			throw new GeoPackageException("Non null "
					+ GeometryColumns.class.getSimpleName()
					+ " is required to create "
					+ FeatureDao.class.getSimpleName());
		}

		// Read the existing table and create the dao
		FeatureTableReader tableReader = new FeatureTableReader(geometryColumns);
		final FeatureTable featureTable = tableReader.readTable(database);
		FeatureDao dao = new FeatureDao(database, geometryColumns, featureTable);

		// Register the table to wrap cursors with the feature cursor
		cursorFactory.registerTable(geometryColumns.getTableName(),
				new GeoPackageCursorWrapper() {

					@Override
					public Cursor wrapCursor(Cursor cursor) {
						return new FeatureCursor(featureTable, cursor);
					}
				});

		return dao;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FeatureDao getFeatureDao(Contents contents) {

		if (contents == null) {
			throw new GeoPackageException("Non null "
					+ Contents.class.getSimpleName()
					+ " is required to create "
					+ FeatureDao.class.getSimpleName());
		}

		GeometryColumns geometryColumns = contents.getGeometryColumns();
		if (geometryColumns == null) {
			throw new GeoPackageException("No "
					+ GeometryColumns.class.getSimpleName() + " exists for "
					+ Contents.class.getSimpleName() + " " + contents.getId());
		}

		return getFeatureDao(geometryColumns);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FeatureDao getFeatureDao(String tableName) throws SQLException {
		GeometryColumnsDao dao = getGeometryColumnsDao();
		List<GeometryColumns> geometryColumnsList = dao.queryForEq(
				GeometryColumns.COLUMN_TABLE_NAME, tableName);
		if (geometryColumnsList.isEmpty()) {
			throw new GeoPackageException(
					"No Feature Table exists for table name: " + tableName);
		} else if (geometryColumnsList.size() > 1) {
			// This shouldn't happen with the table name unique constraint on
			// geometry columns
			throw new GeoPackageException(
					"Unexpected state. More than one Geometry Column matched for table name: "
							+ tableName + ", count: "
							+ geometryColumnsList.size());
		}
		return getFeatureDao(geometryColumnsList.get(0));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createTable(FeatureTable table) {
		tableCreator.createTable(table);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TileMatrixSetDao getTileMatrixSetDao() {
		return createDao(TileMatrixSet.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createTileMatrixSetTable() {
		boolean created = false;
		TileMatrixSetDao dao = getTileMatrixSetDao();
		try {
			if (!dao.isTableExists()) {
				created = tableCreator.createTileMatrixSet() > 0;
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to check if Tile Matrix Set table exists and create it",
					e);
		}
		return created;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TileMatrixDao getTileMatrixDao() {
		return createDao(TileMatrix.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createTileMatrixTable() {
		boolean created = false;
		TileMatrixDao dao = getTileMatrixDao();
		try {
			if (!dao.isTableExists()) {
				created = tableCreator.createTileMatrix() > 0;
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to check if Tile Matrix table exists and create it",
					e);
		}
		return created;
	}

	/**
	 * Create a dao
	 * 
	 * @param type
	 * @return
	 */
	private <T, S extends BaseDaoImpl<T, ?>> S createDao(Class<T> type) {
		S dao;
		try {
			dao = DaoManager.createDao(connectionSource, type);
		} catch (SQLException e) {
			throw new GeoPackageException("Failed to create "
					+ type.getSimpleName() + " dao", e);
		}
		return dao;
	}

	/**
	 * Verify table or view exists
	 * 
	 * @param dao
	 */
	private void verifyTableExists(BaseDaoImpl<?, ?> dao) {
		try {
			if (!dao.isTableExists()) {
				throw new GeoPackageException(
						"Table or view does not exist for: "
								+ dao.getDataClass().getSimpleName());
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to detect if table or view exists for dao: "
							+ dao.getDataClass().getSimpleName(), e);
		}
	}

}
