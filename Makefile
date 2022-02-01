target/ublu.jar: FORCE
	rm -rf target
	mvn package
	cp target/ublu-*-with-dependencies.jar target/ublu.jar

FORCE:

dist: 
	rm -fr dist-build
	mkdir dist-build
	cp target/ublu.jar dist-build/ublu.jar
	cp -R bin dist-build/
	cp -R examples dist-build/
	cp -R extensions dist-build/
	cp -R man dist-build/
	cp -R share dist-build/
	cp -R userdoc dist-build/
	cd dist-build/ && zip -rmv ../ublu-dist.zip ./*