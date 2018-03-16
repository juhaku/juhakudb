/**
MIT License

Copyright (c) 2018 juhaku

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package db.juhaku.juhakudb.core.android;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;

import java.io.IOException;
import java.util.Enumeration;

import dalvik.system.DexFile;
import db.juhaku.juhakudb.core.Criteria;
import db.juhaku.juhakudb.core.DatabaseConfiguration;
import db.juhaku.juhakudb.core.DatabaseConfigurationAdapter;
import db.juhaku.juhakudb.exception.SchemaInitializationException;
import db.juhaku.juhakudb.repository.RepositoryFactory;

/**
 * Created by juha on 16/12/15.
 * <p>DatabaseManager manages database initialization, schema creation as well as repository
 * initialization.</p>
 * <p>This class should be provided only once in application context in order to avoid unwanted
 * outcomes.</p>
 * <code>
 * Super activity or more likely in custom application you can implement this by:<br/><br/>
 * <p>
 * DatabaseManager dm = new DatabaseManager(this, new DatabaseConfigurationAdapter() &#123;<br/><br/>
 * <p>
 * &#9;&#64;Override<br/>
 * &#9;public void configure(DatabaseConfiguration configuration) &#123;<br/>
 * &#9;&#9;configuration.getBuilder().setBasePackages("db.juhaku.dbdemo.model", "db.juhaku.dbdemo.bean")<br/>
 * &#9;&#9;.setVersion(1).setName("dbtest.db").setMode(SchemaCreationMode.UPDATE);<br/>
 * &#9;&#125;<br/>
 * &#125;);<br/>
 * </code>
 *
 * @author juha
 * @since 1.0.2
 */
public class DatabaseManager {

    private DatabaseConfiguration configuration;
    private DatabaseHelper databaseHelper;
    private Object[] repositories = new Object[0];
    private EntityManager em;
    private RepositoryLookupInjector injector;
    private RepositoryFactory factory;

    /**
     * Initialize new DatabaseManager with current context and database configuration adapter.
     *
     * @param context instance of {@link Context} where to create manager for.
     * @param adapter instance of {@link DatabaseConfigurationAdapter} to provide configuration for
     *                this database manager.
     * @since 1.0.2
     */
    public DatabaseManager(Context context, DatabaseConfigurationAdapter adapter) {
        this.configuration = new DatabaseConfiguration();
        adapter.configure(configuration);

        Class<?>[] entityClasses = resolveClasses(context, new EntityCriteria(configuration.getBasePackages()));

        try {
            databaseHelper = new DatabaseHelper(context, entityClasses, configuration);

        } catch (SchemaInitializationException e) {
            Log.e(getClass().getName(), "Failed to create database", e);
        }

        em = new EntityManager(databaseHelper);
        factory = new RepositoryFactory(em, configuration.getBaseRepositoryClass());

        if (configuration.getRepositoryLocations() == null) {

            initializeRepositories(resolveClasses(context,
                    new RepositoryCriteria(context.getApplicationInfo().packageName)));

        } else {

            String[] locations = new String[configuration.getRepositoryLocations().length + 1];
            System.arraycopy(configuration.getRepositoryLocations(), 0, locations, 0,
                    configuration.getRepositoryLocations().length);
            locations[locations.length - 1] = context.getApplicationInfo().packageName;
            initializeRepositories(resolveClasses(context, new RepositoryCriteria(locations)));
        }

        if (configuration.isEnableAutoInject()) {
            injector = new RepositoryLookupInjector(this);
        }
    }

    /*
     * Resolve classes by given criteria and return resolved classes as array.
     */
    private Class<?>[] resolveClasses(Context context, Criteria criteria) {
        Class<?>[] retVal = new Class[0];
        for (DexFile dexFile : getSourceDexFiles(context)) {
            if (dexFile != null) {
                Enumeration<String> dexEntries = dexFile.entries();
                while (dexEntries.hasMoreElements()) {
                    String dexFileName = dexEntries.nextElement();
                    if (!criteria.meetCriteria(dexFileName)) {
                        continue;
                    }

                    Class<?> clazz = initializeClass(dexFileName);
                    if (clazz != null) {
                        int length = retVal.length;
                        Class<?>[] newRetVal = new Class[length + 1];
                        System.arraycopy(retVal, 0, newRetVal, 0, length);
                        newRetVal[length] = clazz;
                        retVal = newRetVal;
                    }
                }
            }
        }

        return retVal;
    }

