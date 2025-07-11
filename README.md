/////Application để chạy web /////////////////////

spring.application.name=AsmGD1


spring.datasource.url=jdbc:sqlserver://acvstore.database.windows.net:1433;database=ACVStore;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;
spring.datasource.username=acvstore@acvstore
spring.datasource.password=@cvstore2025
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.jpa.database-platform=org.hibernate.dialect.SQLServerDialect

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
spring.jpa.defer-datasource-initialization=true
spring.jpa.generate-ddl=true
logging.level.net.ttddyy.dsproxy.listener=debug
logging.level.org.hibernate.type=trace


server.port=${PORT:8080}


spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.enabled=true
spring.thymeleaf.cache=false
spring.web.resources.static-locations=classpath:/static/

# Mail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=datn.acv@gmail.com
spring.mail.password=tmtk byjt mtrb dghp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# OAuth2 Google
spring.security.oauth2.client.registration.google.client-id=307722207866-0mmbf156lfegenhhe3d6f9c9q833hdem.apps.googleusercontent.com
spring.security.oauth2.client.registration.google.client-secret=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
spring.security.oauth2.client.registration.google.scope=profile,email

# OAuth2 Facebook
spring.security.oauth2.client.registration.facebook.client-id=1554266721939730
spring.security.oauth2.client.registration.facebook.client-secret=bf608457f76cf319e174409e37a82443
spring.security.oauth2.client.registration.facebook.scope=public_profile,email

# Cloudinary
cloudinary.cloud-name=dfwbuxr0c
cloudinary.api-key=169597231938161
cloudinary.api-secret=-rawQzc9TSHIDQnAtm2boO_GKb0


PAYOS_CLIENT_ID=358872b1-2ceb-44c7-a0cf-05716ad532ce
PAYOS_API_KEY=e36ee5ff-d648-4819-8e0e-387b0585337c
PAYOS_CHECKSUM_KEY=6abaf645b8842b1e69eae619670a2e3a028280da6d74e7248ea944f125901877

# File upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
server.tomcat.max-swallow-size=50MB

# Logging
logging.pattern.console=${LOGPATTERN_CONSOLE:%green(%d{HH:mm:ss.SSS}) %yellow(%-5level) %red([%thread]) %blue(%logger{255}) - %msg%n}
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG




///MK vps ////////////////////////////////////////////////////

1XCnmbVTf$Ip




///Lệnh de up src jar mới lên////////////////////////////////////////

scp target/AsmGD1-0.0.1-SNAPSHOT.jar root@103.221.221.208:/opt/myapp/app.jar




//////////////// Lệnh để chạy bài ngầm trên vps ///////////////////

nohup java -jar /opt/myapp/app.jar > app.log 2>&1 &





/////////////// LỆNH ĐỂ TẠO FILE JAR MỚI /////////////////////////////

./mvnw clean package -DskipTests


////////////// Lệnh để ngừng bài chạy ngầm //////////////////////////


