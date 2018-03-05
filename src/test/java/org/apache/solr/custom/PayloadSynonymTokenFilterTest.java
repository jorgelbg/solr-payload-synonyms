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

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.IdentityEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class PayloadSynonymTokenFilterTest extends BaseTokenStreamTestCase {
  @Test
  public void testRemovePayload() throws Exception {
    String test = "A";

    PayloadSynonymTokenFilter filter = new PayloadSynonymTokenFilter(
        new WordTokenFilter(whitespaceMockTokenizer(test), "J"), true, false, "_");

    CharTermAttribute termAtt = filter.getAttribute(CharTermAttribute.class);
    PayloadAttribute payAtt = filter.getAttribute(PayloadAttribute.class);

    filter.reset();

    assertTermEquals("A", filter, termAtt, payAtt, null);
    assertTermEquals("J", filter, termAtt, payAtt, null);
    assertFalse(filter.incrementToken());

    filter.end();
    filter.close();

    filter.reset();
  }

  @Test
  public void testMultipleSynonyms() throws Exception {
    String test = "A";

    PayloadSynonymTokenFilter filter = new PayloadSynonymTokenFilter(
        new WordTokenFilter(whitespaceMockTokenizer(test), "D_E"), false, true, "_");

    CharTermAttribute termAtt = filter.getAttribute(CharTermAttribute.class);
    PayloadAttribute payAtt = filter.getAttribute(PayloadAttribute.class);

    filter.reset();

    assertTermEquals("A", filter, termAtt, payAtt, "D_E".getBytes(StandardCharsets.UTF_8));
    assertTermEquals("D", filter, termAtt, payAtt, "D_E".getBytes(StandardCharsets.UTF_8));
    assertTermEquals("E", filter, termAtt, payAtt, "D_E".getBytes(StandardCharsets.UTF_8));
    assertFalse(filter.incrementToken());

    filter.end();
    filter.close();

    filter.reset();
  }

  @Test
  public void testNoSynonyms() throws Exception {
    String test = "F G H";

    PayloadSynonymTokenFilter filter = new PayloadSynonymTokenFilter(new WordTokenFilter(whitespaceMockTokenizer(test)),
        false, false, "_");

    CharTermAttribute termAtt = filter.getAttribute(CharTermAttribute.class);
    PayloadAttribute payAtt = filter.getAttribute(PayloadAttribute.class);

    filter.reset();

    assertTermEquals("F", filter, termAtt, payAtt, null);
    assertTermEquals("G", filter, termAtt, payAtt, null);
    assertTermEquals("H", filter, termAtt, payAtt, null);
    assertFalse(filter.incrementToken());

    filter.end();
    filter.close();

    filter.reset();
  }

  @Test
  public void testSingleSynonym() throws Exception {
    String test = "A B F";

    PayloadSynonymTokenFilter filter = new PayloadSynonymTokenFilter(new WordTokenFilter(whitespaceMockTokenizer(test)),
        false, false, "_");

    CharTermAttribute termAtt = filter.getAttribute(CharTermAttribute.class);
    PayloadAttribute payAtt = filter.getAttribute(PayloadAttribute.class);
    PositionIncrementAttribute posIncAtt = filter.getAttribute(PositionIncrementAttribute.class);

    filter.reset();

    assertTermEquals("A", filter, termAtt, payAtt, "D".getBytes(StandardCharsets.UTF_8));
    assertTermEquals("D", filter, termAtt, payAtt, "D".getBytes(StandardCharsets.UTF_8));

    assertEquals(0, posIncAtt.getPositionIncrement());
    assertTermEquals("B", filter, termAtt, payAtt, "D".getBytes(StandardCharsets.UTF_8));
    assertTermEquals("D", filter, termAtt, payAtt, "D".getBytes(StandardCharsets.UTF_8));
    assertEquals(0, posIncAtt.getPositionIncrement());
    assertTermEquals("F", filter, termAtt, payAtt, null);
    assertFalse(filter.incrementToken());

    filter.end();
    filter.close();
  }

  @Test
  public void testSingleLargeSynonym() throws Exception {
    String test = "A B F";

    PayloadSynonymTokenFilter filter = new PayloadSynonymTokenFilter(
        new WordTokenFilter(whitespaceMockTokenizer(test), "M_H"), false, false, "_");

    CharTermAttribute termAtt = filter.getAttribute(CharTermAttribute.class);
    PayloadAttribute payAtt = filter.getAttribute(PayloadAttribute.class);

    filter.reset();

    assertTermEquals("A", filter, termAtt, payAtt, "M_H".getBytes(StandardCharsets.UTF_8));
    assertTermEquals("M H", filter, termAtt, payAtt, "M_H".getBytes(StandardCharsets.UTF_8));
    assertTermEquals("B", filter, termAtt, payAtt, "M_H".getBytes(StandardCharsets.UTF_8));
    assertTermEquals("M H", filter, termAtt, payAtt, "M_H".getBytes(StandardCharsets.UTF_8));
    assertTermEquals("F", filter, termAtt, payAtt, null);
    assertFalse(filter.incrementToken());

    filter.end();
    filter.close();
  }

  void assertTermEquals(String expected, TokenStream stream, CharTermAttribute termAtt, PayloadAttribute payAtt,
      byte[] expectPay) throws Exception {
    assertTrue(stream.incrementToken());
    assertEquals(expected, termAtt.toString());
    BytesRef payload = payAtt.getPayload();

    if (payload != null) {
      assertTrue(payload.length + " does not equal: " + expectPay.length, payload.length == expectPay.length);
      for (int i = 0; i < expectPay.length; i++) {
        assertTrue(expectPay[i] + " does not equal: " + payload.bytes[i + payload.offset],
            expectPay[i] == payload.bytes[i + payload.offset]);
      }
    } else {
      assertTrue("expectPay is not null and it should be", expectPay == null);
    }
  }

  /**
   * Dummy TokenFilter that adds a default payload to the desired tokens, avoids using
   * {@link org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter}
   */
  private final class WordTokenFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);

    private final List<String> payloadTokens = Arrays.asList("A", "B", "C");
    private final PayloadEncoder encoder = new IdentityEncoder();
    private final BytesRef payload;

    private WordTokenFilter(TokenStream input) {
      super(input);
      this.payload = encoder.encode("D".toCharArray());
    }

    private WordTokenFilter(TokenStream input, String payloadValue) {
      super(input);
      this.payload = encoder.encode(payloadValue.toCharArray());
    }

    @Override
    public boolean incrementToken() throws IOException {
      if (input.incrementToken()) {
        if (payloadTokens.contains(termAtt.toString())) payAtt.setPayload(payload);
        return true;
      } else {
        return false;
      }
    }
  }

}