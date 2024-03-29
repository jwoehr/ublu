target/ublu.jar: FORCE
	rm -f src/main/java/ublu/Version.java
	rm -rf target
	mvn package
	cp target/ublu-*-with-dependencies.jar target/ublu.jar

FORCE:

clean:
	rm -rf target dist-build ublu-dist.zip src/main/java/ublu/Version.java MakeVer.class

dist: target/ublu.jar
	rm -fr dist-build
	mkdir dist-build
	cp target/ublu.jar dist-build/ublu.jar
	cp -R bin dist-build/
	cp -R examples dist-build/
	cp -R extensions dist-build/
	cp -R man dist-build/
	cp -R share dist-build/
	cp -R userdoc dist-build/
	mkdir -p dist-build/licenses
	cp *license* dist-build/licenses
	cp *LICENSE* dist-build/licenses
	cd dist-build/ && zip -rmv ../ublu-dist.zip ./*