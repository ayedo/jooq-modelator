Jooq-Modelator
==============

## Overview

This gradle plugin generates Jooq metamodel classes from Flyway & Liquibase migration files. It does so by running them against a dockerized database, and then running the Jooq generator against that database.

It solves the following problems:

- No need to maintain and run local databases for builds
- You can run migrations against your very own RDMS, as opposed to an in memory H2 in compatibility mode like other plugins do
- The plugin is incremental end-to-end, and only runs if the migrations have changed, or the target metamodel folder has changed

## Requirements

You need to have Docker installed.

The plugin has been tested with Version 18.06.1-ce-mac73 (26764).

## Supported Technologies

Two migration engines are supported:

- Flyway (version '5.1.4')
- Liquibase (version '3.6.2')

Flyway is fully supported.

__For Liquibase there are limitations:__

- You cannot chose the name of your database change log. __It has to be named 'databaseChangeLog'__. The file ending does not matter, and can be any of the supported file types.
- All migration files need be located within the configured migrations folder (see section 'Configuration').

All databases which you can run in a Docker container, and for which a JDBC driver can be provided, are supported. The plugin has been successfully tested with Postgres 9.5, and MariaDB 10.2.

Due to backwards incompatible changes in the API, __no jooq Generator version older than 3.11.0 is currently supported__.

## Installation

Add the following to your *build.gradle* plugin configuration block:

    plugins {
        id 'ch.ayedo.jooqmodelator:1.0.0'
    }

## Configuration

To configure the plugin you need to add two things:

- A 'jooqModelator' plugin configuration extension (subsection "Plugin Configuration")
- A 'jooqModelatorRuntime' configuration in the dependencies for your database driver (subsection "Database Configuration") 

### Plugin Configuration


Add the following to your build script:


    jooqModelator {
    
        // JOOQ RELATED CONFIGURATION
        
        // The version of the jooq-generator that should be used
        // The dependency is added, and loaded dynamically.
        // Only versions 3.11.0 and later are supported.
        jooqVersion = '3.11.4' // required, this is an example
        
        // Which edition of the jooq-generator to be used.
        // Possible values are: "OSS", "PRO"  "PRO_JAVA_6" "TRIAL"
        jooqEdition = 'OSS' // required, this is an example
    
        // The path to the XML jooq configuration file
        jooqConfigPath = '/var/folders/temp/jooqConfig.xml' // required, this is an example
    
        // MIGRATION ENGINE RELATED CONFIGURATION
        
        // Which migration engine to use. 
        // Possible values are: 'FLYWAY', 'LIQUIBASE'
        migrationEngine = 'FLYWAY' // required, this is an example
            
        // The path to the folder containing the migration files
        migrationsPath = '/var/folders/temp/migrations' // required, this is an example
    
        // DOCKER RELATED CONFIGURATION
        
        // The tag of the image that will be pulled, and used to create the db container
        dockerTag = 'postgres:9.5' // required, this is an example
    
        // The environment variables to be passed to the docker container
        dockerEnv = ['POSTGRES_DB=postgres', 'POSTGRES_USER=postgres', 'POSTGRES_PASSWORD=secret'] // required, this is an example
    
        // The container port bindings to use on host and container respectively
        dockerHostPort = 5432 // required, this is an example
    
        dockerContainerPort = 5432 // required, this is an example
    
        // The plugin labels the containers it creates, and uses
        // the following labelkey as key to do so.
        // Should normally be left to the default
        labelKey = 'ch.ayedo.jooqmodelator.tag' // Not required. This is the default
    
        // HEALTH CHECK RELATED CONFIGURATION
        
        // How long to wait in between failed retries. In milliseconds.
        delayMs = 500 // Not required This is the default
    
        // How long to maximally wait for the database to react to the healthcheck. In milliseconds.
        maxDurationMs = 20000 // Not required. This is the default
    
        // The SQL statement to send to the database server as part of the health check.
        sql = 'SELECT 1' // Not required. This is the default
    }

### Database Configuration

You add your database drivers as follows:

    dependencies {
        // Add your JDBC drivers, and generator extensions here
        jooqModelatorRuntime('org.postgresql:postgresql:42.2.4')
    }

## Usage

The plugins adds a task named *generateJooqMetamodel* to your build.
Use it to generate the Jooq metamodel.

    ./gradlew generateJooqMetamodel
