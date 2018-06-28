# lucene-oxygen
Simple project in IR

# Abstract
The focus of this project is the Non-factoid Question-Answering
task. Within such a setting, a user may post questions in
natural language and answers should be informative enough
to satisfy the users information need

# List of command-line options for this program
~~~
usage: java -jar yourJarName.jar [[option1] [option2]] 
 -i,--index              Reindex documents
 -l,--lambda             Create appropriate lambda for language model,
                         depends on median size of document in corpus
 -q,--create-questions   Create questions file from corpus json file
~~~

 ## Reindexing option
 ~~~
 -i,--index              Reindex documents
 ~~~
 Without this option index will not be created
 

 ## Automatic calculation of lambda for Jelinek-Mercer Smoothing
~~~
 -l,--lambda             Create appropriate lambda for language model,
                         depends on median size of document in corpus
~~~
Lambda parameter calculated by this formula:

![LambdaParameterEquation](https://user-images.githubusercontent.com/5923810/42063945-d978fd4a-7b3c-11e8-897e-9a92f1fa18bb.gif)

## Create questions option
~~~
 -q,--create-questions   Create questions file from corpus json file
~~~
This option needed for lambda parameter calculation, in case that there is no corpus of questions available. This requires regular .json file with corpus collection.

# Source documentation

Can be found here https://louiscyphre.github.io/lucene-oxygen/

Additionally, it can be found here (same files) https://github.com/louiscyphre/lucene-oxygen/tree/master/docs


