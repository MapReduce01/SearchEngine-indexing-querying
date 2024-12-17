/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.demo;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.stream.Collectors;
import java.io.StringReader;
import java.util.*;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.AttributeSource;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.DemoHTMLParser;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;
import java.io.Reader;
import java.io.FileReader;
import org.apache.lucene.demo.CMPT456Analyzer;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class TFIDFHtmlIndexFiles {
  
  private TFIDFHtmlIndexFiles() {}
  

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new CMPT456Analyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param path The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
          } catch (IOException ignore) {
            // don't index files that can't be read.
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
    }
  }

  /** Indexes a single document */
  static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
      // make a new, empty document
      Document doc = new Document();
      
      // Add the path of the file as a field named "path".  Use a
      // field that is indexed (i.e. searchable), but don't tokenize 
      // the field into separate words and don't index term frequency
      // or positional information:
      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
      doc.add(pathField);
      
      // Add the last modified date of the file a field named "modified".
      // Use a LongPoint that is indexed (i.e. efficiently filterable with
      // PointRangeQuery).  This indexes to milli-second resolution, which
      // is often too fine.  You could instead create a number based on
      // year/month/day/hour/minutes/seconds, down the resolution you require.
      // For example the long value 2011021714 would mean
      // February 17, 2011, 2-3 PM.
      doc.add(new LongPoint("modified", lastModified));
      
      // Add the contents of the file to a field named "contents".  Specify a Reader,
      // so that the text of the file is tokenized and indexed, but not stored.
      // Note that FileReader expects the file to be in UTF-8 encoding.
      // If that's not the case searching for special characters will fail.
      //doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
      
      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
        // New index, so we just add the document (no old document can be there):
        if (file.toString().contains(".html")){
          System.out.println("adding " + file);

          String filename = file.toString();

          Reader htmlreader = new FileReader(filename);   
          DocData temp_docdata = new DocData();
          Date temp_date = new Date();
          TrecContentSource temp_trecSrc = new TrecContentSource();
          DemoHTMLParser temp_htmlparser = new DemoHTMLParser();
          DocData parsed_Docdata = temp_htmlparser.parse(temp_docdata,"htmlfile",temp_date,htmlreader,temp_trecSrc);
    
          String sav_contents = parsed_Docdata.getBody();
          String sav_title = parsed_Docdata.getTitle();
          String all_txt = sav_title + " " + sav_contents;
          doc.add(new Field("title", sav_title, TextField.TYPE_STORED));
          doc.add(new TextField("contents", sav_contents, Store.YES));
          System.out.println("Title: " + doc.get("title"));

          int ind = filename.length() - 1;
          while (filename.charAt(ind) != '.') {
            ind = ind -1;
          }
          StringBuilder location_text = new StringBuilder();
          StringBuilder location_tokens = new StringBuilder();
          StringBuilder location_title = new StringBuilder();
          int loop = 0;
          while (loop < ind) {      
            location_text.append(Character.toString(filename.charAt(loop)));
            location_tokens.append(Character.toString(filename.charAt(loop)));
            location_title.append(Character.toString(filename.charAt(loop)));
            loop = loop + 1;
          }
          location_text.append("_contents.txt");
          location_tokens.append("_tokens.txt");
          location_title.append("_title.txt");
          String text_path = location_text.toString();
          String tokens_path = location_tokens.toString();
          String title_path = location_title.toString();
          try {
            Files.write(Paths.get(text_path), sav_contents.getBytes(StandardCharsets.UTF_8));
          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            Files.write(Paths.get(title_path), sav_title.getBytes(StandardCharsets.UTF_8));
          } catch (IOException e) {
            e.printStackTrace();
          }

          Analyzer analyzer2 = new CMPT456Analyzer();
          Map<String, Integer> tokens = tokenizeString(analyzer2,all_txt.toLowerCase());
          File tokenfile = new File(tokens_path);      
          BufferedWriter bf = null;;     
          try{         
              //create new BufferedWriter for the output file
              bf = new BufferedWriter( new FileWriter(tokenfile) );
              //iterate map entries
              for(Map.Entry<String, Integer> entry : tokens.entrySet()){          
                  //put key and value separated by a colon
                  bf.write( entry.getKey() + ":" + entry.getValue() );             
                  //new line
                  bf.newLine();
              }     
              bf.flush();
          }catch(IOException e){
              e.printStackTrace();
          }finally{        
              try{
                  //always close the writer
                  bf.close();
              }catch(Exception e){}
          }
        }
        writer.addDocument(doc);
        
      } else {
        // Existing index (an old copy of this document may have been indexed) so 
        // we use updateDocument instead to replace the old one matching the exact 
        // path, if present:
        System.out.println("updating " + file);
        writer.updateDocument(new Term("path", file.toString()), doc);
      }
    }
  }

