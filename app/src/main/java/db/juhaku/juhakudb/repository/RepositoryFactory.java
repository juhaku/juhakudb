package db.juhaku.juhakudb.repository;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import db.juhaku.juhakudb.annotation.Repository;
import db.juhaku.juhakudb.annotation.Repository.NoRepository;
import db.juhaku.juhakudb.core.android.EntityManager;
import db.juhaku.juhakudb.repository.android.SimpleAndroidRepository;
import db.juhaku.juhakudb.util.ReflectionUtils;

/**
 * Created by juha on 18/04/17.
 *
 * <p>Factory class for creating repositories for SQLite databases. </p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.3.0
 */
public class RepositoryFactory {

    private EntityManager em;
    private Class<? extends SimpleAndroidRepository> baseRepositoryClass;

    /**
     * Create new instance of repository factory with entity manager. Repository factory is for
     * creating new repositories for SQLite database that can be used freely in application with
     * {@code @Inject} annotation or by retrieving it repository from database manager.
     *
     * @param entityManager Instance of {@link EntityManager}.
     * @param baseRepositoryClass Class of custom base repository.
     *
     * @since 1.3.0
     */
    public RepositoryFactory(EntityManager entityManager,
                             Class<? extends SimpleAndroidRepository> baseRepositoryClass) {
        this.em = entityManager;
        this.baseRepositoryClass = baseRepositoryClass;
    }

    /**
     * Create new instance of repository for given interface class.
     *
     * @param interf Class of interface which repository will be created.
     * @return Instance of created repository for the given interface.
     *
     * @since 1.3.0
     */
    public <T> T getRepository(Class<T> interf) {

        if (interf.isAnnotationPresent(Repository.class)) {

            Class<?> impl = interf.getAnnotation(Repository.class).value();

            /*
             * If impl refers to no repository then use default implementation for interface.
             * If custom base class is provided create implementation from it.
             */
            if (impl.isAssignableFrom(NoRepository.class)) {
                Class<?> entity = ReflectionUtils.getInterfaceGenericTypes(interf)[1];

                if (baseRepositoryClass == null) {

                    return proxyImpl(interf, new SimpleAndroidRepository(em, entity) {});
                } else {

                    return proxyImpl(interf, (SimpleAndroidRepository)
                            customBaseImpl(baseRepositoryClass, em, entity));
                }


            } else {

                // If real implementation is provided return the provided impl for interface.
                return (T) customImpl(interf.getAnnotation(Repository.class).value(), em);
            }

        }

        return null;
    }

    /**
     * Create custom repository implementation for interface by given implementing class. Class must
     * have constructor with one parameter for {@link EntityManager}.
     *
     * @param impl Class of implementing class of repository.
     * @param em Instance of{@link EntityManager}.
     * @return Custom repository implementation.
     *
     * @since 1.3.0
     *
     * @hide
     */
    private static <T> T customImpl(Class<T> impl, EntityManager em) {

        T repository = ReflectionUtils.instantiateConstructor(impl, em);

        if (repository == null) {
            Log.w(RepositoryFactory.class.getName(), "could not initialize custom impl: "
                    + impl + " with params: " + em);
        }

        return repository;
    }

    /**
     * Create custom base repository by provided base impl class. If used this will be default
     * repository base for all the repositories that are not using custom repository implementation.
     *
     * @param baseImpl Base class that extends {@link SimpleAndroidRepository} to create repository for.
     * @param em Instance of {@link EntityManager}.
     * @param entity Class of the entity that is being managed by the repository.
     * @return  Custom base repository implementation.
     *
     * @since 1.3.0
     *
     * @hide
     */
    private static <T> T customBaseImpl(Class<?> baseImpl, EntityManager em, Class<?> entity) {
        T repository = ReflectionUtils.instantiateConstructor(baseImpl, em, entity);

        if (repository == null) {
            Log.w(RepositoryFactory.class.getName(), "Could not instantiate repository: " + baseImpl
                    + " with: " + em + " and " + entity.getName());

        }

        return repository;
    }

    /**
     * Create proxy implementation of repository for given interface. This is the default scenario
     * when there is no custom implementation class provided. {@link SimpleAndroidRepository} will
     * be proxied to the interface of the repository.
     *
     * @param interf Class instance of repository interface.
     * @param simpleAndroidRepository Instance of {@link SimpleAndroidRepository} to provide proxy for.
     * @return Proxied repository for given interface class.
     *
     * @since 1.3.0
     *
     * @hide
     */
    private static <T> T proxyImpl(final Class<T> interf, final SimpleAndroidRepository simpleAndroidRepository) {
        return (T) Proxy.newProxyInstance(interf.getClassLoader(), new Class[]{interf}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Method repositoryMethod = findMethodWithSameErasure(method);

                if (repositoryMethod == null) {
                    Log.w(getClass().getName(), "Could not find method: " + method.getName() + " with params: "
                            + method.getParameterTypes() + " with return type: " + method.getReturnType() + " from repository: " + interf);
                    throw new NoSuchMethodException("Method with same erasure not found, cannot execute method: " + method.getName());
                }

                // Add some trace logging if enabled.
                Log.v(getClass().getName(), "Executing method: " + method.getName() + " with args: " + args + " in: " + interf);

                return repositoryMethod.invoke(simpleAndroidRepository, args);
            }

            /**
             * Find method with same erasure as is being executed in proxy interface. Method is
             * checked by name, return type and parameter types and order or parameter types.
             *
             * @param method Method to look for.
             * @return Found method or null.
             *
             * @since 1.3.0
             *
             * @hide
             */
            private Method findMethodWithSameErasure(Method method) {
                for (Method m : simpleAndroidRepository.getClass().getMethods()) {
                    // Set method accessible.
                    boolean accessible = m.isAccessible();
                    m.setAccessible(true);

                    // Check that we have right method by name and return type.
                    if (m.getName().equals(method.getName()) && m.getReturnType().equals(method.getReturnType())) {

                        boolean hasParams = true;

                        // Check that method has same amount of parameters with same classes.
                        for (int i = 0; i < m.getParameterTypes().length; i++) {

                            if (m.getParameterTypes()[i].equals(method.getParameterTypes()[i])) {
                                continue;
                            }

                            hasParams = false;
                            break;
                        }

                        // If all the parameters are same execution can be processed.
                        if (hasParams) {
                            return m;
                        }
                    }

                    // Restore default accessible status.
                    m.setAccessible(accessible);
                }

                return null;
            }

        });
    }
}
