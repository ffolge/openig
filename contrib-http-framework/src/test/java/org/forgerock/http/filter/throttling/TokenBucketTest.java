/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.http.filter.throttling;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.time.Duration.duration;

import org.forgerock.util.time.Duration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class TokenBucketTest {

    private FakeTicker ticker;

    @BeforeMethod
    public void setUp() throws Exception {
        ticker = new FakeTicker(0);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class })
    public void shouldNotBePossibleToInstantiateWithUnlimitedDuration() throws Exception {
        new TokenBucket(ticker, new ThrottlingRate(1, Duration.UNLIMITED));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void shouldNotBePossibleToInstantiateWithDurationLessThan1Ms() throws Exception {
        new TokenBucket(ticker, new ThrottlingRate(1, duration("3 nanoseconds")));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void shouldNotBePossibleToInstantiateWithNegativeCapacity() throws Exception {
        new TokenBucket(ticker, new ThrottlingRate(-1, duration("42 years"))); // arbitrary duration
    }

    @Test
    public void shouldTheBucketBeRefilled() throws Exception {
        // a token bucket that can refill 1 token every 333.333 ms.
        TokenBucket bucket = new TokenBucket(ticker, new ThrottlingRate(3, duration("1 second")));

        ticker.advance(0, MILLISECONDS);
        assertThat(bucket.tryConsume()).isEqualTo(0); // First ticker, so we can consume a token
        assertThat(bucket.getRemainingTokensCount()).isEqualTo(2);

        ticker.advance(1, MILLISECONDS); // t0 + 1 ms
        assertThat(bucket.tryConsume()).isEqualTo(0); // Second ticker < 1s, so we can consume a token
        assertThat(bucket.getRemainingTokensCount()).isEqualTo(1);

        assertThat(bucket.tryConsume()).isEqualTo(0); // Third ticker < 1s, so we can consume a token
        assertThat(bucket.getRemainingTokensCount()).isEqualTo(0);

        ticker.advance(2, MILLISECONDS); // t0 + 3 ms
        assertThat(bucket.tryConsume()).isEqualTo(330); // Not enough elapsed ticker to get a refill

        ticker.advance(331, MILLISECONDS); // t0 + 334 ms
        assertThat(bucket.tryConsume()).isEqualTo(0); // Enough elapsed ticker to get a refill
    }

    @Test
    public void shouldTheRefillRateBeLessThan1Millisecond() throws Exception {
        FakeTicker ticker = new FakeTicker(0);

        // a token bucket that can refill 1 token every 0.333 ms.
        TokenBucket bucket = new TokenBucket(ticker, new ThrottlingRate(3000, duration("1 second")));

        assertThat(bucket.tryConsume()).isEqualTo(0); // First ticker, so we can consume a token
        assertThat(bucket.getRemainingTokensCount()).isEqualTo(2999);

        ticker.advance(1, MILLISECONDS);
        assertThat(bucket.tryConsume()).isEqualTo(0); // Second ticker < 1s, so we can consume a token
        assertThat(bucket.getRemainingTokensCount()).isEqualTo(2999);
    }

    @Test
    public void shouldNotConsumeMoreTokensThanExpected() throws Exception {
        FakeTicker ticker = new FakeTicker(42);
        // 3 reqs / sec means it has a unlimited fractional part
        TokenBucket bucket = new TokenBucket(ticker, new ThrottlingRate(3, duration("1 second")));

        // Simulate some heavy load by trying to consume some tokens in the same millisecond : only the first one
        // has to be consumed.
        assertThat(bucket.tryConsume()).as("Consume first token").isLessThanOrEqualTo(0);
        assertThat(bucket.tryConsume()).as("Consume second token").isLessThanOrEqualTo(0);
        assertThat(bucket.tryConsume()).as("Consume third token").isLessThanOrEqualTo(0);
        assertThat(bucket.tryConsume()).as("Consume fourth token").isGreaterThan(0);
    }


}