/*Following is the title parser implemented by myself, it works fine, but I finally use DemoHTMLParser instead as suggested.

  public static String plaintitle(String path) {
    StringBuilder contentBuilder = new StringBuilder();
    StringBuilder plain_text = new StringBuilder();
    try (Stream<String> stream = Files.lines( Paths.get(path), StandardCharsets.UTF_8)) 
    {
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
    }
    catch (IOException e) 
    {
      e.printStackTrace();
    }
   
    String test = contentBuilder.toString();
    Pattern title = Pattern.compile("<title.*?</title>");
    Matcher title_m = title.matcher(test);

    String p = null;
    if (title_m.find( )) {
      p = title_m.group().replace("<title>","").replace("</title>","");  
    }
    return p;
  }
*/

/*Following is the contents parser implemented by myself, it works fine, but I finally use DemoHTMLParser instead as suggested.

  public static String plaincontents(String path) {
      StringBuilder contentBuilder = new StringBuilder();
      StringBuilder plain_text = new StringBuilder();
      try (Stream<String> stream = Files.lines( Paths.get(path), StandardCharsets.UTF_8)) 
      {
        stream.forEach(s -> contentBuilder.append(s).append("\n"));
      }
      catch (IOException e) 
      {
        e.printStackTrace();
      }
   
      String test_1 = contentBuilder.toString();

      Pattern title = Pattern.compile("<title.*?</title>");
      Matcher title_m = title.matcher(test_1);
      
      while (title_m.find( )) {
        String p = title_m.group();
        test_1 = test_1.replace(p, "");
      }
     
      Pattern script = Pattern.compile("<script.*?</script>");
      Matcher script_m = script.matcher(test_1);
      
      while (script_m.find( )) {
        String p = script_m.group();
        test_1 = test_1.replace(p, "");
      }

      
      Pattern style = Pattern.compile("<style.*?</style>");
      Matcher style_m = style.matcher(test_1);
      
      while (style_m.find( )) {
        String p = style_m.group();
        test_1 = test_1.replace(p, "");
      }
      
      
      Pattern reg = Pattern.compile("&.*?;");
      Matcher reg_m = reg.matcher(test_1);
      
      while (reg_m.find( )) {
        String p = reg_m.group();
        test_1 = test_1.replace(p, "");
      }
      
      Pattern r = Pattern.compile(">.*?<");
      Matcher m = r.matcher(test_1);
    
      while (m.find( )) {
        if(!m.group().contains("@import")){
          plain_text.append(m.group().trim().replace(">", "").replace("<", "")).append(" ");
        }
      }
      
      String final_text = plain_text.toString();
      final_text = final_text.replaceAll("\\s{2,}", " ");
      return final_text;    
  }
*/

    public static Map<String, Integer> tokenizeString(Analyzer analyzer, String string) {
    List<String> result = new ArrayList<String>();
    try {
      TokenStream stream  = analyzer.tokenStream(null, new StringReader(string));
      stream.reset();
      while (stream.incrementToken()) {
        String token = stream.getAttribute(CharTermAttribute.class).toString();
        result.add(token);
      }
      stream.reset();
    } catch (IOException e) {
      // not thrown b/c we're using a string reader...
      throw new RuntimeException(e);
    }
    Map<String, Integer> dict = result.parallelStream().collect(Collectors.toConcurrentMap(w -> w, w -> 1, Integer::sum));
    return dict;
  }

}
