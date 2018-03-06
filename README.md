# solr-payload-synonyms

Add "contextual" synonyms to Solr using payloads. 

## Contextual Synonyms

A single term can relate to different concepts in the same field/document. We call a "contextual synonym" to a synonym only appliend to one (or more) specific tokens withing a field.

The principle behind this component is explained in [this post](https://jorgelbg.github.io/posts/solr-contextual-synonyms/). This code is provided as a support for the given post. Although a very similar approach was used in a production environment.

## Build

To build the project just execute

```
mvn -e package
```

## Installation

You can wrap the `.jar` file on the `target/` directory and add it to your Solr/Fusion installation. After that you need to add the filter to one of your `fieldtype`:

```xml
<fieldtype name="payloads" stored="false" indexed="true" class="solr.TextField" >
 <analyzer>
   <tokenizer class="solr.WhitespaceTokenizerFactory"/>
   <filter class="solr.DelimitedPayloadTokenFilterFactory" delimiter="|" encoder="identity"/>
   <filter class="solr.custom.PayloadSynonymTokenFilterFactory"/>
 </analyzer>
</fieldtype>
```

Once your `fieldtype` is defined we can use the very helpful Analysis page of the Solr Admin UI to check if things are working as expected. If we use the test string: `Bill|Clinton talked about the bill` in the Field value (index) input and select our payload `fieldtype` we can see an output similar to what is shown in the figure. 

![Solr Admin UI](http://jorgelbg.github.io/images/solr-synonyms/analysis-ui.png "Solr Admin UI")

A quick inspection, reveals that the tokens `Bill` and `Clinton` have the same positional information. Also the `Clinton` token has a defined type of `SYNONYM`.
