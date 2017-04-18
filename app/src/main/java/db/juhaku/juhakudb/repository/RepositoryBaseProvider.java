package db.juhaku.juhakudb.repository;

import db.juhaku.juhakudb.repository.android.SimpleAndroidRepository;

/**
 * Created by juha on 18/04/17.
 *
 * <p>This class is repository base provider in case the default implementation is not functional
 * enough.</p>
 *
 * <p>Provide Interface for the custom repository and Base implementing class for the repository.</p>
 *
 * <p>Implementation of this class will override the default simple android repository functionality.
 * This is particularly useful if custom methods is wished to implement all of the repositories.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.2.1-SNAPSHOT
 */
public abstract class RepositoryBaseProvider {

    /**
     * Custom base interface class for repository.
     * @return Class for interface.
     *
     * @since 1.2.1-SNAPSHOT
     */
    public abstract Class<? extends SimpleRepository> getRepositoryBaseInterface();

    /**
     * Custom base class for repository.
     * @return Implementing class for repository.
     *
     * @since 1.2.1-SNAPSHOT
     */
    public abstract Class<? extends SimpleAndroidRepository> getRepositoryBaseClass();

}
