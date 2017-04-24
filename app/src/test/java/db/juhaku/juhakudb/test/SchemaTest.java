package db.juhaku.juhakudb.test;

import org.junit.Test;

import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.core.schema.Schema.DDL;
import db.juhaku.juhakudb.core.DatabaseConfiguration;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.Filters;
import db.juhaku.juhakudb.filter.JoinMode;
import db.juhaku.juhakudb.filter.Order;
import db.juhaku.juhakudb.filter.Predicate;
import db.juhaku.juhakudb.filter.Predicate.Disjunction;
import db.juhaku.juhakudb.filter.Predicates;
import db.juhaku.juhakudb.filter.Query;
import db.juhaku.juhakudb.filter.QueryProcessor;
import db.juhaku.juhakudb.filter.Root;
import db.juhaku.juhakudb.test.bean.Authority;
import db.juhaku.juhakudb.test.bean.ClassRoom;
import db.juhaku.juhakudb.test.bean.Group;
import db.juhaku.juhakudb.test.bean.Permission;
import db.juhaku.juhakudb.test.bean.Person;
import db.juhaku.juhakudb.test.bean.Teacher;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by juha on 22/12/15.
 *
 * @author juha
 */
public class SchemaTest {
    @Test
    public void createCorrectSchema() throws Exception {
        DatabaseConfiguration configuration = new DatabaseConfiguration();
        configuration.setName("testdb");
        configuration.setBasePackages("db.juhaku.juhakudb.test.bean");
        configuration.setVersion(1);
        Schema schema = Schema.newInstance(configuration, new Class<?>[]{
                Teacher.class, Person.class, ClassRoom.class, Group.class, Authority.class, Permission.class});
        assertNotNull("schema not null", schema);
//        assertEquals("schema wrong table amount", 3, schema.getElements().size());
        for (Schema element : Schema.toSet(schema)) {
            System.out.println("schema: " + element.getName() + " " + element.getOrder());
            System.out.println("ddl:" + element.toDDL(DDL.CREATE));
//            for (Reference reference : element.getReferences()) {
//                System.out.println(reference.toDDL());
//            }
        }
    }

    @Test
    public void testFilters() throws Exception {
        DatabaseConfiguration configuration = new DatabaseConfiguration();
        configuration.setName("testdb");
        configuration.setBasePackages("db.juhaku.juhakudb.test.bean");
        configuration.setVersion(1);
        Schema schema = Schema.newInstance(configuration, new Class<?>[]{
                Teacher.class, Person.class, ClassRoom.class, Group.class});

        QueryProcessor processor = new QueryProcessor(schema);

        Filters filters = new Filters();
        filters.add(new Filter<Person>() {
            @Override
            public void filter(Root<Person> root, Predicates predicates) {
                root.join("this.rooms", "r", JoinMode.LEFT_JOIN).join("r.teacher", "t", JoinMode.FULL_JOIN);
                root.join("this.groups", "g", JoinMode.INNER_JOIN);

                predicates.add(Predicate.in("this.name", "matti", "kimmo")).add(Predicate.not(Predicate.eq("name", "lauri")));

//                predicate.in("p.name", "matti", "kimmo").not(predicate.eq("p.name", "lauri"));

                Disjunction or = Predicate.disjunction();
                or.add(Predicate.eq("t.name", "laura")).add(Predicate.eq("t.name", "minna"));
                predicates.add(or);

                predicates.add(Predicate.conjunction().add(Predicate.between("t.id", 1, 3))
                        .add(Predicate.not(Predicate.isNull("t.name"))));

                predicates.sort(Order.ASC, "p.name");
                predicates.setPageSize(20).setPage(1);

//                and.between("t.id", 1, 3).not(predicates.isNull("t.name"));
            }
        });

        Query query = processor.createQuery(Person.class, filters);
        assertNotNull("query not null", query);
        StringBuilder argBuilder = new StringBuilder();
        for (String arg : query.getArgs()) {
            argBuilder.append(arg).append(", ");
        }
        System.out.println(query + "\n" + argBuilder.toString());
    }

    @Test
    public void testFilters2() throws Exception {
        DatabaseConfiguration configuration = new DatabaseConfiguration();
        configuration.setName("testdb");
        configuration.setBasePackages("db.juhaku.juhakudb.test.bean");
        configuration.setVersion(1);
        Schema schema = Schema.newInstance(configuration, new Class<?>[]{
                Teacher.class, Person.class, ClassRoom.class, Group.class});

        QueryProcessor processor = new QueryProcessor(schema);

        Filters filters = new Filters();
        filters.add(new Filter<Teacher>() {
            @Override
            public void filter(Root<Teacher> root, Predicates predicates) {
                root.join("classRoom", "r", JoinMode.INNER_JOIN);
            }
        });

        Query query = processor.createQuery(Teacher.class, filters);
        assertNotNull("query not null", query);
        System.out.println(query);
    }

