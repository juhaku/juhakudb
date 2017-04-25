# JuhakuDB current release: 1.3.1
Spring DATA like Android ORM Library for SQLite dabaseses

## Introduction
JuhakuDb is created to provide advanced database management with simplicity in mind as well. This libary implements 
Spring DATA and Hibernate like API for database management. Key features are annotation based ORM handling as well 
as filter based criteria API supporting nomal SQL as well in case of necessarity. 

This libaray supporst automatic database creation, schema updates as well as rollbacks. With easy to use and fast to 
configure API it gives you levarage for managing databases in Android devices. 

With JuhakuDb managing SQLite database is fast and simply and it is quick to adapt on using the library. Libarary also provides annotated repositories with basic CRUD functionalities.

### ORM handling
Annotation based ORM handling reminds a lot Hibernate. If Hibernate is something you are already familiar with 
this should not be a too much to take then. There are similar annotations like ManyToOne, ManyToMany, OneToOne, OneToMany,
Entiy, Id, Column etc. and they work in similar way to Hibernite.

Annotated classes will be mapped accordingly and database tables will be automatically created with relations by the 
annotations.

### Filter based criteria API
Filter based criteria API is fairly similar to Spring DATA's representation of criteria API but in comparizon this is a lot simplier and easier to use. It provides easy way to create join queries from multiple tables with easy to use filter chain syntax. This syntax allows you to create complex queries wihtout writing single line of sql.

## Installation

Currently available from central repository.

### Maven

```xml
<dependency>
    <groupId>io.github.juhaku</groupId>
    <artifactId>juhaku-db</artifactId>
    <version>1.3.2</version>
    <type>aar</type>
</dependency>
```

### Gradle

```java
    compile 'io.github.juhaku:juhaku-db:1.3.2@aar'
```

## Usage
* Database manager
* Annotations
* Repositories
* Filter criteria API

### Database manager
With following snippet you can create database manager. This is the core of the JuhakuDb which provides you configuration possibilities for database. Initializing of this class will automatically create your database by the given configurations.
This class should be initialized in super class or in application level and never should be initialized more than once in same runtime context for avoiding unwanted behaviour.
```java
DatabaseManager dbManager = new DatabaseManager(context, new DatabaseConfigurationAdapter() {
    @Override
    public void configure(DatabaseConfiguration configuration) {
        configuration.getBuilder().setBasePackages("db.juhaku.dbdemo.model", "db.juhaku.dbdemo.bean")
                .setVersion(1) // Updating this version will cause database to update.
                .setName("dbtest.db")
                .setMode(SchemaCreationMode.UPDATE)
                .setAllowRollback(false)
                .setRollbackHistorySize(5) // Number of back-ups allowed to take from database schema.
                .setEnableAutoInject(true) // This will enable annotation based repository injection.
                .setBaseRepositoryClass(CustomCoreRepository.class); // Custom base class for repositories.
    }
});
```
Context here is a Android Context. Other attributes are quite self explanatory. They are documented so reading java docs will give you more knowledge on them as well.

### Annotations
Currently available annotation.

|Annotation| Description|
|------------------------------| --- |
|Column| Define column name for class attribute, can only be added to non-relation attribute.|
|Entity| Define table name for class.|
|Id| Define primary key field for database table, id is auto generated per table (way of the anroid). Only numeric value can be id currently. Id annotated column will map to "_id" column in database. This is default to Android.|
|ManyToMany| Defines many to many relation.|
|ManyToOne| Defines many to one relation.|
|OneToMany| Defines one to many relation.|
|OneToOne| Defines one to one relation.|
|Repository| Marks interface as repository.|
|Transient| Marks class attribute as transient which will not be saved to database.|
|Inject| Marks class attribute as injectable for automatic repository injection. Type of the attribute must be a repository annotated with Repository annotation.|

All operations are cascading and storing will return stored item with populated database id.
Fetch can be either EAGER or LAZY. This is defined by Fetch enum that can be provided as attribute for relation annotations. Lazy will not load relation from database and it need to be manually loaded. EAGER will automatically fetch referenced relation from database along with original item. However using EAGER is not recommended behauviour.

Annotated table examples with minimal annnotation configuration.
```java
@Entity
public class Class {

    @Id
    private Long id;

    private String name;

    @ManyToOne
    private Person person;

    @OneToOne
    private Teacher teacher;
    
    // ... getters and setters
}

@Entity
public class Book {

    @Id
    private Long id;

    private String name;

    @ManyToMany
    private List<Person> persons;

    @ManyToMany
    private List<Library> libraries;
    
    // ... getters and setters   
}

@Entity
public class Person {

    @Id
    private Long id;

    private String name;

    private String nickName;

    @ManyToMany
    private List<Book> books;

    @OneToMany
    private List<Class> classes;
    
    // ... getters and setters   
}
```

Classes can be defined with or without name in @Entity annotation and with or without @Column annotation. If @Column annotation and name in @Enity annotation is not defined name for database column and table is still resolved.

