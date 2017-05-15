/**
MIT License

Copyright (c) 2017 juhaku

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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map.Entry;

import db.juhaku.juhakudb.core.schema.Constraint;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.core.schema.Schema.DDL;
import db.juhaku.juhakudb.core.schema.SchemaCreationMode;
import db.juhaku.juhakudb.core.DatabaseConfiguration;
import db.juhaku.juhakudb.exception.SchemaInitializationException;

/**
 * Created by juha on 22/11/15.
 * <p>This class provides simplified access to SQLite database inside android device.
 * Instance to database is automatically dropped once application is closed. Once instance is acquired
 * it remains open {@link #closeDb()} is called. It should be called on application close.</p>
 * @author juha
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /*
     * Private folder location for database version.
     */
    private static final String DB_FOLDER = ".juhaku_dbs";
    private Schema schema;
    private SQLiteDatabase db;
    private DatabaseConfiguration databaseConfiguration;
    private Context context;

    public DatabaseHelper(Context context, Class<?>[] entities, DatabaseConfiguration configuration)
            throws SchemaInitializationException {
        super(context, configuration.getName(), null, configuration.getVersion());
        this.databaseConfiguration = configuration;
        this.context = context;
        this.schema = Schema.newInstance(configuration, entities);
        persist();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for (Schema table : Schema.toSet(schema)) {
            sqLiteDatabase.execSQL(table.toDDL(DDL.CREATE));
            createConstrains(sqLiteDatabase, table);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Schema oldSchema = restoreSchema(oldVersion);

        switch (databaseConfiguration.getMode()) {
            case CREATE:
                if (oldSchema != null) {
                    dropTables(sqLiteDatabase, oldSchema);
                }
                onCreate(sqLiteDatabase);
                break;
            case UPDATE:
                updateDb(sqLiteDatabase, oldSchema);
                break;
            default:
                throw new IllegalArgumentException("Illegal mode: " + databaseConfiguration.getMode()
                        + ", should be either: " + SchemaCreationMode.CREATE
                        + " or " + SchemaCreationMode.UPDATE);
        }
    }

    private void dropTables(SQLiteDatabase db, Schema oldSchema) {
        for (Schema table : Schema.toSet(oldSchema)) {
            db.execSQL(table.toDDL(DDL.DROP));
        }
    }

    /**
     * Create constraints if they do not exist in database.
     *
     * @param db SQLiteDatabase to add constraints for to specific table.
     * @param table {@link Schema} table to add constraints for.
     *
     * @since 2.0.2-SNAPSHOT
     */
    private void createConstrains(SQLiteDatabase db, Schema table) {
        for (Constraint ctx : table.getConstraints()) {
            db.execSQL(ctx.toString());
        }
    }

    /**
     * Create or update database tables based on comparison. Tables will be inserted or
     * altered but nothing will be removed.
     * @param db SQLiteDatabase to save tables for.
     * @param oldSchema Schema to compare current {@link Schema} for.
     */
    private void updateDb(SQLiteDatabase db, Schema oldSchema) {
        for (Schema table : Schema.toSet(schema)) {
            Schema oldTable;
            // if table is not found from previous schema, add it, otherwise alter it if necessary.
            if ((oldTable = oldSchema.getElement(table.getName())) == null) {
                db.execSQL(table.toDDL(DDL.CREATE));
            } else {
                for (Entry<String, Schema> entry : table.getElements().entrySet()) {
                    // if old column not found, add it.
                    if (oldTable.getElement(entry.getKey()) == null) {
                        db.execSQL(DDL.alterTable(table, entry.getValue()));
                    }
                }
            }
            createConstrains(db, table);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (databaseConfiguration.isAllowRollback()) {
            Schema oldSchema = restoreSchema(oldVersion);
            if (oldSchema != null) {
                for (Schema table : Schema.toSet(oldSchema)) {
                    db.execSQL(table.toDDL(DDL.DROP));
                }
            }
            Schema requestedSchema = restoreSchema(newVersion);
            if (requestedSchema != null) {
                for (Schema table : Schema.toSet(requestedSchema)) {
                    db.execSQL(table.toDDL(DDL.CREATE));
                }
            }
        } else {
            // throws SQLiteException
            super.onDowngrade(db, oldVersion, newVersion);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Get read write instance of database for current application context.
     * @return SQLiteDatabase
     */
    public SQLiteDatabase getDb() {
        if (db == null) {
            db = getWritableDatabase();
        }
        return db;
    }

    /**
     * Get current schema used with database.
     * @return instance of {@link Schema}.
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Execute SQL string in current database.
     * <p><strong>Note!</strong> Executing sql via this ignores return result. Use this to
     * make database changes.</p>
     * @param sql String SQL to execute.
     * @see SQLiteDatabase#execSQL(String)
     */
    void execSQL(String sql) {
        getDb().execSQL(sql);
    }

    /**
     * Closes any open database connection. This should be called when database connection
     * is no longer needed. Mainly this should be called on application close.
     */
    void closeDb() {
        if (db != null) {
            close();
        }
    }

    /**
     * Generates database schema file name based on current configuration given.
     * @param databaseConfiguration instance of {@link DatabaseConfiguration}.
     * @return String value as name of the file.
     */
    private static String generateSchemaName(DatabaseConfiguration databaseConfiguration) {
        return new StringBuilder(databaseConfiguration.getName()).append("_")
                .append(databaseConfiguration.getVersion())
                .append(".dbs").toString();
    }

    private File getSchemaFolder() {
        File schemaFolder = new File(context.getFilesDir().getAbsolutePath(), DB_FOLDER);
        if (!schemaFolder.exists()) {
            Log.i(getClass().getName(), "folder: " + schemaFolder.getAbsolutePath()
                    + " did not found, it will be created!");
            if (!schemaFolder.mkdirs()) {
                Log.w(getClass().getName(), "Failed to create schema folder location!");
            }
        }

        return schemaFolder;
    }

    /**
     * Tries to restore {@link Schema} by given old version.
     * @param oldVersion int value of old version of the schema.
     * @return instance of {@link Schema} if restoration is successful. If schema could not
     * be restored nor it is not found null will be returned instead.
     */
    private Schema restoreSchema(int oldVersion) {
        Schema schema = null;
        File schemaFolder = getSchemaFolder();

        int currentVersion = databaseConfiguration.getVersion();
        databaseConfiguration.setVersion(oldVersion);
        String schemaFileName = generateSchemaName(databaseConfiguration);
        databaseConfiguration.setVersion(currentVersion);

        File schemaFile = new File(schemaFolder.getAbsolutePath(), schemaFileName);
        if (schemaFile.exists()) {
            ObjectInputStream in = null;
            try {
                in =  new ObjectInputStream(new FileInputStream(schemaFile));
                schema = (Schema) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                Log.e(getClass().getName(), "Schema restoration failed", e);
            } finally {
                // try closing the stream
                closeStream(in);
            }
        }

        return schema;
    }

    /**
     * Tires to persist current {@link Schema} to folder.
     * @return boolean value; true if successful; false if not.
     */
    private boolean persist() {
        if (databaseConfiguration.getRollbackHistorySize() < databaseConfiguration.getVersion()) {
            Log.w(getClass().getName(), "Failed to persist schema, maximum allowed history exceeded: "
                    + databaseConfiguration.getRollbackHistorySize() + ", current version: " + databaseConfiguration.getVersion());
            return false;
        }
        File schemaFile = new File(getSchemaFolder(), generateSchemaName(databaseConfiguration));

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(schemaFile));
            out.writeObject(schema);

        } catch (IOException e) {
            Log.e(getClass().getName(), "Schema persis failure", e);
            return false;
        } finally {
            // try closing the stream
            closeStream(out);
        }

        return true;
    }

    /**
     * Closes any closable stream or object. Errors during closing will be logged.
     * @param closeable Instance of {@link Closeable} to close.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private static void closeStream(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            Log.e(DatabaseHelper.class.getName(), "Failed to close object stream", e);
        }
    }
}
