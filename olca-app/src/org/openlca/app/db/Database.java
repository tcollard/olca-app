package org.openlca.app.db;

import java.io.File;
import java.util.Objects;

import org.openlca.app.App;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.DataDir;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DatabaseConfigList;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Database management of the application. */
public class Database {

	private static IDatabase database;
	private static DatabaseConfig config;
	private static DatabaseListener listener;
	private static final DatabaseConfigList configurations = loadConfigs();

	private Database() {
	}

	public static IDatabase get() {
		return database;
	}

	public static WorkspaceIdUpdater getWorkspaceIdUpdater() {
		return listener.getWorkspaceIdUpdater();
	}

	public static IDatabase activate(DatabaseConfig config) {
		try {
			database = config.connect(DataDir.databases());
			listener = new DatabaseListener();
			database.addListener(listener);
			Cache.create(database);
			Database.config = config;
			Logger log = LoggerFactory.getLogger(Database.class);
			log.trace("activated database {} with version{}",
					database.getName(), database.getVersion());
			Repository.connect(database);
			return database;
		} catch (Exception e) {
			database = null;
			Cache.close();
			Database.config = null;
			ErrorReporter.on("failed to activate database: " + config, e);
			return null;
		}
	}

	public static boolean isActive(DatabaseConfig config) {
		if (config == null)
			return false;
		return Objects.equals(config, Database.config);
	}

	/** Closes the active database. */
	public static void close() throws Exception {
		if (database == null)
			return;
		Cache.close();
		CopyPaste.clearCache();
		database.close();
		database = null;
		listener = null;
		config = null;
		if (Repository.get() != null) {
			Repository.disconnect();
		}
	}

	private static DatabaseConfigList loadConfigs() {
		var workspace = App.getWorkspace();
		var listFile = new File(workspace, "databases.json");
		return !listFile.exists()
				? new DatabaseConfigList()
				: DatabaseConfigList.read(listFile);
	}

	private static void saveConfig() {
		File workspace = App.getWorkspace();
		File listFile = new File(workspace, "databases.json");
		configurations.write(listFile);
	}

	public static DatabaseConfigList getConfigurations() {
		return configurations;
	}

	public static DatabaseConfig getActiveConfiguration() {
		for (var conf : configurations.getDerbyConfigs())
			if (isActive(conf))
				return conf;
		for (var conf : configurations.getMySqlConfigs())
			if (isActive(conf))
				return conf;
		return null;
	}

	public static void register(DerbyConfig config) {
		if (configurations.contains(config))
			return;
		configurations.getDerbyConfigs().add(config);
		saveConfig();
	}

	public static void remove(DerbyConfig config) {
		if (!configurations.contains(config))
			return;
		configurations.getDerbyConfigs().remove(config);
		saveConfig();
	}

	public static void register(MySqlConfig config) {
		if (configurations.contains(config))
			return;
		configurations.getMySqlConfigs().add(config);
		saveConfig();
	}

	public static void remove(MySqlConfig config) {
		if (!configurations.contains(config))
			return;
		configurations.getMySqlConfigs().remove(config);
		saveConfig();
	}

}
