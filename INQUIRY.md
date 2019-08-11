## Answers to questions

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

## Assignment 2

### 2.4

Difficult case: documents with two or three words and a redirect, are they relevant or not?

> Compare the precision at 10, 20, 30, 40, 50 for the ranked retrieval to the precision for unranked retrieval. Which precision is the highest? Are there any trends?

Precision for the intersection queries is in general higher than for ranked queries. This is to be expected because The ranked retrieval with tfidf tends to favor shorter documents that are not very relevant.

The precision of the ranked retrieval became larger as N grew. Longer documents tended to be more relevant.

> Do the same comparison of recalls. Which recall is the highest? Is there any correlation between precision at 10, 20, 30, 40, 50, recall at 10, 20, 30, 40, 50. 

Recall for ranked retrieval tended to be worse for ranked retrieval than for intersection query for the same reason as above.

### 2.5

> Look up the titles of some documents with high rank, and some documents with low rank. Does the ranking make sense. 

A lot of documents link to 21, and 21 links to 36 so it in turn has high rating. It makes sense. 3 links to 21. 

### 2.6

> Look up the titles of some documents with high rank, and some documents with low rank. Does the ranking make sense. 

A lot of documents link to 121 (1242). 121 links to 245, so 245 subsequently also has a high score. 

For documents with low or no score, are rarely linked to, if at all, and have little to no outlinks. As expected.

> What is the effect of letting the tf_idf score dominate this ranking? What is the effect of letting the pagerank dominate? What would be a good strategy for selecting an "optimal" combination? (Remember the quality measures you studied in Task 2.3)

One method to find good linear combination coefficient is to get results for many coefficient values and use relevance feedback to determine 

If tfidf dominates the score, the engine puts more weight into the content of the document, while if pagerank dominates, the authority of the page will instead show itself. 

### 2.7 

> What do you see? Why do you get this result? Explain, and relate to the properties of the (probabilistic) Monte-Carlo methods in contrast to the (deterministic) power iteration. 

We see that the precision of the probabilistic method increases (the alignment of the top 30 document decreases and approaches zero) as N grows. However it should be noted at N=1e8 the Monte carlo took almost as long as the deterministic method. The convergence of the Monte Carlo method is reasonable. The downside with monte carlo approximation is that the approximation for lower ranked documents will be worse. 

> Do your findings about the difference between the four method variants and the dependence of N support the claims made in the paper by Avrachenkov et al.?

Method 4 and 5 seem to be much faster than 1 and 2, which is expected since the random walk terminates one it reaches a dangling node, instead of walking further. The accuracy of the different methods seem to be largely the same, however. 

For `m=1` the precision of the MC method is indeed very good. The goodness at 1e-4 should be sufficient for most. 

Cyclic start does in general seem to outperform random start. 

> Finally, show your list of 30 top documents for the `linksSvwiki.txt` graph. Argue for why they are correct by looking up the titles of the top documents in the file `svwikiTitles.txt`. 

113605 (highest score) links to 174465, so it has high score.

110193 links to 51553 links.

865076 is linked to by most pages, so it has high score. It links to 865079 which links to 50673, which in turn links to 61554, 836 and 113605. It all makes sense. 

### 2.8 

> Compare your 30 highest ranked hub and authority scores to the ones provided in the files. Does the ranking make sense? How does it compare to pageranks?

Top document in hub score links to a shitton of documents, so it makes sense it is a good hub. Top authorities aren't that highly ranked, but seems like they are linked to by a lot of documents. 

> After you have implemented the HITS algorithm, you'll need to integrate HITS ranking into your search engine. On the way there are several issues that need to be addressed:
> 1. Unlike PageRank, HITS method should run on the fly and only on the query-specific subset of documents. How should one select this subset of documents correctly?
> 2. The HITS algorithm provides two scores for each document in the subset, but the search engine can show only one score. How should one combine these two scores in a meaningful way? Can we use linear combination?

1. The root set is all the documents that explicitly matches the query, and then we construct the base set which is all the documents that link to, and link from the root set. Then we just grab the rows from the A and AT matrices and iterate.
2.  One can use a linear combination, but should then weight the hub and authority score equally. The alternative is to take the largest score of the two, so the page is either a good authority or a good hub. 

> You should be able to list similarities and differences between PageRank and HITS and argue about the advantages and disadvantages of each algorithm. 

* HITS has to run at query-time, which affects performance. 
* HITS is query dependant and does only a local link analysis, but may return more relevant documents.
* HITS cannot detect advertisement. 
* HITS can easily be spammed by adding outlinks from own page. 
* HITS computes two scores for a single document, need to combine them meaningfully. 

* PageRank only returns documents that immediately contain the query words. 
* PageRank is global score.
* PageRank is query independent. 
* Older pages might have higher PageRank, because of obsolete links. 
* PageRank is easily exploitable (link farms).

### 3.1

> What happens to the two documents that you selected?

The query will be expanded according to the contents of the two selected documents. All tokens in the documents will be enumerated and assigned weights in the new query. 

> What are the characteristics of the other documents in the new top ten list - what are they about? Are there any new ones that were not among the top ten before?

The new top 10 will be more related to the documents selected for the relevance feedback. There are new documents, containing content more closely related to the selected document than the original query. 

> Try different values for the weights α and β: How is the relevance feedback process affected by α and β?

High α puts more emphasis on the original query, while a high β makes the modified query more dependant on the contents of the selected document. 

> Ponder these questions: Why is the search after feedback slower? Why is the number of returned documents larger? Why is relevance feedback called a local query refining method? What are other alternatives to relevance feedback for query refining?

Because the query is expanded to include all the tokens in the selected documents for relevance feedback. So a query could contain several hundred tokens instead of just one or two. Since the query is much larger in terms of corpus, a much larger subset of the dataset will be retuurned. 

Local methods a query relative to the documents that initially appear to match the query. 

Other methods include 
* Query expansion/reformulation with a thesaurus or WordNet
* Techniques like spelling correction


### 3.2

> Why do we want to omit that document? (Mathematics.f)
Because that's the document that query feedback was executed with. It will introduce a large bias. 

> Compare your result in 1 and 3 above. What do you see?

Before relevance feedback:  0.6484007637819353
After relevance feedback:  0.7272725425944752

The normalized discount gain decreases after relevance feedback...

### 3.3

### 3.4

> How would you interpret the meaning of the query "historic* humo*r"?

Any spelling of humor, humour or humor (american vs british english?) and historic, historically etc

> Why could the word "revenge" be returned by a bigram index in return to a query "re*ve"?

Because the bigram index fetches the intersection between "re" and "ve". 

> How could this problem of false positives be solved?

Enumerate the bigrams as "^r, re, ve e$" instead. And do a linear search over the retrieved words to make sure they match the query. 

> How would you get the ranking for the ranked wildcard queries?

"mo*y transfer" could result in the queries "money transfer" and "moriarty transfer". While for intersection and ranked query one would search those queries separately, for ranked query a search using "money moriarty transfer" would be executed. 

> Which of the three queries was the fastest? Which was the slowest? Why? (mo\*y transfer, b\* colo\*r, b\* colo\*r, having \*n)

"mo\*y transfer" was the fastest, the wildcard didn't match that many documents. "b\* colo\*r" was the slowest due to the sheer number of matching wildcards. "having *n" was also quite slow, due to all tokens matching "\*n" (all words engin in n). 

### 3.5

