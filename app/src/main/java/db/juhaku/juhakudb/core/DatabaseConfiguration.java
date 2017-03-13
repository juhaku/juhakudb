package db.juhaku.juhakudb.core;

import db.juhaku.juhakudb.core.schema.SchemaCreationMode;

/**
 * Created by juha on 04/12/15.
 *<p>User level configuration class that provides user defined configurations for the database.</p>
 *
 * @author Juha Kukkonen
 * @since 1.0.2
 */
public class DatabaseConfiguration {

    private String name;
    private int version;
    private String[] basePackages;
    private boolean allowRollback;
    private int rollbackHistorySize;
    private SchemaCreationMode mode;
    private String[] repositoryLocations;
    private boolean enableAutoInject;

    /**
     * @return String value of database name
     *
     * @since 1.0.2
     */
    public String getName() {
        return name;
    }

    /**
     * @param name String value of database name.
     *
     * @since 1.0.2
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return int value of database version.
     *
     * @since 1.0.2
     */
    public int getVersion() {
        return version;
    }

    /**
     * Changing the version will cause database to upgrade or downgrade depending on version number.
     * @param version int value of database version.
     *
     * @since 1.0.2
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @return String array or base packages for entities.
     *
     * @since 1.0.2
     */
    public String[] getBasePackages() {
        return basePackages;
    }

    /**
     * Set base packages for entity locations what are used in database and schema creation.
     * @param basePackages String array of base packages for entities.
     *
     * @since 1.0.2
     */
    public void setBasePackages(String... basePackages) {
        this.basePackages = basePackages;
    }

    /**
     * @return boolean value whether rollback is allowed.
     *
     * @since 1.0.2
     */
    public boolean isAllowRollback() {
        return allowRollback;
    }

    /**
     * @param allowRollback boolean value for allowing rollback.
     *
     * @since 1.0.2
     */
    public void setAllowRollback(boolean allowRollback) {
        this.allowRollback = allowRollback;
    }

    /**
     * @return int value of number or backups are stored from database schema.
     * @since 1.0.2
     */
    public int getRollbackHistorySize() {
        return rollbackHistorySize;
    }

    /**
     * Set number of backups will be stored from database schema.
     * @param rollbackHistorySize int value of number of backups allowed to store.
     *
     * @since 1.0.2
     */
    public void setRollbackHistorySize(int rollbackHistorySize) {
        this.rollbackHistorySize = rollbackHistorySize;
    }

    /**
     * @return instance of {@link SchemaCreationMode} that is used with current database.
     * @since 1.0.2
     */
    public SchemaCreationMode getMode() {
        return mode;
    }

    /**
     * @param mode {@link SchemaCreationMode} that use used with current database.
     * @since 1.0.2
     */
    public void setMode(SchemaCreationMode mode) {
        this.mode = mode;
    }

    /**
     * @return String array of additional repository locations.
     */
    public String[] getRepositoryLocations() {
        return repositoryLocations;
    }

    /**
     * Set additional repository locations for looking repositories that need to be initialized
     * for the application. By default application package is used to look for repositories which makes
     * this not mandatory. However if repositories are out of application package then the packages
     * need to be provided via this method.
     * @param repositoryLocations String array of additional repository package locations.
     *
     * @since 1.0.2
     */
    public void setRepositoryLocations(String... repositoryLocations) {
        this.repositoryLocations = repositoryLocations;
    }

    /**
     * Check is annotation based inject repositories enabled or not.
     * @return true if auto inject repositories is enabled; false otherwise.
     * @since 1.0.8-SNAPSHOT
     */
    public boolean isEnableAutoInject() {
        return enableAutoInject;
    }

    /**
     * Set annotation based inject repositories enabled. Automatic annotation based repository injection
     * will only become effective on objects that calls
     * {@link db.juhaku.juhakudb.core.android.DatabaseManager#lookupRepositories(Object)}
     * method somewhere in application execution context before the repositories is being used.
     *
     * @param enableAutoInject boolean value to define whether annotation based repository injection
     *                         is enabled.
     * @since 1.0.8-SNAPSHOT
     */
    public void setEnableAutoInject(boolean enableAutoInject) {
        this.enableAutoInject = enableAutoInject;
    }

    /**
     * @return new instance of {@link db.juhaku.juhakudb.core.DatabaseConfiguration.Builder} to
     * build configuration.
     */
    public Builder getBuilder() {
        return new Builder(this);
    }

    /**
     * Convenience class to build database configuration by chaining methods.
     *
     * @since 1.0.2
     */
    public static class Builder {

        private DatabaseConfiguration databaseConfiguration;

        /**
         * Initialize database configuration builder for given database configuration.
         * @param databaseConfiguration instance of {@link DatabaseConfiguration} that is being built.
         *
         * @since 1.0.2
         */
        public Builder(DatabaseConfiguration databaseConfiguration) {
            this.databaseConfiguration = databaseConfiguration;
        }

        /**
         * {@link DatabaseConfiguration#setName(String)}
         *
         * @since 1.0.2
         */
        public Builder setName(String name) {
            databaseConfiguration.setName(name);
            return this;
        }

        /**
         * {@link DatabaseConfiguration#setVersion(int)}
         *
         * @since 1.0.2
         */
        public Builder setVersion(int version) {
            databaseConfiguration.setVersion(version);
            return this;
        }

        /**
         *  {@link DatabaseConfiguration#setBasePackages(String...)}
         *
         *  @since 1.0.2
         */
        public Builder setBasePackages(String... basePackages) {
            databaseConfiguration.setBasePackages(basePackages);
            return this;
        }

        /**
         * {@link DatabaseConfiguration#setAllowRollback(boolean)}
         *
         * @since 1.0.2
         */
        public Builder setAllowRollback(boolean allowRollback) {
            databaseConfiguration.setAllowRollback(allowRollback);
            return this;
        }

        /**
         * {@link DatabaseConfiguration#setRollbackHistorySize(int)}
         *
         * @since 1.0.2
         */
        public Builder setRollbackHistorySize(int rollbackHistorySize) {
            databaseConfiguration.setRollbackHistorySize(rollbackHistorySize);
            return this;
        }

        /**
         * {@link DatabaseConfiguration#setMode(SchemaCreationMode)}
         *
         * @since 1.0.2
         */
        public Builder setMode(SchemaCreationMode mode) {
            databaseConfiguration.setMode(mode);
            return this;
        }

        /**
         * {@link DatabaseConfiguration#setRepositoryLocations(String...)}
         *
         * @since 1.0.2
         */
        public Builder setRepositoryLocations(String... repositoryLocations) {
            databaseConfiguration.setRepositoryLocations(repositoryLocations);
            return this;
        }

        /**
         * {@link DatabaseConfiguration#setEnableAutoInject(boolean)}
         *
         * @since 1.0.8-SNAPSHOT
         */
        public Builder setEnableAutoInject(boolean enableAutoInject) {
            databaseConfiguration.setEnableAutoInject(enableAutoInject);
            return this;
        }
    }
}
