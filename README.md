# JuhakuDB current release: 1.0.7
Spring DATA like Android ORM Library for SQLite dabaseses

## Introduction
JuhakuDb is created to provide advanced database management with simplicity in mind as well. This libary implements 
Spring DATA and Hibernate like API for database management. Key features are annotation based ORM handling as well 
as filter based criteria API supporting nomal SQL as well in case of necessarity.

This libaray supporst automatic database creation, schema updates as well as rollbacks. With easy to use and fast to 
configure API it gives you levarage for managing databases in Adroid devices. 

### ORM handling
Annotation based ORM handling reminds a lot Hibernate. If Hibernate is something you are already familiar with 
this should not be a too much to take then. There are similar annotations like ManyToOne, ManyToMany, OneToOne, OneToMany,
Entiy, Id, Column etc. and they work in similar way to Hibernite.

Annotated classes will be mapped accordingly and database tables will be automatically created with relations by the 
annotations specified.

### Filter based criteria api



## Usage

## Roadmap

### 2.x release

Own ORM annotations will be replaced with javax.persistence annotations in favor of standardized usages.

### 3.x release

Added support for stand-alone SQLite database as well.
