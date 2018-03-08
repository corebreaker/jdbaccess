# JDBAccess

Little ORM for Java


# Informations

This project use **Apache Ant** as builder.

_JDBAccess_ need stub class and interface that represents DB mapped objects.

Stub class is called an _Entity Class_ and stub interface is called a _Model_
and theses 2 kinds of Java types have to be placed in 2 distinct packages
but brother packages.

_JDBAccess_ uses `CachedRowSet`. But oddly, the default implementation provided in JDK8 doesn' work.
So, an implementation has been tinkered from implementation found in JDK7.

An example has been included in this project and can be found in `example` directory.
The example works only on a MySQL database (or MariaDB).

Before running the example, you must execute SQL file `example/sql/createdb.sql`,
for example by the command:
```shell
mysql <example/sql/createdb.sql
```

Then, to execute the example, do this:
```shell
ant example
```


# Features

- Object Mapping
- Foreign Keys
- Transactions


# Commands

## Build the JAR file

```shell
ant
```

The JAR file will be in `dist` directory.

## Run the example

```shell
ant example
```
