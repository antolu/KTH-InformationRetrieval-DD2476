if not exist build mkdir build

javac -g -cp . -d build ir/HITSRanker.java
java -cp build ir.HITSRanker data/linksDavis.txt data/davisTitles.txt