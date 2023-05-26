# Bahmni Reports

Hosts the reports web application for the [Bahmni project](http://www.bahmni.org/)

[![Build Status](https://travis-ci.org/Bahmni/bahmni-reports.svg?branch=master)](https://travis-ci.org/Bahmni/bahmni-reports)

### Prerequisite
1. JDK 1.8


### Installing the application

1. Clone or download this repository.

2. Run `./mvnw clean install -DskipTests` to build it

3. Deploy the WAR file in `target/bahmnireports.war`

### Setting up the Bahmni-reports for development


**Steps to manually upload the bahmnireports.war file to your docker container**

Copy the generated bahmnireports.war to your reports container
1. `docker cp <your-bahmnireports-location>/bahmni-reports/target/bahmnireports.war bahmni-docker-reports-1:/etc/bahmni-reports/`

Login to your container
2. `docker exec -it bahmni-docker-reports-1 sh`

Navigate to the bahmnireports.war location and extract the WAR file
3. `cd /var/run/bahmni-reports/bahmni-reports && jar xvf /etc/bahmni-reports/bahmnireports.war`

Exit and restart your reports container
4. `exit`
5. `docker restart bahmni-docker-reports-1`

# Running Integration tests

   1. Install MySQL client and server in your machine. If you already have a MySQL server available make sure that the user has the privileges to dump the database.
   2. Run: `./mvnw -DskipDump=false -DskipConfig=false clean package` (note this would trigger `scripts/create_configuration.sh` as part of test-compile and create respective test properties under `$HOME/.bahmni-reports/bahmni-reports-test.properties`. You can also explicitly run `scripts/create_configuration.sh` to create the properties (incase if you are using IDE to run the test)
   3. This should trigger all the tests including integration (it assumes jdbc:mysql://localhost:3306/reports_integration_tests as the DB URL)

**Note:**

OpenMRS 2.1.6 and its corresponding schema dump is on MySql 5.6(**preferred**). There are breaking changes between 5.6 and 5.7

E.g. only_full_group_by is enabled by default 

We are mutating global and session sql_mode as workaround to make 5.7 almost similar to 5.6. For reference check github action workflow. 

