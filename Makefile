all: clean run

clean:
	./gradlew clean
	rm -rf .gradle
	rm -rf .idea
	rm -rf build

run:
	./gradlew --no-daemon run

n: clean
	nvim

# GIT HELPERS

MESSAGE = .

push: add commit
	git push

add:
	git add .

commit:
	git commit -a -m "$(MESSAGE)"