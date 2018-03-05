package org.apache.solr.custom;

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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 * Factory for {@link PayloadSynonymTokenFilter}.
 *
 * <pre class="prettyprint">
 * &lt;fieldType name="text_dlmtd" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.PayloadSynonymTokenFilter" remove="true" multiple="true"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 */
public class PayloadSynonymTokenFilterFactory extends TokenFilterFactory {
  public final String REMOVE_PAYLOAD_ATTR = "remove";
  public final String MULTIPLE_TOKENS_ATTR = "multiple";
  public final String DELIMITER_ATTR = "delimiter";

  private final boolean removePayload;
  private final boolean multipleTokens;
  private final String delimiter;

  public PayloadSynonymTokenFilterFactory(Map<String,String> args) {
    super(args);

    removePayload = getBoolean(args, REMOVE_PAYLOAD_ATTR, false);
    multipleTokens = getBoolean(args, MULTIPLE_TOKENS_ATTR, false);
    delimiter = get(args, DELIMITER_ATTR, "_");
  }

  @Override
  public TokenStream create(TokenStream tokenStream) {
    return new PayloadSynonymTokenFilter(tokenStream, removePayload, multipleTokens, delimiter);
  }
}
