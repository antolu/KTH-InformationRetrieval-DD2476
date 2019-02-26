if not exist build mkdir build

javac -g -cp . -d build pagerank/*.java montecarlo/*.java

java -Xmx2G -cp build montecarlo.MonteCarlo data/linksDavis.txt