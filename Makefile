all:
	javac NormalizeEncodings.java

jar: all
	jar cfve normalize_encodings.jar NormalizeEncodings *.java *.class

clean:
	-rm *.class

dist-clean: clean
	-rm normalize_encodings.jar

test: all
	java NormalizeEncodings ../z_submission8_for_8_6.zip /dev/stdout

test-masysma: all
	7z a -tzip /tmp/test.zip encoding_tests > /dev/null
	java NormalizeEncodings /tmp/test.zip /dev/stdout
	rm /tmp/test.zip
