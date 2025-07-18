Moovies Deployment Guide
============================================

System Requirements
-------------------
- Java 11
- MySQL 8
- Tomcat 10
- Apache2 (for production load balancing)

Installation Instructions
-------------------------

1. Java 11 Installation
-----------------------
Production:
sudo apt update
sudo apt install default-jdk

Development:
- Install Java 11 SDK for your platform
- Set JAVA_HOME environment variable

2. MySQL 8 Installation
-----------------------
Production:
sudo apt update
sudo apt install mysql-server

Development:
- Install MySQL 8.0
- Enable logging:
  mysql -u root -p
  SET GLOBAL general_log = 'ON';
  exit

Log Monitoring:
sudo tail -f /var/log/mysql/mysql.log

3. Tomcat 10 Installation
-------------------------
Production:
sudo apt update
sudo apt install tomcat10 tomcat10-admin

Development:
- Install Tomcat 10
- Monitor logs:
  Linux: sudo tail -f /var/lib/tomcat/logs/*
  Windows: tail -f "C:\\Path\\To\\Tomcat\\logs\\*"

Tomcat Configuration (Production)
---------------------------------
1. Open firewall port:
   sudo ufw allow 8080/tcp

2. Configure admin user:
   Edit /etc/tomcat10/tomcat-users.xml:
   <role rolename="manager-gui"/>
   <user username="(user created)" password="(user created)" roles="manager-gui"/>

3. Restart Tomcat:
   sudo systemctl restart tomcat10

Database Setup
--------------
Schema:
- Database name: moviedb
- Required tables:
  * movies (id, title, year, director)
  * stars (id, name, birthYear)
  * stars_in_movies (starId, movieId)
  * genres (id, name)
  * genres_in_movies (genreId, movieId)
  * customers (id, firstName, lastName, ccId, address, email, password)
  * sales (id, customerId, movieId, saleDate)
  * creditcards (id, firstName, lastName, expiration)
  * ratings (movieId, rating, numVotes)

Initialization:
Run these SQL files in order:
1. create_table.sql
2. stored-procedure.sql
3. create_index.sql

Application Deployment
----------------------
1. Build:
   mvn package

2. Deploy:
   cp ./target/*.war /var/lib/tomcat/webapps/ (Production)
   or copy to your development Tomcat webapps directory

Load Balancer Setup (Production)
--------------------------------
1. Install Apache2
2. Configure:
   - Load balancing
   - Connection pooling
   - Sticky sessions

Credentials Summary
-------------------
Tomcat:
- Username: (user created)
- Password: (user created)

MySQL:
- Username: (user created)
- Password: (user created)
