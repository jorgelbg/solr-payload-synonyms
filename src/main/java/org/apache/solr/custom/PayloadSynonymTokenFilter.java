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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Reads the payload of each token in the stream and use it as a synonym
 * initially only for single token synonyms.
 * <p/>
 * Note make sure to use this after using the DelimitedPayloadTokenFilter.
 */
public final class PayloadSynonymTokenFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);
  private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

  private State state;
  private final LinkedList<String> extraTokens = new LinkedList<>();

  /**
   * Delimiter used for splitting the payload for generating several tokens, since the payload is only one
   * sequence of characters without spaces
   */
  private final String delimiter;

  /**
   * Remove the payload after using it as a synonym
   */
  private final boolean removePayload;

  /**
   * Generated separated tokens for the payload using a delimiter
   */
  private final boolean multipleTokens;

  protected PayloadSynonymTokenFilter(TokenStream input, boolean removePayload, boolean multipleTokens,
      String delimiter) {
    super(input);
    this.removePayload = removePayload;
    this.multipleTokens = multipleTokens;
    this.delimiter = delimiter;
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (!extraTokens.isEmpty()) {
      restoreState(state);

      posIncrAtt.setPositionIncrement(0);
      typeAtt.setType(SynonymFilter.TYPE_SYNONYM);
      termAtt.setEmpty().append(extraTokens.remove());

      return true;
    }

    if (input.incrementToken()) {
      BytesRef payload = payAtt.getPayload();

      if (payload != null) {
        if (multipleTokens) {
          String[] tokens = payload.utf8ToString().split("_");

          extraTokens.addAll(Arrays.asList(tokens));
        } else {
          // remove _ and add the whole payload as a single token
          extraTokens.add(payload.utf8ToString().replaceAll(this.delimiter, " "));
        }

        if (removePayload) {
          payAtt.setPayload(null);
        }

        state = captureState();
      }

      return true;
    }

    return false;
  }
}
