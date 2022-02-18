package org.jmqtt.bus.subscription;

import org.jmqtt.support.log.JmqttLogger;
import org.jmqtt.support.log.LogUtil;
import org.slf4j.Logger;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Topic implements Serializable , Comparable<Topic> {

    private static final Logger log = JmqttLogger.busLog;

    private static final long serialVersionUID = 2438799283749822L;

    private final String topic;

    private transient List<Token> tokens;

    private transient boolean valid;

    /**
     * Factory method
     *
     * @param s the topic string (es "/a/b").
     * @return the created Topic instance.
     * */
    public static Topic asTopic(String s) {
        return new Topic(s);
    }

    public Topic(String topic) {
        this.topic = topic;
    }

    Topic(List<Token> tokens) {
        this.tokens = tokens;
        List<String> strTokens = tokens.stream().map(Token::toString).collect(Collectors.toList());
        this.topic = String.join("/", strTokens);
        this.valid = true;
    }

    public List<Token> getTokens() {
        if (tokens == null) {
            try {
                tokens = parseTopic(topic);
                valid = true;
            } catch (ParseException e) {
                valid = false;
                LogUtil.error(log,"Error parsing the topic: {}, message: {}", topic, e.getMessage());
            }
        }

        return tokens;
    }

    private List<Token> parseTopic(String topic) throws ParseException {
        if (topic.length() == 0) {
            throw new ParseException("Bad format of topic, topic MUST be at least 1 character [MQTT-4.7.3-1] and " +
                    "this was empty", 0);
        }
        List<Token> res = new ArrayList<>();
        String[] splitted = topic.split("/");

        if (splitted.length == 0) {
            res.add(Token.EMPTY);
        }

        if (topic.endsWith("/")) {
            // Add a fictious space
            String[] newSplitted = new String[splitted.length + 1];
            System.arraycopy(splitted, 0, newSplitted, 0, splitted.length);
            newSplitted[splitted.length] = "";
            splitted = newSplitted;
        }

        for (int i = 0; i < splitted.length; i++) {
            String s = splitted[i];
            if (s.isEmpty()) {
                // if (i != 0) {
                // throw new ParseException("Bad format of topic, expetec topic name between
                // separators", i);
                // }
                res.add(Token.EMPTY);
            } else if (s.equals("#")) {
                // check that multi is the last symbol
                if (i != splitted.length - 1) {
                    throw new ParseException(
                            "Bad format of topic, the multi symbol (#) has to be the last one after a separator",
                            i);
                }
                res.add(Token.MULTI);
            } else if (s.contains("#")) {
                throw new ParseException("Bad format of topic, invalid subtopic name: " + s, i);
            } else if (s.equals("+")) {
                res.add(Token.SINGLE);
            } else if (s.contains("+")) {
                throw new ParseException("Bad format of topic, invalid subtopic name: " + s, i);
            } else {
                res.add(new Token(s));
            }
        }

        return res;
    }

    public Token headToken() {
        final List<Token> tokens = getTokens();
        if (tokens.isEmpty()) {
            return null;
        }
        return tokens.get(0);
    }

    public boolean isEmpty() {
        final List<Token> tokens = getTokens();
        return tokens == null || tokens.isEmpty();
    }

    public Topic exceptHeadToken() {
        List<Token> tokens = getTokens();
        if (tokens.isEmpty()) {
            return new Topic(Collections.emptyList());
        }
        List<Token> tokensCopy = new ArrayList<>(tokens);
        tokensCopy.remove(0);
        return new Topic(tokensCopy);
    }

    public boolean isValid() {
        if (tokens == null)
            getTokens();

        return valid;
    }

    public boolean match(Topic subscriptionTopic) {
        List<Token> msgTokens = getTokens();
        List<Token> subscriptionTokens = subscriptionTopic.getTokens();
        int i = 0;
        for (; i < subscriptionTokens.size(); i++) {
            Token subToken = subscriptionTokens.get(i);
            if (!Token.MULTI.equals(subToken) && !Token.SINGLE.equals(subToken)) {
                if (i >= msgTokens.size()) {
                    return false;
                }
                Token msgToken = msgTokens.get(i);
                if (!msgToken.equals(subToken)) {
                    return false;
                }
            } else {
                if (Token.MULTI.equals(subToken)) {
                    return true;
                }
            }
        }
        return i == msgTokens.size();
    }

    @Override
    public String toString() {
        return topic;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Topic other = (Topic) obj;

        return Objects.equals(this.topic, other.topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }

    @Override
    public int compareTo(Topic o) {
        return topic.compareTo(o.topic);
    }
}
