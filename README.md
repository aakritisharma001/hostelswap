# HostelSwap (Spring Boot version)

No Tomcat, no MySQL, no passwords to set. The database is a file (H2) that is created automatically.

## Run

1. Install JDK 17 or newer and Maven (you already have both).
2. Open a terminal in this folder (the one that contains pom.xml) and run:

       mvn spring-boot:run

3. Wait until you see "Started HostelSwapApplication".
4. Open http://localhost:8080 in your browser.

Stop the app with Ctrl+C.

## Admin account

Open src/main/resources/application.properties and set hostelswap.admin-email to your own
@thapar.edu address before you register. The account registered with that email becomes admin.

## Notes

- Data is saved in ./data/hostelswap.mv.db. Delete that file to start with an empty database.
- Uploaded photos are saved in ./uploads.