Classes can also define fetch in the relation annotation. However fetch is by default LAZY. Cascade option in relation annotation is deprecated and there is no use to use that as all store operations and delete operations are cascading by default. Just to make your life easier without calling different repositories for simple delete operation or store operation.

Relations still need to be defined in both sides of tables.

Annotated class example with more configuration. Compared to above there is name in @Entity annotation and @Column annotations added.
```java
@Entity(name = "authority")
public class Authority {

    @Id
    private Long id;

    @Column(name = "value")
    private String value;

    @ManyToMany(fetch = Fetch.LAZY)
    private List<Person> person;
    
    // ... getters and setters
}
```

### Repositories

There are 3 ways to work with repositories since version 1.3.0. All repositories must be annotated with @Repository annotation. If value is not provided the default implementation is being used. If value is provided inside the @Repository annotation then that is being used as implementation for the repository interface.

As shown below currently the by writing following snippet is enough to create instance of a repository. Absolutely no implementation of repositories is required as long as de default functionality is enough.
```java
@Repository
public interface PersonRepository extends SimpleRepository<Long, Person> {}
```

#### Default implementation

Default implementation has implementation for following methods. Javadocs omitted from example.
```java
public interface SimpleRepository<K, T> {

    T store(T object);

    List<T> storeAll(Collection<T> objects);

    int remove(K id);

    int removeAll(Collection<K> ids);

    T findOne(final K id);

    T findOne(Filter<T> filter);

    List<T> findAll();

    List<T> find(Filter<T> filter);

    <E> E find(Query query, ResultTransformer<E> resultTransformer);

}
```

#### Custom base repository

However if default functionality is not enough you are able to create custom base repository as well. This is particulary useful if custom behiour is required for all the repositories. Following steps will guide you through how to create a custom base repository that is being used as base for all repositories except those with own implemenation.

Create an interface called custom cre repo. It could be anything you wish but the important part is to extend simple repository. That gives you the default functionalities available as well. There is additional method called exists. This method will be available to all repositories that are using the default repository.
```java
public interface CustomCoreRepo<K, T> extends SimpleRepository<K, T> {
    boolean exists(K id);
}
```

Now create implementation for the custom core repository. We extend the default repository called simple android repository. And we implement the new functionality from the new custom core repo. Find one is function in the default simple android repository and K stands for key wich is Id class of the entity and T stands for the entity class itself.
```java
public class CustomCoreRepository<K, T> extends SimpleAndroidRepository<K, T> implements CustomCoreRepo<K, T> {

    public CustomCoreRepository(EntityManager entityManager, Class<T> persistentClass) {
        super(entityManager, persistentClass);
    }

    @Override
    public boolean exists(K id) {
        Log.d(getClass().getName(), "checking does item exists");
        return findOne(id) != null;
    }
}
```

To this point we have created custom core repository. In order to use it we just need to do 2 things.

First we create a repository that is using the custom core repo we created as following. As told previously this is enough to create a repository instance. But the difference is that we are extending custom core repo instead of simple repository.
```java
@Repository
public interface PermissionRepository extends CustomCoreRepo<Long, Permission> {}
```

Then we tell database manager to use the cusom core repository instead of the simple android repository as the base of repositories by adding this line to database configuration adapter. Also shown above in database manager section.
```java
.setBaseRepositoryClass(CustomCoreRepository.class); // Custom base class for repositories.
```

#### Old school repository

The third option is little old school so to say. This can be useful if totally custom repository is required. 

First we create a repository interface with @Repository annotation. The main difference is that we provide implementing class as attribute inside @Repository annotation and we do not extend any repository class.
```java
@Repository(BookRepositoryImpl.class)
public interface BookRepository {
    Long storeBook(Book book);
    List<Book> findBooks(Filters filters);
}
```

Then we provide the implementation for the repository interface like following. We implement the bok repository and extend the default simple android repository.
```java
public class BookRepositoryImpl extends SimpleAndroidRepository<Long, Book> implements BookRepository {

    public BookRepositoryImpl(EntityManager em) {
        super(em);
    }
    
    @Override
    public Long storeBook(Book book) {
        return store(book);
    }
    
    @Override
    public List<Book> findBooks(Filters filters) {
        return find(filters);
    }
}
```

#### Get instance of repository in application

If you have access to database manager you can always get an instance by simple calling getRepository method with the repository interface to get instance of repository inside your application.
```java
repository = dbManager.getRepository(BookRepository.class);
```

More robust solution is to use @Inject annotation. To enable this feature we need to add following configuration to database configuration adapter. This will enable this feature.
```java
.setEnableAutoInject(true); // this will enable annotation based repository injection 
```

Then we need to call explisitly following method in the class that the repository injection should be initiated. This method can exists in super class of the class stack as well. It still will go through the class stack and inject all repositories that is marked with @Inject annotation.

This behaviour is necessary as Android does not have default functionality to provide stack of running classes or to follow opening fragments or other classes. Only opening activites can be retrieved. So we cannot execute this process at the background. But good thing is that you can hide this at the top of your activity stack or fragment stack.
```java
getDbManager().lookupRepositories(this); // this will initiate repository lookup injection.

// ... then later in code class stack we can add @Inject annotation to any repository.
@Inject
private PersonRepository personRepository;
```

