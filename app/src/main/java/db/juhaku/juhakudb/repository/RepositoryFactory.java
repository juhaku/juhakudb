package db.juhaku.juhakudb.repository;

import android.util.Log;

import java.lang.reflect.Constructor;
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
 * @since 1.2.1-SNAPSHOT
 */
public class RepositoryFactory {

    private EntityManager em;

    public RepositoryFactory(EntityManager entityManager) {
        this.em = entityManager;
    }

    /**
     * Create new instance of repository for given interface class.
     *
     * @param interf Class of interface which repository will be created.
     * @return Instance of created repository for the given interface.
     *
     * @since 1.2.1-SNAPSHOT
     */
    public <T> T getRepository(Class<T> interf) {

        if (interf.isAnnotationPresent(Repository.class)) {

            Class<?> impl = interf.getAnnotation(Repository.class).value();

            if (impl.isAssignableFrom(NoRepository.class)) {
                Class<?> entity = ReflectionUtils.getInterfaceGenericTypes(interf)[1];

                return proxyImpl(interf, new SimpleAndroidRepository(em, entity) {});

            } else {

                return (T) customImpl(interf.getAnnotation(Repository.class).value(), em);
            }

        }

        return null;
    }

    /**
     * Create custom repository implementation for interface by given implementing class. Class must
     * have constructor with one parameter for {@link EntityManager}.
     * @param impl Class of implementing class of repository.
     * @param em Instance of{@link EntityManager}.
     * @return Custom repository implementation.
     *
     * @since 1.2.1-SNAPSHOT
     *
     * @hide
     */
    private static <T> T customImpl(Class<T> impl, EntityManager em) {
        for (Constructor cons : impl.getDeclaredConstructors()) {
            Class[] params = cons.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(EntityManager.class)) {
                try {
                    return (T) cons.newInstance(em);

                } catch (Exception e) {
                    Log.w(RepositoryFactory.class.getName(), "could not initialize constructor with params: "
                            + em, e);
                }
            }
        }

        return null;
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
     * @since 1.2.1-SNAPSHOT
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
                Log.v(getClass().getName(), "Executing method: " + method + "with args: " + args + " in: " + interf);

                return repositoryMethod.invoke(simpleAndroidRepository, args);
            }

            /**
             * Find method with same erasure as is being executed in proxy interface. Method is
             * checked by name, return type and parameter types and order or parameter types.
             *
             * @param method Method to look for.
             * @return Found method or null.
             *
             * @since 1.2.1-SNAPSHOT
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