    @Test
    public void testFilters3() throws Exception {
        DatabaseConfiguration configuration = new DatabaseConfiguration();
        configuration.setName("testdb");
        configuration.setBasePackages("db.juhaku.juhakudb.test.bean");
        configuration.setVersion(1);
        Schema schema = Schema.newInstance(configuration, new Class<?>[]{
                Teacher.class, Person.class, ClassRoom.class, Group.class});

        QueryProcessor processor = new QueryProcessor(schema);

        Filters filters = new Filters();
        filters.add(new Filter<Group>() {
            @Override
            public void filter(Root<Group> root, Predicates predicates) {
                root.join("person", "p", JoinMode.LEFT_JOIN);
            }
        });

        Query query = processor.createQuery(Group.class, filters);
        assertNotNull("query not null", query);
        System.out.println(query);
    }

    @Test
    public void testFilters4() throws Exception {
        DatabaseConfiguration configuration = new DatabaseConfiguration();
        configuration.setName("testdb");
        configuration.setBasePackages("db.juhaku.juhakudb.test.bean");
        configuration.setVersion(1);
        Schema schema = Schema.newInstance(configuration, new Class<?>[]{
                Teacher.class, Person.class, ClassRoom.class, Group.class});

        QueryProcessor processor = new QueryProcessor(schema);

        Filters filters = new Filters();
        filters.add(new Filter<ClassRoom>() {
            @Override
            public void filter(Root<ClassRoom> root, Predicates predicates) {
                root.join("persons", "p", JoinMode.INNER_JOIN);
            }
        });

        Query query = processor.createQuery(ClassRoom.class, filters);
        assertNotNull("query not null", query);
        System.out.println(query);
    }

    @Test
    public void testFilter5() throws Exception {
        DatabaseConfiguration configuration = new DatabaseConfiguration();
        configuration.setName("testdb");
        configuration.setBasePackages("db.juhaku.juhakudb.test.bean");
        configuration.setVersion(1);
        Schema schema = Schema.newInstance(configuration, new Class<?>[]{
                Teacher.class, Person.class, ClassRoom.class, Group.class});

        QueryProcessor processor = new QueryProcessor(schema);

        Filters filters = new Filters(new Filter<ClassRoom>() {
            @Override
            public void filter(Root<ClassRoom> root, Predicates predicates) {
                root.join("persons", JoinMode.INNER_JOIN);
            }
        }, new Filter<ClassRoom>() {
            @Override
            public void filter(Root<ClassRoom> root, Predicates predicates) {
                root.join("this.teacher", "t", JoinMode.LEFT_JOIN);

                predicates.add(Predicate.ge("id", 1));
            }
        }, new Filter<ClassRoom>() {
            @Override
            public void filter(Root<ClassRoom> root, Predicates predicates) {
                predicates.add(Predicate.eq("t.name", "tester"));
            }
        });

        Query query = processor.createQuery(ClassRoom.class, filters);
        assertNotNull("query not null", query);
        System.out.println(query);
    }

    @Test
    public void testFilter6() throws Exception {
        DatabaseConfiguration configuration = new DatabaseConfiguration();
        configuration.setName("testdb");
        configuration.setBasePackages("db.juhaku.juhakudb.test.bean");
        configuration.setVersion(1);
        Schema schema = Schema.newInstance(configuration, new Class<?>[]{
                Teacher.class, Person.class, ClassRoom.class, Group.class});

        QueryProcessor processor = new QueryProcessor(schema);

        Filters filters = new Filters(new Filter<Person>() {
            @Override
            public void filter(Root<Person> root, Predicates predicates) {
                root.fetch("groups", JoinMode.INNER_JOIN);
                root.fetch("rooms", "r", JoinMode.LEFT_JOIN);
            }
        }, new Filter<Person>() {
            @Override
            public void filter(Root<Person> root, Predicates predicates) {
                predicates.add(Predicate.eq("this.name", "tester"));
            }
        });

        Query query = processor.createQuery(Person.class, filters);
        assertNotNull("query not null", query);
        System.out.println(query);
    }

    @Test
    public void testSortAndOrder() throws Exception {
        DatabaseConfiguration configuration = new DatabaseConfiguration();
        configuration.setName("testdb");
        configuration.setBasePackages("db.juhaku.juhakudb.test.bean");
        configuration.setVersion(1);
        Schema schema = Schema.newInstance(configuration, new Class<?>[]{
                Teacher.class, Person.class, ClassRoom.class, Group.class});

        QueryProcessor processor = new QueryProcessor(schema);

        Filters filters = new Filters();
        filters.add(new Filter<ClassRoom>() {
            @Override
            public void filter(Root<ClassRoom> root, Predicates predicates) {
                root.join("persons", "p", JoinMode.INNER_JOIN);
                predicates.setPage(2).setPageSize(10).sort(Order.DESC, "c.name", "c._id").sort(Order.ASC, "p._id");
            }
        });

        Query query = processor.createQuery(ClassRoom.class, filters);
        assertNotNull("query not null", query);
        System.out.println(query);
    }
}
