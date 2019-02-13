javac -cp . -d build ir\TokenTest.java ir\Tokenizer.java
cd build
java -cp . ir.TokenTest -f ..\token_test.txt -p ..\patterns.txt -rp -cf > ..\tokenized_result.txt
fc ..\token_test_tokenized_ok.txt ..\tokenized_result.txt