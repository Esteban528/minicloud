alias tc := test-class

start:
  mvn spring-boot:run
test:
  mvn test
package:
  mvn clean package
test-class target:
  @echo 'Testing {{target}}…'
  mvn test -Dtest="{{target}}"
