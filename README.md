# DD2476
Search engines and information retrieval

### 1.3
Tested queries:
have you met my mom: 54 matches

```
if (docID1 == docID2)   
    add(docID1)   
    docID1 <- next(docID1)   
    docID2 <- next(docID2)   
else if (docID1 > docID2)   
    docID1 <- next(docID1)  
else   
    docID2 <- next(docID2)  
```

### 1.4
have you met my mom: 0 matches

> Why are fewer documents generally returned in phrase query mode than in intersection query mode? 

In a sense phrase query is more strict than intersection query. The phrase `a query` consists of the words `a` and `query`, which may both be present in a document, but must not necessarily be right next to each other in intersection mode. 

### 1.5

Difficult cases: majors with courses in mathematics. Are we looking for documents about majors in mathematics, or majors with math courses?
Some documents describe people having a degree in mathematics living in davis, or have a graduate, and know mathematics. Are they relevant?

**Biological_Systems_Engineering.f 2**  
About graduate program in biological systems, with course in mathematics.

**Candidate_Statements.f 1**  
\[difficult\] Some candidate statements (for a job?). Mentions someone studying mathematics. 

**Computer_Science.f 2**  
\[difficult\] Someone studying computer science, has taken a course in mathematics. 

**document_translated.f 0**  
Terms including the word `mathematics` point toward a professor in mathematics (background). No relevance whatsoever to a graduate program.

**ECE_Course_Reviews.f 0**  
Course overview. Math course exists. Not about graduate program.

**Economics.f 2**  
About economics graduate programme, where mathematics is required (background). 

**Elaine_Kasimatis.f 3**  
\[difficult\] Person with degree in mathematics. 

**Evelyn_Silvia.f 3**  
Mathematics professor. worked with improving mathematics education at all levels. 

**Events_Calendars.f 0**  
An event calendar. Nothing of relevance at all.

**Fiber_and_Polymer_Science.f 2**  
About major in fiber and polymer science where mathematics is required. 

**Hydrology.f 2**  
Abou hydrology program where mathematics is useful. 

**Mathematics.f 3**  
Mathematics graduate program. 

**MattHh.f 3**  
Someone with a degree in mathematics. 

**Private_Tutoring.f 3**  
Company offering private tutoring in mathematics for high school and college students. Presenting people with degrees in mathematics. 

**Quantitative_Biology_and_Bioinformatics.f 0**  
Minor with some maths. 

**Statistics.f 3**  
Statistics major. Obviously a lot of maths. 

**Student_Organizations.f 1**  
Student organizations. One in mathematics. 

**UCD_Honors_and_Prizes.f 0**  
Honorary awards. Some organization in maths. 

**UC_Davis_English_Department.f 2**  
Major in english. Someone was math major who took english 101.

**University Departments.f 1**  
University departnents. One in maths.

**What_I_Wish_I_Knew...Before_Coming_to_UC_Davis_Entomology.f 1**  
Welcome email. Info to graduate students about their department contact persons. 

**Wildlife%2C_Fish%2C_and_Conservation_Biology.f 1**  
Minor in wildlife and conservation biology, part of other majors, like applied mathematics. 

Precision: percentage of returned results which are relevant.  "How useful the search results are"
Recall: percentage of total relevant results returned. "How complete the search results are"

Precision 17/22

Recall 17/100

### 1.6  

### First query
`mathematics graduate uc davis`

27 documents, too many to categorize. (too broad query)

### Second query
`mathematics graduate major uc davis`
When talking about graduate education, the word major often also comes up, so it may yield narrower results.

#### Documents

Candidate_Statements 0
Computer_Science 3
ECE_Course_Reviews 2
Economics 3
Events_Calendars 0
Hydrology 1
Mathematics 3
MattHh 3
PhilipNeustrom 0
Private_Tutoring 1
Statistics 3
Teaching_Assistants 1
UCD_Honors_and_prizes 0
UD_Davis_English_Department 0
University_Departments 0
What_I_Wish_I_Knew...Before_Coming_to_UC_Davis_Entomology.f 0
Wildlife%2C_Fish%2C_and_Conservation_Biology.f 0

Precision: 9/17
Recall: 9/?


### Third query

`mathematics math graduate major uc davis`
Should one look for something related to mathematics, articles tend to use mathematics and maths more interchangeably, thus one can search for both at once in an intersection query.

#### Documents

Computer Science 3
ECE_Course_Reviews 2
Economics 3
Mathematics 3
MattHh 3
Private tutoring 1
Statistics 3
Teaching Assistants 1

Precision: 1 (9/9)
Recall: 8/?
> Why can we not simply set the query to be the entire information need description?  
Because computers don't understand natural language as humans do. Asking *How do I travel from London to Paris via train?* to Google will probably yield very relevant results, in many cases were *travel*, *London*, *Paris* and *train* are frequent, and not because the search engine understood your query (what you were "asking"). 

### 1.7
PostingsLists saved as

```
List1:List2:List3...
```

Where each list is saved as 

```
Entry1;Entry2;Entry3
```

And each entry is saved as

```
docID!offset1,offset2,offset3
```

### 1.8

The reason the query `queen of england` yields 364 (out of 364) matches in intersection query mode, but only 6 (out of 7) in phrase query mode, may be because of how the tokenizer works. Let's say that we have the phrase `queen of<england>` in the document, then the tokenizer may translate this to `queen`, `of`, `<`, `england` and `>`, where queen of england is no longer a contiguous phrase. 

### 2.5

> Look up the titles of some documents with high rank, and some documents with low rank. Does the ranking make sense. 

A lot of documents link to 21, and 21 links to 36 so it in turn has high rating. It makes sense. 3 links to 21. 