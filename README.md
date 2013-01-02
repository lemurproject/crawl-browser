
=============
crawl-browser
=============

Requirements & Use Cases
------------------------

1. Read input files
   - Read the files stored on multiple input directories
   - Select the files modified within certain dates

2. Filter each WARC file:
   - Select only response records
   - Filter out non-english responses
     - Extract the first text elements fron the HTML
     - Use a language identifier

3. Index the records
   - Add the records to a Lucene index. Fields:
     - Source file name
     - Record #
     - URI
     - Host
     - Crawled date
     - Content (text)
     - Content (html)

4. Generate reports
     