##### The quick quide of annotation based repository injection comes as follows. 

1. There should be only one database manager in super level of your Android application.
2. Call lookupRepositories(this) in Activity or Fragment or inside ohter object that repositories is wished to be injected and is accessible to DatabaseManager. This method call should appear in super level of your component hierarcy.
3. Use Inject annotation in any child or parent component where lookupRepositories(this) is already called.


### Filter criteria API

If joins or predicates are prefixed with "this" or left without prefix it will be mapped to the root entity.
```java
root.join("this.rooms", JoinMode.LEFT_JOIN)
predicates.add(Predicate.in("this.name", "john", "kimmo")).add(Predicate.not(Predicate.eq("name", "kim")));
```

In predicate using .id or ._id as colum name is mapped as to be equal and both will refer to primary key column. In Adroid primary key column is mapped to "_id" column thus we made this convention for cleaner code.
```java
predicates.add(Predicate.eq("this.id", "1"));
predicates.add(Predicate.eq("this._id", "1"));
```

You can also create custom queries by wrinting normal sql and then use result trasformer to trasform custom result. 
```java
Query query = new Query("select persons_id, books_id from books_persons", null);
return find(query, new ResultTransformer<List<String>>() {
    @Override
    public List<String> transformResult(List<ResultSet> resultSets) {
        List<String> strings = new ArrayList<>();
        for (ResultSet res : resultSets) {
            String val = "";
            for (Result result : res.getResults()) {
                val = val.concat(result.getColumnName()).concat("=").concat(result.getColumnValue().toString());
            }
            strings.add(val);
        }

        return strings;
    }
});
```

Joins work little different from version 1.2.0 forward. Method join will return new root to the join target. E.g. in example below the first join will return root to rooms object and the second will return teacher root from rooms.
So be careful when creating joins.

Below are couple of more advanced examples.
```java
Filters filters = new Filters();
filters.add(new Filter<Person>() {
    @Override
    public void filter(Root<Person> root, Predicates predicates) {
        root.join("this.rooms", "r", JoinMode.LEFT_JOIN).join("r.teacher", "t", JoinMode.FULL_JOIN);
        root.join("this.groups", "g", JoinMode.INNER_JOIN);

        predicates.add(Predicate.in("this.name", "matti", "kimmo"))
            .add(Predicate.not(Predicate.eq("name", "lauri")));

        Disjunction or = Predicate.disjunction();
        or.add(Predicate.eq("t.name", "laura")).add(Predicate.eq("t.name", "minna"));
        predicates.add(or);

        predicates.add(Predicate.conjunction().add(Predicate.between("t.id", 1, 3))
                .add(Predicate.not(Predicate.isNull("t.name"))));

        predicates.sort(Order.ASC, "p.name");
        predicates.setPageSize(20).setPage(1);
    }
});
```

```java
Filters filters = new Filters();
filters.add(new Filter<ClassRoom>() {
    @Override
    public void filter(Root<ClassRoom> root, Predicates predicates) {
        root.join("persons", "p", JoinMode.INNER_JOIN);
        predicates.setPage(2).setPageSize(10).sort(Order.DESC, "c.name", "c._id").sort(Order.ASC, "p._id");
    }
});
```

Joins can also be chained to Filters object like this from version 1.2.0 forward. So there is no need to call add method. It can be a personal decision whether to add multipler filters or use just one. The end result is same.
```java
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
```

Below is an example of new function since 1.2.0. It is fetch method in root object. It is a same as join but it will fetch the objects from the database along with the original objects. So there is no need to create additional query to fetch related objects manually nor use the EAGER fetch mode in relation annotations. 

As you might have noted you can specify alias for join and as well as fetch method. This is an optional functionality 
and thus no necessary to provide. If alias is not provided it will be generated automatically. However if you use alias
you can refer to object in queries with the alias. So without alias you cannot refer to the table in queries and you cannot add predicates for the table.
```java
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
```

Yet another invented real case scenario example.
```java
List<Library> libraries = libraryRepository.findLibraries(new Filters(new Filter() {
    @Override
    public void filter(Root root, Predicates predicates) {
        root.fetch("books", JoinMode.INNER_JOIN).fetch("persons", "bp", JoinMode.INNER_JOIN);
        root.fetch("roles", JoinMode.LEFT_JOIN).fetch("person", JoinMode.LEFT_JOIN);
    }
}, new Filter() {
    @Override
    public void filter(Root root, Predicates predicates) {
        predicates.add(Predicate.not(Predicate.isNull("this.name")));
    }
}));
```

Now begin to use the library that rocks the Android's SQLite database.

## Roadmap

### 2.x release

Own ORM annotations will be replaced with javax.persistence annotations in favor of standardized usages.

### 3.x release

Added support for stand-alone SQLite database as well.

# Terms and conditions MIT
Copyright 2017 juhaku

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
