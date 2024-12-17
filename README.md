Project Data
● We are going to use Wiki Small data (6043 documents) from our textbook
<http://www.search-engines-book.com/collections/>. 

● We have included the data for you, within the codebase at location lucene/demo/data.
In the subsequent sections, you will use it in to demonstrate indexing and querying
process.

Compiling
● Build Docker image from the source code (make sure that we have. (i.e. current location)
at the end of the command):
docker build -t cmpt456-lucene-solr:6.6.7.
NOTE: Since Docker is not available free for Windows OS, we recommend you use VirtualBox with
Ubuntu OS or Windows Subsystem for Linux (WSL)
● Run the Docker image we just built in order to activate the Docker container:
docker run -it cmpt456-lucene-solr:6.6.7

Demo
● Index Files: this program uses standard analyzers to create tokens from input text files,
convert them to lowercase then filer out predefined list of stop-words.
The source code is stored in this file within the codebase:
lucene/demo/src/java/org/apache/lucene/demo/IndexFiles.java
Index demo data with the following command inside the Docker container:
ant -f lucene/demo/build.xml \
-Ddocs=lucene/demo/data/wiki-small/en/articles/ run-indexing-demo
● Search Files: this program uses a query parser to parse the input query text, then pass to
the index searcher to look for matching results.
The source code is stored in this file within the codebase:
lucene/demo/src/java/org/apache/lucene/demo/SearchFiles.java
Search demo data with the following command inside the Docker container:
ant -f lucene/demo/build.xml run-search-index-demo

PREPARATION:
	Replace build.xml in < /cmpt456-project1-starter-code/lucene/demo >
	Put HtmlIndexFiles.java ; CMPT456Analyzer.java ; stopwords.txt ; SimpleMetrics.java ; TFIDFHtmlIndexFiles.java ; TFIDFSearchFiles.java ; CMPT456Similarity.java into < /cmpt456-project1-starter-code/lucene/demo/src/java/org/apache/lucene/demo >
	*Put PorterStemFilter.java; PorterStemmer.java into < /cmpt456-project1-starter-code/lucene/core/src/java/org/apache/lucene/analysis/standard >  
(Because “import org.apache.lucene.analysis.en.PorterStemFilter;” always showed “org.apache.lucene.analysis.en this package does not exist”, maybe it’s a MacOS docker application problem or something else, so, I just moved the PorterStemmer and PorterStemFilter to its upper level package “analysis”, and it worked fine. I’ve asked a TA about this and she said it’s OK to do this.)
	


Part 1:
	I parsed every document in the articles folder, also tokenized all of them and saved them along with the original html file.  Which means, after running < ant -f lucene/demo/build.xml \-Ddocs=lucene/demo/data/wiki-small/en/articles/ run-html-indexing-demo >, all the tokens and plain text will be saved along with the html file. For example, if you enter
< ls lucene/demo/data/wiki-small/en/articles/b/b/c/ >, will see 

	BBC_Global_30_dc4a.html		 BBC_Global_30_dc4a_title.txt
	BBC_Global_30_dc4a_contents.txt  BBC_Global_30_dc4a_tokens.txt


Part 2:
	run < ant -f lucene/demo/build.xml \-Ddocs=lucene/demo/data/wiki-small/en/articles/ run-tfidf-indexing > and do the same things in part 1 to display the tokens.


Part 3:
	I used CMPT456Analyzer in this part.
	run < ant -f lucene/demo/build.xml run-simple-metrics-demo > for Simple Metrics
	run < ant -f lucene/demo/build.xml \-Ddocs=lucene/demo/data/wiki-small/en/articles/ run-tfidf-indexing > for TFIDFHtmlIndexFiles
	run < ant -f lucene/demo/build.xml run-tfidf-search > for TFIDFSearchFiles.
