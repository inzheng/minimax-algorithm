all: Minmax.java
	javac Minmax.java

clean:
	rm -f *.class

.PHONY: all clean
