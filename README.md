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
Person with degree in mathematics. 

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

Precision: percentage of returned results which are relevant.  
Recall: percentage of total relevant results returned.

Precision 17/22

Recall 17/100

### 1.6  

Query: ****

> Why can we not simply set the query to be the entire information need description?  
Because computers don't understand natural language as humans do. Asking *How do travel from London to Paris via train?* to Google will probably yield very relevant results, in many cases were *trabel*, *London*, *Paris* and *train* are frequent, and not because the search engine understood your query (what you were "asking"). 

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