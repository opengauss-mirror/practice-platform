# practice-platform

#### Introduction
practice-platform is a teaching-focused database practice software built on openGauss. It aims to solve these three issues:

1. Complex openGauss installation and usage: Installing and using openGauss is easier with this platform. Users can access openGauss directly on a web page, skipping complex script installations. This lets students focus on SQL programming. The platform also supports self-learning and exercises, making learning more student-centered and less teacher-dependent.
2. Accessing openGauss using SQL statements: University courses teach basic database theory. Some faculties may teach how to access a relational database via GUI, but students often learn this on their own because it's easy. This teaching platform converts most database object access methods into SQL statements, guiding students to focus on SQL statements rather than GUI operations.
3. Automatic evaluation of SQL statements: Teachers can set database tasks using this platform for data access (SELECT, INSERT, UPDATE, DELETE), tables (CREATE TABLE, ALTER TABLE), views (CREATE VIEW), procedures (CREATE PROCEDURE), functions (CREATE FUNCTION), indexes (CREATE INDEX), and security (CREATE USER, CREATE ROLE, GRANT, REVOKE). Students can see tests and questions and submit SQL statement answers. The platform checks these answers automatically. This relieves teachers, boosts student interest in openGauss, and facilitates students in future studies and research. 

#### Software Architecture
This software consists of the following four modules:

* Practice: provides a GUI for teachers and common users to access the openGauss database.
* Capability assessment: provides an environment for teachers and common users to answer and assess SQL statement questions.
* Teaching management: allows teachers to manage students, classes, exams, and questions.
* Administrator: allows administrators to manage teachers and system database connections.

The software comprises the frontend, backend, and database.

* Frontend: developed using Vue to provide a UI.
* Backend: developed using Spring Boot to process service logic.
* Database: developed using openGauss 2.1.0, responsible for data storage and automatic evaluation of SQL statements.


#### Installation

1.  Build and package the frontend code.
2.  Create a `vue` folder in the `src\main\resources\static` directory of the backend code.
3.  Decompress the `css`, `fonts`, `img`, `js` folders, the `favicon.ico` file, and the `index.html` file from the packaging result to the `src\main\resources\static\vue` directory of the backend code.
4.  Pack the backend code into a JAR file.
5.  Perform subsequent steps according to the installation guide in the document.

#### Instruction

See the common user guide, teacher guide, and administrator guide in the documentation.
