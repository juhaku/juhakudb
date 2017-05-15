# JuhakuDB current release: 2.1.0
Spring DATA like Android ORM Library for SQLite dabaseses

Previous release: 1.3.2 docs can be found: [1.3.2 Release](https://github.com/juhaku/juhakudb/tree/b1.3.x). 
1.x will not get any new feature releases, but only patches and bug fixes.

## Introduction
JuhakuDb is created to provide advanced database management with simplicity in mind as well. This library implements 
Spring DATA and Hibernate like API for database management. Key features are annotation based ORM handling as well 
as filter based criteria API supporting normal SQL as well in case of necessarity. 

This library supports automatic database creation, schema updates as well as rollbacks. With easy to use and fast to 
configure API it gives you levarage for managing databases in Android devices. 

With JuhakuDb managing SQLite database is fast and simply and it is quick to adapt on using the library. 
Library also provides annotated repositories with basic CRUD functionalities.

### ORM handling
Annotation based ORM handling works with same javax persistence annotations as Hibernate would work. 
If Hibernate is something you are already familiar with this should not be a too much to take. 
Currently supported javax persistence annotations are ManyToOne, ManyToMany, OneToOne, OneToMany, Entity, Id, 
Column and Transient.

Annotated classes will be mapped accordingly and database tables will be automatically created with 
relations by the annotations.

### Filter based criteria API
Filter based criteria API is fairly similar to Spring DATA's representation of criteria API but in 
comparison this is a lot simpler and easier to use. It provides easy way to create join queries from 
multiple tables with easy to use filter chain syntax. This syntax allows you to create complex queries 
without writing single line of sql.

## Installation

Currently available from central repository.

### Maven

```xml
<dependency>
    <groupId>io.github.juhaku</groupId>
    <artifactId>juhaku-db</artifactId>
    <version>2.1.0</version>
    <type>aar</type>
</dependency>
```

### Gradle

```java
    compile ('io.github.juhaku:juhaku-db:2.1.0@aar') {
        transitive = true
    }
```

It is crusial to set transitive to true so dependant javax persistence annotations will be loaded as well. 
If not added you need to manually add the annotation dependency.

## Usage
* Database manager
* Annotations
* Repositories
* Filter criteria API

### Database manager
With following snippet you can create database manager. This is the core of the JuhakuDb which provides you 
configuration possibilities for database. Initializing of this class will automatically create your database 
by the given configurations. This class should be initialized in super class or in application level and 
never should be initialized more 
than once in same runtime context for avoiding unwanted behaviour.
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
Context here is a Android Context. Other attributes are quite self explanatory. They are documented so 
reading javadocs will give you more knowledge on them as well.

### Annotations
Currently available javax persistence annotation.

|Annotation| Supported attributes| Description| 
|------------------------------|---| --- |
|Table| name, indexes, uniqueConstraints | Define table name for class. Not required as name can be resolved from class itself. Indexes as well as unique constraints contain list of constraints for the table.|
|Index| name, columnList | Defines index for given column list in table. Name is name of the index constraint.|
|UniqueConstraint| name, columnNames | Defines unique constraint index for given list of columns in table. Name is name of the constraint.|
|Column|name | Define column name for class attribute, can only be added to non-relation field.|
|Entity|-- | Marks class as entity of database.|
|Id|-- | Define primary key field for database table, id is auto generated per table (way of the Android). Only numeric value can be id currently. Id annotated column will map to "_id" column in database. This is default to Android.|
|ManyToMany|fetch | Defines many to many relation. Fetch attribute can have value FetchType.LAZY or FetchType.EAGER. Default: FetchType.LAZY.|
|ManyToOne| fetch | Defines many to one relation. Fetch attribute can have value FetchType.LAZY or FetchType.EAGER. Default: FetchType.EAGER.|
|OneToMany| fetch | Defines one to many relation. Fetch attribute can have value FetchType.LAZY or FetchType.EAGER. Default: FetchType.LAZY.|
|OneToOne| fetch | Defines one to one relation. Fetch attribute can have value FetchType.LAZY or FetchType.EAGER. Default: FetchType.EAGER.|
|Transient| -- | Marks class field as transient which will not be saved to database.|

All operations are cascading and storing will return stored item with populated database id.
Fetch can be either EAGER or LAZY. This is defined by FetchType enum that can be provided as attribute for 
relation annotations. Lazy will not load relation from database and it need to be manually loaded. EAGER 
will automatically fetch referenced relation from database along with original item. However using EAGER 
is not recommended behaviour.

Currently available own annotations.

|Annotation| Supported attributes| Description|
|------------------------------|---| --- |
|Repository| value | Marks interface as repository. Value can be implementing class of repository.|
|Inject| -- | Marks class field as injectable for automatic repository injection. Type of the field must be a repository annotated with Repository annotation.|


Annotated table examples with minimal annotation configuration.
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

Database table names are resolved from the class name if class has @Entity annotation. Table name can 
also be resolved from @Table annotation if @Table annotation has name attribute provided. Column names 
are defined by the @Column annotation's name attribute. If name attribute is not defined name will be 
resolved from field's name.

Table annotation can contain along with name information array of indexes and unique constraints needed
to add to the table. Below is an example of how to configure them as well.

Classes can also define fetch in the relation annotation. All store operations and delete operations 
are cascading by default. Just to make your life easier without calling different repositories for 
simple delete operation or store operation.

Relations still need to be defined in both sides of tables.

Annotated class example with more configuration. 
```java
@Entity
@Table(name = "authorities", indexes = {@Index(name = "value_idx", columnList = "value,name"), @Index(name = "level_idx", columnList = "level", unique = true)},
uniqueConstraints = {@UniqueConstraint(name = "unique_permission_ctx", columnNames = {"permission"})})
public class Authority {

    @Id
    private Long id;

    @Column(name = "value")
    private String value;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "level")
    private Integer level;
    
    @Column(name = "permission")
    private String permission;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<Person> person;
    
    // ... getters and setters
}
```

### Repositories

There are 3 ways to work with repositories since version 1.3.0. All repositories must be annotated 
with @Repository annotation. If value is not provided the default implementation is being used. If 
value is provided inside the @Repository annotation then that is being used as implementing class 
for the repository interface.

As shown below currently by writing following snippet is enough to create instance of a repository. 
Absolutely no implementation is required as long as de default functionality is enough.
```java
@Repository
public interface PersonRepository extends SimpleRepository<Long, Person> {}
```

#### Default implementation

By default there is a implementation for following methods. Javadocs omitted from example.
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

However if default functionality is not enough you are able to create custom base repository as well. 
This is particularly useful if custom behaviour is required for all the repositories. Following steps 
will guide you through how to create a custom base repository that is being used as base for all 
repositories except those with own implementation.

Create an interface called custom core repo. It could be anything you wish but the important part is 
to extend simple repository. That gives you the default functionalities available in simple repository. 
There is additional method called exists. This method will be available to all repositories that are using 
the default repository.
```java
public interface CustomCoreRepo<K, T> extends SimpleRepository<K, T> {
    boolean exists(K id);
}
```

Now create implementation for the custom core repository. We extend the default repository called 
simple android repository. We implement the new functionality from the new custom core repo. Find 
one is function in the default simple android repository and K stands for key which is Id class of 
the entity and T stands for the entity class itself.
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

First we create a repository that is using the custom core repo we created. As told previously this is 
enough to create a repository instance. But the difference is that we are extending custom core repo 
instead of simple repository.
```java
@Repository
public interface PermissionRepository extends CustomCoreRepo<Long, Permission> {}
```

Then we tell database manager to use the cusom core repository instead of the simple android repository 
as the base of repositories by adding this line to database configuration adapter. Also shown above in 
database manager section.
```java
.setBaseRepositoryClass(CustomCoreRepository.class); // Custom base class for repositories.
```

#### Old school repository

The third option is little bit old school. This can be useful if totally custom repository is required. 

First we create a repository interface with @Repository annotation. The main difference is that we provide 
implementing class as attribute inside @Repository annotation and we do not extend any repository class.
```java
@Repository(BookRepositoryImpl.class)
public interface BookRepository {
    Long storeBook(Book book);
    List<Book> findBooks(Filters filters);
}
```

Then we provide the implementation for the repository interface as follows. We implement the book repository 
and extend the default simple android repository.
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

If you have access to database manager you can always get an instance of repository by simple calling 
getRepository method with the repository interface from database manager to your class.
```java
repository = dbManager.getRepository(BookRepository.class);
```

More robust solution is to use @Inject annotation. To enable this feature we need to add following 
configuration to database configuration adapter. Following will enable the feature.
```java
.setEnableAutoInject(true); // this will enable annotation based repository injection 
```

Then we need to call explisitly following method in the class that the repository injection should be 
initiated. This method can exists in super class of the class stack as well. It still will go through 
the class stack and inject all repositories that is marked with @Inject annotation.

This behaviour is necessary as Android does not have default functionality to provide stack of running 
classes or to follow opening fragments or other classes. Only opening activities can be retrieved. So we 
cannot execute this process at the background. But good thing is that you can hide this at the top of 
your activity stack or fragment stack.
```java
getDbManager().lookupRepositories(this); // this will initiate repository lookup injection.

// ... then later in code class stack we can add @Inject annotation to any repository.
@Inject
private PersonRepository personRepository;
```

##### The quick quide of annotation based repository injection comes as follows. 

1. There should be only one database manager in super level of your Android application.
2. Call lookupRepositories(this) in Activity or Fragment or inside other object that repositories is wished to be injected and is accessible to DatabaseManager. This method call should appear in super level of your component hierarchy.
3. Use Inject annotation in any child or parent component where lookupRepositories(this) is already called.


### Filter criteria API

Since 2.1.0 the old criteria api is being changed to new predicate builder criteria api. This enables
you to write more robust queries as well as SQL predicates without mentioning the size of the code. 
Since 2.1.0 you only need write half as much as previously with more expressive api.

For example compare below the statements. Above one is how it is written currently and below you can 
find old substitute.
```java
builder.in("this.name", "john", "kimmo").not().eq("name", "kim");
predicates.add(Predicate.in("this.name", "john", "kimmo")).add(Predicate.not(Predicate.eq("name", "kim")));
```

If joins or predicates are prefixed with "this" or left without prefix it will be mapped to the root entity 
of repository.
```java
root.join("this.rooms", JoinMode.LEFT_JOIN)
builder.in("this.name", "john", "kimmo").not().eq("name", "kim");
```

In predicate using .id or ._id as column name is mapped as to be equal and both will refer to primary key column. 
In Android primary key column is mapped to "_id" column thus we made this convention for cleaner code.
```java
builder.eq("this.id", "1");
builder.eq("this._id", "1");
```

You can also create custom queries by writing normal sql and then use result transformer to transform custom result. 
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

Joins work little different from version 1.2.0 forward. Method join will return new root to the join target. 
E.g. in example below the first join will return root to rooms object and the second will return teacher root 
from rooms.
So be careful when creating joins.

Below are couple of more advanced examples.
```java
Filters filters = new Filters();
filters.add(new Filter<Person>() {
    @Override
    public void filter(Root<Person> root, PredicateBuilder builder) {
        root.join("this.rooms", "r", JoinMode.LEFT_JOIN).join("r.teacher", "t", JoinMode.FULL_JOIN);
        root.join("this.groups", "g", JoinMode.INNER_JOIN);

        builder.in("this.name", "matti", "kimmo").not().eq("name", "lauri");

        Disjunction or = builder.disjunction();
        or.eq("t.name", "laura").eq("t.name", "minna");

        builder.conjunction().between("t.id", 1, 3).not().isNull("t.name");

        builder.sort(Order.ASC, "name");
        builder.setPageSize(20).setPage(1);
    }
});
```

```java
Filters filters = new Filters();
filters.add(new Filter<ClassRoom>() {
    @Override
    public void filter(Root<ClassRoom> root, PredicateBuilder builder) {
        root.join("persons", "p", JoinMode.INNER_JOIN);
        builder.setPage(2).setPageSize(10).sort(Order.DESC, "name", "_id").sort(Order.ASC, "p._id");
    }
});
```

Joins can also be chained to Filters object like this from version 1.2.0 forward. So there is no need to 
call add method. It can be a personal decision whether to add multiple filters or use just one. The end 
result is same.
```java
Filters filters = new Filters(new Filter<ClassRoom>() {
    @Override
    public void filter(Root<ClassRoom> root, PredicateBuilder builder) {
        root.join("persons", JoinMode.INNER_JOIN);
    }
}, new Filter<ClassRoom>() {
    @Override
    public void filter(Root<ClassRoom> root, PredicateBuilder builder) {
        root.join("this.teacher", "t", JoinMode.LEFT_JOIN);

        builder.ge("id", 1);
    }
}, new Filter<ClassRoom>() {
    @Override
    public void filter(Root<ClassRoom> root, PredicateBuilder builder) {
        builder.eq("t.name", "tester");
    }
});
```

Below is an example of new function since 1.2.0. It is fetch method in root object. It is a same as join 
but it will fetch the objects from the database along with the original objects. So there is no need to 
create additional query to fetch related objects manually nor use the EAGER fetch mode in relation annotations. 

As you might have noted you can specify alias for join and as well as fetch method. This is an optional 
functionality and thus no necessary to provide. If alias is not provided it will be generated automatically. 
However if you use alias you can refer to object in queries with the alias. So without alias you cannot 
refer to the table in queries and you cannot add predicates for the table.
```java
Filters filters = new Filters(new Filter<Person>() {
    @Override
    public void filter(Root<Person> root, PredicateBuilder builder) {
        root.fetch("groups", JoinMode.INNER_JOIN);
        root.fetch("rooms", "r", JoinMode.LEFT_JOIN);
    }
}, new Filter<Person>() {
    @Override
    public void filter(Root<Person> root, PredicateBuilder builder) {
        builder.eq("this.name", "tester");
    }
});
```

Yet another invented real case scenario example.
```java
List<Library> libraries = libraryRepository.findLibraries(new Filters(new Filter() {
    @Override
    public void filter(Root root, PredicateBuilder builder) {
        root.fetch("books", JoinMode.INNER_JOIN).fetch("persons", "bp", JoinMode.LEFT_JOIN);
        root.fetch("roles", JoinMode.LEFT_JOIN).fetch("person", JoinMode.LEFT_JOIN);
    }
}, new Filter() {
    @Override
    public void filter(Root root, PredicateBuilder builder) {
        builder.not().isNull("this.name");
    }
}));
```

Since 2.1.0 there is the possibility to create SQL predicates for more advanced queries. Just if 
the default functionality is not enough. See couple of hypotetical examples below.
```java
Permission p1 = permissionRepository.findOne(new Filter<Permission>() {
    @Override
    public void filter(Root<Permission> root, PredicateBuilder builder) {
        builder.sqlPredicate("value = lower(?)", "PERMISSION 1");
    }
});

Permission p2 = permissionRepository.findOne(new Filter<Permission>() {
    @Override
    public void filter(Root<Permission> root, PredicateBuilder builder) {
        builder.sqlPredicate("value = coalesce(?, ?)", "permission 2", "permission 3");
    }
});
```

Now begin to use the library that rocks the Android's SQLite database.

## Roadmap

### 3.x release

Added support for stand-alone SQLite database as well.

# Terms and conditions MIT
Copyright 2017 juhaku

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
