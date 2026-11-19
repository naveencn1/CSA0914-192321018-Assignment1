#!/bin/sh
# Compile and run every task. Requires: JDK 17+, MySQL/MariaDB running locally, JDBC driver jar path in $JDBC.
# Create the test user once:  CREATE USER 'csa09'@'localhost' IDENTIFIED BY 'csa09pass'; GRANT ALL ON csa09_assignment5.* TO 'csa09'@'localhost';
JDBC=${JDBC:-/usr/share/java/mariadb-java-client.jar}
set -e
mysql -u root < Task02/sql/01_create_login_db.sql
mysql -u root < Task03/sql/01_create_bus_pass.sql
mysql -u root < Task04/sql/01_create_migration_tables.sql
( cd Task01 && mkdir -p classes && javac -d classes src/*.java && java -cp classes Task01Main && java -Xmx1g -cp classes Bench01 )
( cd Task02 && mkdir -p classes && javac -cp ../Task01/classes:$JDBC -d classes src/*.java && java -cp classes:../Task01/classes:$JDBC LoginDemo && java -cp classes:../Task01/classes DamagedDataTest )
( cd Task03 && mkdir -p classes && javac -cp ../Task01/classes:$JDBC -d classes src/*.java && java -cp classes:../Task01/classes:$JDBC Task03Main && java -cp classes:../Task01/classes:$JDBC Bench03 )
( cd Task04 && mkdir -p classes && CP=classes:../Task01/classes:../Task03/classes:$JDBC && javac -cp ../Task01/classes:../Task03/classes:$JDBC -d classes src/*.java \
  && java -cp $CP MigrationTool data/sample_migration_input.txt results/sample_rejects.txt \
  && java -cp $CP GenerateInput 100000 500 data/migration_100k.txt && java -cp $CP MigrationTool data/migration_100k.txt results/rejects_100k.txt \
  && java -Xmx1800m -cp $CP DecisionBench )
