/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.stompjms;

import javax.jms.IllegalStateException;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

/**
 * Implementation of a TopicSubscriber
 * 
 */
public class StompJmsTopicSubscriber extends StompJmsMessageConsumer implements TopicSubscriber {
    private boolean noLocal;
    /**
     * Constructor
     * 
     * @param s
     * @param destination
     */
    protected StompJmsTopicSubscriber(String id,StompJmsSession s, StompJmsDestination destination, String name, 
            boolean noLocal,String selector) {
        super(id,s, destination,selector);
        this.noLocal=noLocal;
    }

    /**
     * @return noLocak flag
     * @throws IllegalStateException
     * @see javax.jms.TopicSubscriber#getNoLocal()
     */
    public boolean getNoLocal() throws IllegalStateException {
        checkClosed();
        return this.noLocal;
    }

    /**
     * @return the Topic
     * @throws IllegalStateException
     * @see javax.jms.TopicSubscriber#getTopic()
     */
    public Topic getTopic() throws IllegalStateException {
        checkClosed();
        return (Topic) this.destination;
    }
}