    /**
     * Gets array of dex files for Android context. If Android API version is higher than 21
     * split sources are also added to the dex file array. Otherwise only base dex file is being added.
     *
     * @param context Android context to look for sources.
     * @return Array of dex files.
     * @since 2.1.2-SNAPSHOT
     */
    private DexFile[] getSourceDexFiles(Context context) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            boolean hasSplitSources = context.getApplicationInfo().splitSourceDirs != null;
            String[] paths = hasSplitSources ? new String[context.getApplicationInfo().splitSourceDirs.length] : new String[1];

            // If application has split sources add them to paths
            if (hasSplitSources) {
                for (int i = 0, splitSourcesLength = paths.length; i < splitSourcesLength; i++) {
                    paths[i] = context.getApplicationInfo().splitSourceDirs[i];
                }
            }

            // Lastly add base APK source dir to paths
            paths[paths.length - 1] = context.getApplicationInfo().sourceDir;

            return getSources(paths);
        } else {
            return getSources(new String[]{context.getApplicationInfo().sourceDir});
        }
    }

    /**
     * Get array of dex files from given source paths.
     *
     * @param paths String array of of source paths to create dex files from.
     * @return Array of dex files.
     * @since 2.1.2-SNAPSHOT
     */
    private DexFile[] getSources(String[] paths) {
        DexFile[] dexFiles = new DexFile[paths.length];

        try {
            for (int i = 0, pathsLength = paths.length; i < pathsLength; i++) {
                dexFiles[i] = new DexFile(paths[i]);
            }

        } catch (IOException e) {
            Log.e(getClass().getName(), "Could not initialize classes from ", e);
            dexFiles = new DexFile[0];
        }

        return dexFiles;
    }

    private static Class<?> initializeClass(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            Log.e(DatabaseManager.class.getName(), "Class could not be initialized by name: " +
                    name + ", table wont be created to database", e);
        }

        return null;
    }

    /*
     * Initialize given repository interfaces.
     */
    private <T> void initializeRepositories(Class[] interfaces) {
        for (Class<?> repository : interfaces) {
            T repo = (T) factory.getRepository(repository);
            int len = repositories.length;
            Object[] newRepos = new Object[len + 1];
            System.arraycopy(repositories, 0, newRepos, 0, len);
            repositories = newRepos;
            repositories[len] = repo;
        }
    }

    /**
     * Get repository from database manager for data access purposes.
     *
     * @param type Class<T> of type to look for repository.
     * @return Found repository as given type or null if not found.
     * @since 1.0.2
     */
    public <T> T getRepository(Class<T> type) {
        for (Object repository : repositories) {
            if (type.isAssignableFrom(repository.getClass())) {
                return (T) repository;
            }
        }

        return null;
    }

    /**
     * Call this method to inject automatically repositories to given object. Automatic annotation
     * based repository injection will be used if it is enabled by the {@link DatabaseConfiguration}.
     * <p>
     * <p>See {@link RepositoryLookupInjector#lookupRepositories(Object)} for additional details of
     * annotation based repository injection.</p>
     * <p>
     * <p>Calling this method can be done from any object that has access to database manager but
     * for sake of design it is only encouraged to do so from super Activities and super Fragments.
     * For most cases calling this method is not necessary in other application classes.</p>
     * <p>
     * <p>Since Android does not provide access to instantiated objects neither do we. You are free
     * to write your own running objects mapping system and call this method from there if needed.</p>
     * <p>
     * <p>This method is provided to give you leverage to execute repository lookup on creation
     * of objects in "Android way" and that's how we think it should be. In example this could be
     * something like following code executed in super activity.</p>
     * <code>
     * public void onCreate(Bundle bundle) &#123;<br/>
     * &#9;&#9;super.onCreate(bundle);<br>
     * &#9;&#9;getDatabaseManager().lookupRepositories(this);<br>
     * &#125;
     * </code>
     *
     * @param object Object to look for repositories to inject. e.g. your extended Activity.
     * @since 1.1.0
     */
    public void lookupRepositories(Object object) {
        // safety check
        if (configuration.isEnableAutoInject()) {
            injector.lookupRepositories(object);
        }
    }
}
